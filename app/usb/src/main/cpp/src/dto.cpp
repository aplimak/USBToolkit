//
// Created by aeliux on 11/24/26.
//

#include "../internal.h"

#include <axsn/jni.h>

static int dtoInit(JavaVM *vm, void *reserved) {
    (void) reserved;
    LOGV("dtoInit");

    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("dtoInit: GetEnv failed");
        return JNI_ERR;
    }

    if (usbtk_dto_init(env) != 0) {
        usbtk_dto_release(env);
        char error[256];
        axsn_get_pending_exception(env, error, sizeof(error));
        LOGE("error in usbtk_dto_init: %s", error);
        return JNI_ERR;
    }

    LOGV("dtoInit done");

    return JNI_OK;
}

static int dtoRelease(JavaVM *vm, void *reserved) {
    (void)reserved;
    JNIEnv *env = nullptr;

    LOGV("dtoInit");

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        /* Thread torn down / VM going away — best effort only. */
        LOGE("JNI_OnUnload: GetEnv failed, skipping cleanup");
        return JNI_ERR;
    }

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
