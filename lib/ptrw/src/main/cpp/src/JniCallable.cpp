//
// Created by aeliux on 11/19/26.
//

#include "JniCallable.hpp"

#include <memory>
#include <stdexcept>

struct EnvHolder {
    JavaVM *jvm;
    JNIEnv *env;
    bool attached;

    ~EnvHolder() {
        if (attached) {
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
            }
            jvm->DetachCurrentThread();
        }
    }
};

static std::unique_ptr<EnvHolder> acquireEnv(JavaVM *jvm) {
    if (!jvm) throw std::invalid_argument("jvm can't be null");
    JNIEnv *env = nullptr;
    bool attached = true;
    int rc = JNI_OK;
    if ((rc = jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6)) != JNI_OK) {
        rc = jvm->AttachCurrentThread(&env, nullptr);
    } else {
        attached = false;
    }

    if (rc != JNI_OK || ! env) {
        throw std::runtime_error("Can't aquire a valid jni env");
    }

    return std::unique_ptr<EnvHolder>(new EnvHolder{
        jvm,
        env,
        attached
    });
}

namespace ptrw {
    struct JniCallable::Impl {
        JavaVM *jvm;
        jobject object;
        jmethodID mid;
    };

    JniCallable::JniCallable(JavaVM *jvm, jobject object, jmethodID mid): m_impl(nullptr)
    {
        if (jvm == nullptr) {
            throw std::invalid_argument("jvm can't be null");
        }
        m_impl = new Impl{
            jvm,
            object,
            mid
        };
    }

    JniCallable::~JniCallable()
    {
        try {
            auto holder = acquireEnv(m_impl->jvm);
            JNIEnv *env = holder->env;
            env->DeleteGlobalRef(m_impl->object);
            delete m_impl;
        } catch (const std::runtime_error &e) {
            std::terminate();
        }
    }

    std::shared_ptr<JniCallable> JniCallable::fromRunnable(JavaVM *jvm, jobject runnable)
    {
        if (!jvm)      throw std::invalid_argument("jvm can't be null");
        if (!runnable) throw std::invalid_argument("runnable can't be null");
        auto holder = acquireEnv(jvm);
        JNIEnv *env = holder->env;
        jobject object = env->NewGlobalRef(runnable);
        if (object == nullptr || env->ExceptionCheck()) {
            throw std::runtime_error("could not get the global ref of the runnable");
        }
        struct GlobalRefGuard {
            JNIEnv *env; jobject ref;
            ~GlobalRefGuard() { if (ref) env->DeleteGlobalRef(ref); }
        } guard{env, object};

        jclass callbackClass = env->GetObjectClass(object);
        if (callbackClass == nullptr || env->ExceptionCheck()) {
            throw std::runtime_error("could not get the class ref of the runnable");
        }
        jmethodID mid = env->GetMethodID(callbackClass, "run", "()V");
        env->DeleteLocalRef(callbackClass);
        if (mid == nullptr || env->ExceptionCheck()) {
            throw std::runtime_error("could not resolve Runnable.run()");
        }

        auto self = std::shared_ptr<JniCallable>(new JniCallable{jvm, object, mid});
        guard.ref = nullptr;
        return self;
    }

    bool JniCallable::call()
    {
        auto holder = acquireEnv(m_impl->jvm);
        JNIEnv *env = holder->env;
        if (env->ExceptionCheck()) env->ExceptionClear();
        env->CallVoidMethod(m_impl->object, m_impl->mid);
        return !env->ExceptionCheck();
    }
} // ptrw
