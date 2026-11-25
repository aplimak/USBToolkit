#include "axsn/jni.h"
#include "axsn/jni_internal.h"

#include <axsn/error.h>
#include <axsn/log.h>
#include <jni.h>
#include <stdatomic.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>

/*
 * This storage belongs to the object file, which CMake embeds in each
 * consuming shared library. It is consequently per-DSO, not process-global.
 */
static _Atomic(JavaVM *) g_axsn_java_vm = ATOMIC_VAR_INIT(NULL);

static _Atomic(int) g_axsn_jni_alive_libraries = ATOMIC_VAR_INIT(0);

JavaVM *axsn_jni_get_vm(void) {
    return atomic_load_explicit(&g_axsn_java_vm, memory_order_acquire);
}

JNIEnv *axsn_jni_get_env(void) {
    JavaVM *vm = axsn_jni_get_vm();
    void *raw_env = NULL;
    jint result;

    if (vm == NULL) {
        return NULL;
    }

    result = (*vm)->GetEnv(vm, &raw_env, JNI_VERSION_1_6);
    if (result != JNI_OK) {
        return NULL;
    }

    return (JNIEnv *)raw_env;
}

jint axsn_jni_env_acquire(axsn_jni_env *jenv, const char *thread_name) {
    JavaVM *vm;
    void *raw_env = NULL;
    jint result;

    if (jenv == NULL) {
        return JNI_EINVAL;
    }

    jenv->vm = NULL;
    jenv->env = NULL;
    jenv->attached = 0;

    vm = axsn_jni_get_vm();
    if (vm == NULL) {
        return JNI_ERR;
    }

    result = (*vm)->GetEnv(vm, &raw_env, JNI_VERSION_1_6);
    if (result == JNI_OK) {
        jenv->vm = vm;
        jenv->env = (JNIEnv *)raw_env;
        return JNI_OK;
    }

    if (result != JNI_EDETACHED) {
        return result;
    }

    if (thread_name != NULL) {
        JavaVMAttachArgs args;
        size_t name_size = strlen(thread_name) + 1U;
        char *mutable_name = (char *)malloc(name_size);
        if (mutable_name == NULL) {
            return JNI_ENOMEM;
        }
        memcpy(mutable_name, thread_name, name_size);
        args.version = JNI_VERSION_1_6;
        args.name = mutable_name;
        args.group = NULL;
        result = (*vm)->AttachCurrentThread(vm, (JNIEnv **)&raw_env, &args);
        free(mutable_name);
    } else {
        result = (*vm)->AttachCurrentThread(vm, (JNIEnv **)&raw_env, NULL);
    }

    if (result != JNI_OK) {
        return result;
    }

    jenv->vm = vm;
    jenv->env = (JNIEnv *)raw_env;
    jenv->attached = 1;
    return JNI_OK;
}

void axsn_jni_env_release(axsn_jni_env *jenv) {
    if (jenv == NULL) {
        return;
    }

    if (jenv->attached && jenv->vm != NULL) {
        if ((*jenv->env)->ExceptionCheck(jenv->env)) {
            char error_buf[1024];
            axsn_get_pending_exception(jenv->env, error_buf, sizeof(error_buf));
            LOGE("Unhandled pending exception on a jvm attached thread:\n%s", error_buf);
        }
        (void)(*jenv->vm)->DetachCurrentThread(jenv->vm);
    }

    jenv->vm = NULL;
    jenv->env = NULL;
    jenv->attached = 0;
}

jint axsn_jni_load(JavaVM *vm, void *reserved) {
    LOGI("axsn_jni_load");
    void *raw_env = NULL;
    jint result;

    (void)reserved;

    if (vm == NULL) {
        return JNI_ERR;
    }

    result = (*vm)->GetEnv(vm, &raw_env, JNI_VERSION_1_6);
    if (result != JNI_OK || raw_env == NULL) {
        LOGE("axsn_jni_load: GetEnv failed");
        return JNI_ERR;
    }

    atomic_store_explicit(&g_axsn_java_vm, vm, memory_order_release);

    const axsn_jni_env jenv = {
            .vm = vm,
            .env = (JNIEnv *)raw_env,
            .attached = 0
    };

    LOGI("axsn_jni_load call hooks");
    if (axsn_jni_call_load_hooks(&jenv) != JNI_OK) {
        return JNI_ERR;
    }

    atomic_fetch_add_explicit(&g_axsn_jni_alive_libraries, 1, memory_order_acq_rel);
    int libs = atomic_load(&g_axsn_jni_alive_libraries);

    LOGD("axsn_jni_load alive libs=%d", libs);
    LOGI("axsn_jni_load done");
    return JNI_VERSION_1_6;
}

void axsn_jni_unload(JavaVM *vm, void *reserved) {
    LOGI("axsn_jni_unload");
    JavaVM *expected = vm;
    (void)reserved;

    atomic_fetch_sub_explicit(&g_axsn_jni_alive_libraries, 1, memory_order_acq_rel);
    int libs = atomic_load(&g_axsn_jni_alive_libraries);
    LOGD("axsn_jni_unload alive libs=%d", libs);
    if (libs > 0) return;

    (void)atomic_compare_exchange_strong_explicit(
        &g_axsn_java_vm,
        &expected,
        NULL,
        memory_order_acq_rel,
        memory_order_acquire
    );

    LOGI("axsn_jni_unload done");
}
