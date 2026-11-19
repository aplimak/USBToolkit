//
// Created by aeliux on 11/19/26.
//

#pragma once

#include "shared.hpp"
#include <jni.h>

namespace ptrw {

    class PTRW_API JniCallable
    {
    public:
        ~JniCallable();

        JniCallable(const JniCallable&)            = delete;
        JniCallable& operator=(const JniCallable&) = delete;
        JniCallable(JniCallable&&)                 = delete;
        JniCallable& operator=(JniCallable&&)      = delete;

        static JniCallable fromRunnable(JavaVM *jvm, jobject runnable);
        bool call();
    private:
        JniCallable(JavaVM *jvm, jobject object, jmethodID mid);
        struct Impl;
        Impl* m_impl;
    };

} // ptrw
