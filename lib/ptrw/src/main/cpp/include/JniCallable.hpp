//
// Created by aeliux on 11/19/26.
//

#pragma once

#include "shared.hpp"
#include <jni.h>
#include <memory>

namespace ptrw {

    class PTRW_API JniCallable
    {
    public:
        ~JniCallable() noexcept;

        JniCallable(const JniCallable&) = delete;
        JniCallable& operator=(const JniCallable&) = delete;

        JniCallable(JniCallable&&) noexcept;
        JniCallable& operator=(JniCallable&&) noexcept;

        static JniCallable fromRunnable(JavaVM *jvm, jobject runnable);
        bool call();
    private:
        JniCallable(JavaVM *jvm, jobject object, jmethodID mid);
        struct Impl;
        std::unique_ptr<Impl> m_impl;
    };

} // ptrw
