#pragma once

/* Optional C++ RAII wrapper over the C ABI in <axsn/jni.h>. */
#include <axsn/jni.h>

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
    [[nodiscard]] explicit operator bool() const noexcept {
        return status_ == JNI_OK && state_.env != nullptr;
    }
    [[nodiscard]] JNIEnv *operator->() const noexcept { return state_.env; }

private:
    axsn_jni_env state_{};
    jint status_ = JNI_ERR;
};

} // namespace axsn::jni
