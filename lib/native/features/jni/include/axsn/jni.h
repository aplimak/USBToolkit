#pragma once

#include <jni.h>
#include <axsn/export.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Data structure used to store jni pointers and track native attached thread.
 */
typedef struct {
    /**
     * Pointer to JVM.
     * @note This pointer is valid for this process. it can be cached.
     */
    JavaVM *vm;
    /**
     * Pointer to the current thread's jni env.
     * @note This pointer is valid only on this thread and until the thread is died or detached from JVM. Must not be cached globally.
     */
    JNIEnv *env;
    /**
     * The value indicating whether this thread is attached to jvm by the call to axsn_jni_env_acquire or already attached.
     */
    int attached;
} axsn_jni_env;

/**
 * JNI Hook function signature.
 * @note return non-zero to fail the load phase, ignored in unload phase.
 * @note exceptions will be treated as fail.
 */
typedef int (*axsn_jni_hook)(const axsn_jni_env *jenv);

/**
 * Type that indicates when the hook is called.
 */
typedef enum {
    /**
     * Global load hook, called by jni runtime.
     */
    AXSN_JNI_LOAD,
    /**
     * Local unload hook, called by the local jni facility.
     */
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

/**
 * Get the JVM captured by this process.
 * @return Pointer to the JVM if it captured, otherwise NULL.
 */
AXSN_EXPORT JavaVM *axsn_jni_get_vm(void);

/**
 * Get the current thread's jni env.
 * @return Pointer to the jni env only if the current thread is already attached, otherwise NULL.
 * @note This function won't try to attach the current thread if its not already attached.
 */
AXSN_EXPORT JNIEnv *axsn_jni_get_env(void);

/**
 * Acquire jni jenv. either by getting current thread's jenv or attaching to JVM and then getting a jenv.
 * @param jenv out parameter to hold the axsn_jni_env.
 * @param thread_name The new for the new thread, used only when current thread is not already attached to JVM, pass NULL to use default.
 * @return JNI_OK if succeeded.
 * @note After a successful call, you must call axsn_jni_env_release to detach the thread safely if it attached by this call.
 */
AXSN_EXPORT jint axsn_jni_env_acquire(
    axsn_jni_env *jenv,
    const char *thread_name
);

/**
 * Clean up resources and detach thread if needed.
 * @param jenv Pointer to axsn_jni_env.
 */
AXSN_EXPORT void axsn_jni_env_release(axsn_jni_env *jenv);

#ifdef __cplusplus
}

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
