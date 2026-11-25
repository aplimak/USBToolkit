//
// Created by aeliux on 11/24/26.
//

// This file is compiled by both entry and runtime, so each get separate registry
// Runtime store the load hooks and entry store unload hooks

#include "axsn/jni.h"

#include <axsn/log.h>
#include <vector>
#include <mutex>

namespace {
    const axsn_jni_hook_type hookType = AXSN_JNI_RUNTIME ? AXSN_JNI_LOAD : AXSN_JNI_UNLOAD;
    const char *hookTypeName = AXSN_JNI_RUNTIME ? "load" : "unload";

    enum class HookState {
        NotStarted,
        Running,
        Completed,
        Failed
    };

    struct HookEntry {
        axsn_jni_hook function;
        HookState state = HookState::NotStarted;

        HookEntry(axsn_jni_hook function) : function(function) {}
        constexpr const char* getStateName() noexcept {
            switch (state) {
                case HookState::NotStarted: return "NotStarted";
                case HookState::Running:    return "Running";
                case HookState::Completed:  return "Completed";
                case HookState::Failed:     return "Failed";
            }

            return "Unknown";
        }

        int call(const axsn_jni_env *jenv) {
            if (!function || state != HookState::NotStarted) return JNI_ERR;
            state = HookState::Running;
            int rc = (*function)(jenv);
            state = rc == JNI_OK ? HookState::Completed : HookState::Failed;
            return rc;
        }
    };

    struct HookRegistry {
        std::vector<HookEntry> entries;
        std::mutex mutex;
    };

    HookRegistry& getRegistry() {
        static HookRegistry registry;
        return registry;
    }
}

void _axsn_jni_register_hook(axsn_jni_hook hook) {
    try {
        auto& registry = getRegistry();
        std::lock_guard<std::mutex> lock(registry.mutex);
        LOGD("registry=%p", static_cast<void*>(&registry));
        registry.entries.emplace_back(hook);
        LOGV("jni %s hook %p registered", hookTypeName, hook);
    }
    catch (...) {
        LOGE("An exception tried to cross boundary in _axsn_jni_register_hook");
    }
}

int _axsn_jni_call_hooks(const axsn_jni_env *jenv) {
    try {
        auto& registry = getRegistry();
        std::lock_guard<std::mutex> lock(registry.mutex);
        int rc = JNI_OK;
        LOGD("registry=%p", static_cast<void*>(&registry));

        LOGV("jni %s hooks=%zu", hookTypeName, registry.entries.size());

        for (HookEntry& entry : registry.entries) {
            bool skipped = entry.state != HookState::NotStarted;
            LOGV("jni %s hook %p %s, state=%s", hookTypeName, entry.function, skipped ? "skip" : "call", entry.getStateName());
            if (skipped) continue;
            try {
                rc = entry.call(jenv);
                LOGV("jni %s hook %p %s", hookTypeName, entry.function, entry.getStateName());
                if (rc != JNI_OK) {
                    LOGE("jni %s hook %p failed", hookTypeName, entry.function);
#if AXSN_JNI_RUNTIME
                    return rc;
#endif
                }
            }
            catch (...) {
                LOGE("jni %s hook %p threw exception", hookTypeName, entry.function);
#if AXSN_JNI_RUNTIME
                return JNI_ERR;
#endif
            }
        }

        return rc;
    }
    catch (...) {
        LOGE("An exception tried to cross boundary in _axsn_jni_call_hooks");
        return JNI_ERR;
    }
}
