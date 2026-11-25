//
// Created by aeliux on 11/25/26.
//

#include "../internal.h"
#include <axsn/error.h>
#include <axsn/log.h>
#include <axsn/jni.h>

static int nativeInit(const axsn_jni_env *jenv) {
    LOGV("nativeInit");

    JNIEnv *env = jenv->env;

    if (ptrw_register_native_methods(env) != JNI_OK) {
        if (env->ExceptionCheck()) {
            char error[256];
            axsn_get_pending_exception(env, error, sizeof(error));
            LOGE("error in ptrw_register_native_methods: %s", error);
        } else {
            LOGE("ptrw_register_native_methods failed");
        }
        return JNI_ERR;
    }

    LOGV("nativeInit done");

    return JNI_OK;
}

[[maybe_unused]]
const void *const initializer = []() noexcept {
    axsn_jni_hook initHook = &nativeInit;

    LOGV("register jni load hook %p", initHook);
    axsn_jni_register_hook(initHook, AXSN_JNI_LOAD);

    return nullptr;
}();
