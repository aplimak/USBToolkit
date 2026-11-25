//
// Created by aeliux on 11/24/26.
//

#include "../internal.h"

#include <axsn/jni.h>

static int dtoInit(const axsn_jni_env *jenv) {
    LOGV("dtoInit");

    JNIEnv *env = jenv->env;

    if (usbtk_dto_init(env) != 0) {
        if (env->ExceptionCheck()) {
            char error[256];
            axsn_get_pending_exception(env, error, sizeof(error));
            LOGE("error in usbtk_dto_init: %s", error);
        } else {
            LOGE("usbtk_dto_init failed");
        }
        usbtk_dto_release(env);
        return JNI_ERR;
    }

    LOGV("dtoInit done");

    return JNI_OK;
}

static int dtoRelease(const axsn_jni_env *jenv) {
    LOGV("dtoInit");

    JNIEnv *env = jenv->env;

    usbtk_dto_release(env);

    LOGV("dtoInit done");
    return JNI_OK;
}

[[maybe_unused]]
const void *const initializer = []() noexcept {
    axsn_jni_hook initHook = &dtoInit;
    axsn_jni_hook releaseHook = &dtoRelease;

    LOGV("register jni load hook %p", initHook);
    axsn_jni_register_hook(initHook, AXSN_JNI_LOAD);

    LOGV("register jni unload hook %p", releaseHook);
    axsn_jni_register_hook(releaseHook, AXSN_JNI_UNLOAD);
    return nullptr;
}();
