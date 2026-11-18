//
// Created by aeliux on 11/18/26.
//

#define LOG_TAG "libptrwtests:tests_jni.cpp"

#include <axsn/log.h>
#include <gtest/gtest.h>
#include <jni.h>

AXSN_GTEST_LOGCAT;

TEST(PointerWrapperTest, Hello)
{
    ASSERT_EQ(1+2, 3);
}

extern "C"
JNIEXPORT jint JNICALL
Java_ir_aeliux_ptrw_NativeTests_runNativeTests(JNIEnv *env, jclass clazz)
{
    testing::InitGoogleTest();

    AXSN_GTEST_LOGCAT_REGISTER;

    return RUN_ALL_TESTS();
}
