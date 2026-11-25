//
// Created by aeliux on 11/24/26.
//

#include "axsn/jni.h"
#include "axsn/jni_internal.h"
#include "shared.hpp"
#include <jni.h>
#include <axsn/log.h>

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGI("JNI_OnLoad");

    int rc = axsn_jni_load(vm, reserved);
    if (rc == JNI_ERR) {
        return rc;
    }

    LOGI("JNI_OnLoad done");
    return rc;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    LOGI("JNI_OnUnload");

    LOGI("JNI_OnUnload call hooks");

    void *raw_env;
    int result = vm->GetEnv(&raw_env, JNI_VERSION_1_6);
    if (result != JNI_OK || raw_env == nullptr) {
        LOGE("JNI_OnUnload: GetEnv failed");
    } else {
        const axsn_jni_env jenv = {
                .vm = vm,
                .env = (JNIEnv *)raw_env,
                .attached = 0
        };
        _axsn_jni_call_hooks(&jenv);
    }

    axsn_jni_unload(vm, reserved);

    LOGI("JNI_OnUnload done");
}

void axsn_jni_register_unload_hook(axsn_jni_hook hook) {
    _axsn_jni_register_hook(hook);
}
