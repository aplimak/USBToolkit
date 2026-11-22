//
// Created by aeliux on 11/22/26.
//

#include "JniCallable.hpp"
#include <axsn/JniTestFixture.hpp>
#include <gtest/gtest.h>

class PointerWrapperJni: public axsn::gtest::JniTestFixture {};

TEST_F(PointerWrapperJni, CallbackTest) {
    jclass cls = env->FindClass("ir/aeliux/ptrw/GTests");
    ASSERT_TRUE(cls != nullptr);
    jfieldID jfid = env->GetStaticFieldID(cls, "testCallback", "Ljava/lang/Runnable;");
    ASSERT_TRUE(jfid != nullptr);
    jobject runnable = env->GetStaticObjectField(cls, jfid);
    ASSERT_TRUE(runnable != nullptr);
    std::shared_ptr<ptrw::JniCallable> callable = ptrw::JniCallable::fromRunnable(env.getVM(), runnable);
    ASSERT_FALSE(callable->call());
    ASSERT_TRUE(env->ExceptionCheck());
    jthrowable exc = env->ExceptionOccurred();
    ASSERT_TRUE(exc != nullptr);
    env->ExceptionClear();
    jclass targetExcCls = env->FindClass("java/util/DuplicateFormatFlagsException");
    ASSERT_TRUE(targetExcCls != nullptr);
    ASSERT_TRUE(env->IsInstanceOf(exc, targetExcCls));
}
