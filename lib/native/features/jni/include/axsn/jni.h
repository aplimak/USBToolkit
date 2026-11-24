#pragma once

#include <jni.h>
#include <axsn/export.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    JavaVM *vm;
    JNIEnv *env;
    int attached_by_axsn;
} axsn_jni_env;

/**
 * JNI Hook function signature.
 * @note return non-zero to fail the load phase, ignored in unload phase.
 * @note exceptions will be treated as fail.
 */
typedef int (*axsn_jni_hook)(JavaVM *vm, void *reserved);

/**
 * Type that indicates when the hook is called.
 */
typedef enum {
    AXSN_JNI_LOAD,
    AXSN_JNI_UNLOAD,
} axsn_jni_hook_type;

AXSN_EXPORT void axsn_jni_register_load_hook(axsn_jni_hook hook);
AXSN_LOCAL void axsn_jni_register_unload_hook(axsn_jni_hook hook);

/**
 * Register a function to be called on jni load or unload phase.
 * @param hook Pointer to the hood function.
 * @param type Type of the hook, either load or unload.
 * @note Hook is able to fail the load phase by throwing exception or returning non zero value.
 */
inline void axsn_jni_register_hook(axsn_jni_hook hook, axsn_jni_hook_type type) {
    if (type == AXSN_JNI_LOAD) {
        axsn_jni_register_load_hook(hook);
    } else {
        axsn_jni_register_unload_hook(hook);
    }
}

/* Returns the VM captured by this process, or NULL before load. */
AXSN_EXPORT JavaVM *axsn_jni_get_vm(void);

/* Returns this thread's JNIEnv only when it is already attached. */
AXSN_EXPORT JNIEnv *axsn_jni_get_env(void);

/*
 * Obtains the current thread's JNIEnv, attaching it if necessary.
 * On success, out_env->attached_by_axsn records whether release must detach.
 * Pass NULL as thread_name to use the VM's default thread name.
 */
AXSN_EXPORT jint axsn_jni_env_acquire(
    axsn_jni_env *out_env,
    const char *thread_name
);

/* Detaches only when axsn_jni_env_acquire() attached the current thread. */
AXSN_EXPORT void axsn_jni_env_release(axsn_jni_env *env);

#ifdef __cplusplus
} /* extern "C" */
#endif

#ifdef __cplusplus
namespace axsn::jni {

class ScopedEnv final {
public:
    explicit ScopedEnv(const char *thread_name = nullptr) noexcept {
        status_ = axsn_jni_env_acquire(&state_, thread_name);
    }

    ~ScopedEnv() noexcept {
        axsn_jni_env_release(&state_);
    }

    ScopedEnv(const ScopedEnv &) = delete;
    ScopedEnv &operator=(const ScopedEnv &) = delete;
    ScopedEnv(ScopedEnv &&) = delete;
    ScopedEnv &operator=(ScopedEnv &&) = delete;

    [[nodiscard]] jint status() const noexcept { return status_; }
    [[nodiscard]] JNIEnv *get() const noexcept { return state_.env; }
    [[nodiscard]] JavaVM *getVM() const noexcept { return state_.vm; }
    [[nodiscard]] explicit operator bool() const noexcept {
        return status_ == JNI_OK && state_.env != nullptr;
    }
    [[nodiscard]] JNIEnv *operator->() const noexcept { return state_.env; }

private:
    axsn_jni_env state_{};
    jint status_ = JNI_ERR;
};

} // namespace axsn::jni
#endif
