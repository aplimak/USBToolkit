//
// Created by aeliux on 11/19/26.
//

#include "JniCallable.hpp"

#include <memory>
#include <stdexcept>

namespace {

    struct EnvHolder
    {
        JavaVM* jvm;
        JNIEnv* env;
        bool attached;

        EnvHolder(JavaVM* jvm, JNIEnv* env, bool attached) noexcept
                : jvm(jvm), env(env), attached(attached)
        {
        }

        EnvHolder(const EnvHolder&) = delete;
        EnvHolder& operator=(const EnvHolder&) = delete;

        ~EnvHolder() noexcept
        {
            if (!attached)
                return;

            if (env->ExceptionCheck())
                env->ExceptionClear();

            jvm->DetachCurrentThread();
        }
    };

    EnvHolder acquireEnv(JavaVM* jvm)
    {
        if (!jvm)
            throw std::invalid_argument("jvm can't be null");

        JNIEnv* env = nullptr;

        jint rc = jvm->GetEnv(
                reinterpret_cast<void**>(&env),
                JNI_VERSION_1_6
        );

        bool attached = false;

        if (rc == JNI_EDETACHED) {
            rc = jvm->AttachCurrentThread(&env, nullptr);
            attached = (rc == JNI_OK);
        }

        if (rc != JNI_OK || !env)
            throw std::runtime_error("Can't acquire a valid JNI environment");

        return EnvHolder{jvm, env, attached};
    }

    void deleteGlobalRef(JavaVM* jvm, jobject object) noexcept
    {
        if (!jvm || !object)
            return;

        JNIEnv* env = nullptr;

        jint rc = jvm->GetEnv(
                reinterpret_cast<void**>(&env),
                JNI_VERSION_1_6
        );

        bool attachedHere = false;

        if (rc == JNI_EDETACHED) {
            rc = jvm->AttachCurrentThread(&env, nullptr);
            attachedHere = (rc == JNI_OK);
        }

        // We cannot safely perform JNI operations without a valid environment.
        // During shutdown, attachment may fail. In that case, cleanup is best-effort.
        if (rc != JNI_OK || !env)
            return;

        env->DeleteGlobalRef(object);

        if (attachedHere) {
            if (env->ExceptionCheck())
                env->ExceptionClear();

            jvm->DetachCurrentThread();
        }
    }
}

namespace ptrw {
    struct JniCallable::Impl {
        JavaVM *jvm;
        jobject object;
        jmethodID mid;

        Impl(JavaVM* jvm, jobject object, jmethodID mid) noexcept
                : jvm(jvm), object(object), mid(mid)
        {
        }

        ~Impl() noexcept
        {
            deleteGlobalRef(jvm, object);
        }

        Impl(const Impl&) = delete;
        Impl& operator=(const Impl&) = delete;
        Impl(Impl&&) noexcept = delete;
        Impl& operator=(Impl&&) noexcept = delete;
    };

    JniCallable::JniCallable(JavaVM *jvm, jobject object, jmethodID mid): m_impl(nullptr)
    {
        if (jvm == nullptr) {
            throw std::invalid_argument("jvm can't be null");
        }
        if (object == nullptr) {
            throw std::invalid_argument("object can't be null");
        }
        if (mid == nullptr) {
            throw std::invalid_argument("mid can't be null");
        }

        m_impl = std::make_unique<Impl>(
            jvm,
            object,
            mid
        );
    }

    JniCallable JniCallable::fromRunnable(JavaVM *jvm, jobject runnable)
    {
        if (!jvm)      throw std::invalid_argument("jvm can't be null");
        if (!runnable) throw std::invalid_argument("runnable can't be null");
        auto holder = acquireEnv(jvm);
        JNIEnv *env = holder.env;
        if (env->ExceptionCheck())
            throw std::runtime_error("JVM thread already has a pending exception");

        jobject object = env->NewGlobalRef(runnable);
        if (object == nullptr) {
            throw std::runtime_error("could not get the global ref of the runnable");
        }
        struct GlobalRefGuard {
            JNIEnv *env; jobject ref;
            ~GlobalRefGuard() noexcept { if (ref) env->DeleteGlobalRef(ref); }
            void release() noexcept { ref = nullptr; }
            GlobalRefGuard(const GlobalRefGuard&) = delete;
            GlobalRefGuard& operator=(const GlobalRefGuard&) = delete;
            GlobalRefGuard(const GlobalRefGuard&&) = delete;
            GlobalRefGuard& operator=(const GlobalRefGuard&&) = delete;
        } guard{env, object};

        if (env->ExceptionCheck()) {
            throw std::runtime_error("could not get the global ref of the runnable");
        }

        jclass callbackClass = env->GetObjectClass(object);
        if (callbackClass == nullptr) {
            throw std::runtime_error("could not get the class ref of the runnable");
        }
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(callbackClass);
            throw std::runtime_error("could not get the class ref of the runnable");
        }
        jmethodID mid = env->GetMethodID(callbackClass, "run", "()V");
        env->DeleteLocalRef(callbackClass);
        if (mid == nullptr || env->ExceptionCheck()) {
            throw std::runtime_error("could not resolve Runnable.run()");
        }

        JniCallable obj(jvm, object, mid);
        guard.release();
        return obj;
    }

    bool JniCallable::call()
    {
        if (!m_impl)
            throw std::logic_error("JniCallable is empty");

        auto holder = acquireEnv(m_impl->jvm);
        JNIEnv *env = holder.env;
        if (env->ExceptionCheck())
            throw std::runtime_error("JVM thread already has a pending exception");

        env->CallVoidMethod(m_impl->object, m_impl->mid);
        return !env->ExceptionCheck();
    }

    JniCallable::~JniCallable() noexcept = default;
    JniCallable::JniCallable(JniCallable&&) noexcept = default;
    JniCallable& JniCallable::operator=(JniCallable&&) noexcept = default;
} // ptrw
