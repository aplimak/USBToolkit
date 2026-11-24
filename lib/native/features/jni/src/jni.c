#include <axsn/jni.h>

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

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved);
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved);

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

jint axsn_jni_env_acquire(axsn_jni_env *out_env, const char *thread_name) {
    JavaVM *vm;
    void *raw_env = NULL;
    jint result;

    if (out_env == NULL) {
        return JNI_EINVAL;
    }

    out_env->vm = NULL;
    out_env->env = NULL;
    out_env->attached_by_axsn = 0;

    vm = axsn_jni_get_vm();
    if (vm == NULL) {
        return JNI_ERR;
    }

    result = (*vm)->GetEnv(vm, &raw_env, JNI_VERSION_1_6);
    if (result == JNI_OK) {
        out_env->vm = vm;
        out_env->env = (JNIEnv *)raw_env;
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

    out_env->vm = vm;
    out_env->env = (JNIEnv *)raw_env;
    out_env->attached_by_axsn = 1;
    return JNI_OK;
}

void axsn_jni_env_release(axsn_jni_env *state) {
    if (state == NULL) {
        return;
    }

    if (state->attached_by_axsn && state->vm != NULL) {
        if ((*state->env)->ExceptionCheck(state->env)) {
            char error_buf[1024];
            axsn_get_pending_exception(state->env, error_buf, sizeof(error_buf));
            LOGE("Unhandled pending exception on a jvm attached thread:\n%s", error_buf);
        }
        (void)(*state->vm)->DetachCurrentThread(state->vm);
    }

    state->vm = NULL;
    state->env = NULL;
    state->attached_by_axsn = 0;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGV("JNI_OnLoad");
    void *raw_env = NULL;
    jint result;

    (void)reserved;

    if (vm == NULL) {
        return JNI_ERR;
    }

    result = (*vm)->GetEnv(vm, &raw_env, JNI_VERSION_1_6);
    if (result != JNI_OK || raw_env == NULL) {
        return JNI_ERR;
    }

    atomic_store_explicit(&g_axsn_java_vm, vm, memory_order_release);
    LOGI("JNI_OnLoad done");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    LOGV("JNI_OnUnload");
    JavaVM *expected = vm;
    (void)reserved;

    (void)atomic_compare_exchange_strong_explicit(
        &g_axsn_java_vm,
        &expected,
        NULL,
        memory_order_acq_rel,
        memory_order_acquire
    );

    LOGI("JNI_OnUnload done");
}
