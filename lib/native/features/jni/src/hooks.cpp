//
// Created by aeliux on 11/24/26.
//

#include "axsn/jni.h"
#include "internal.h"

#include <axsn/log.h>
#include <vector>
#include <mutex>

static std::vector<axsn_jni_hook> loadHooks;
static std::vector<axsn_jni_hook> unloadHooks;
static std::mutex hookMutex;

static constexpr const char *getTypeName(axsn_jni_hook_type type) {
    return type == AXSN_JNI_LOAD ? "load" : "unload";
}

void axsn_jni_register_hook(axsn_jni_hook hook, axsn_jni_hook_type type) {
    try {
        std::lock_guard<std::mutex> lock(hookMutex);
        if (type == AXSN_JNI_LOAD) {
            loadHooks.push_back(hook);
        } else {
            unloadHooks.push_back(hook);
        }
        LOGV("jni %s hook %p registered", getTypeName(type), hook);
    }
    catch (...) {
        LOGE("An exception tried to cross C++ boundary in axsn_jni_register_hook");
    }
}

int axsn_jni_call_hooks(JavaVM *vm, void *reserved, axsn_jni_hook_type type) {
    try {
        std::lock_guard<std::mutex> lock(hookMutex);
        int rc = JNI_OK;

        std::vector<axsn_jni_hook>* target = nullptr;
        if (type == AXSN_JNI_LOAD) {
            target = &loadHooks;
        } else {
            target = &unloadHooks;
        }

        for (axsn_jni_hook hook : *target) {
            try {
                rc = hook(vm, reserved);
                if (rc != JNI_OK) {
                    LOGE("jni %s hook %p failed", getTypeName(type), hook);
                    if (type == AXSN_JNI_LOAD) {
                        return rc;
                    }
                }
            }
            catch (...) {
                LOGE("jni %s hook %p threw exception", getTypeName(type), hook);
                if (type == AXSN_JNI_LOAD) {
                    return JNI_ERR;
                }
            }
        }

        return rc;
    }
    catch (...) {
        LOGE("An exception tried to cross C++ boundary in axsn_jni_call_hooks");
        return JNI_ERR;
    }
}
