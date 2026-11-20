//
// Created by aeliux on 11/18/26.
//

#define LOG_TAG "libptrwtests:tests_jni.cpp"

#include "PointerWrapper.hpp"
#include <axsn/gtest.hpp>
#include <gtest/gtest.h>
#include <jni.h>

using namespace ptrw;

struct dummy_data {
    bool deleted;
};

class DummyPointerWrapper: public PointerWrapper
{
public:
    static void deleter(void *ptr) noexcept {
        dummy_data *data = static_cast<dummy_data *>(ptr);
        data->deleted = true;
    }

    explicit DummyPointerWrapper(void *pointer) : PointerWrapper(pointer,
                                                                 &DummyPointerWrapper::deleter,
                                                                 nullptr,
                                                                 PointerWrapperFlags::ThreadConfined)
    {

    }
};

TEST(PointerWrapperTest, BasicTest)
{
    dummy_data data;
    DummyPointerWrapper w(&data);
    ASSERT_EQ(w.getRawPointer(), &data);
    EXPECT_FALSE(data.deleted);
    EXPECT_FALSE(w.isClosed());
    EXPECT_TRUE(w.isOwnerThread());
    ASSERT_NO_THROW(w.close());
    EXPECT_THROW(w.getRawPointer(), std::runtime_error);
    EXPECT_TRUE(data.deleted);
    EXPECT_TRUE(w.isClosed());
    EXPECT_TRUE(w.isOwnerThread());
}

extern "C"
JNIEXPORT jint JNICALL
Java_ir_aeliux_ptrw_NativeTests_runNativeTests(JNIEnv *env, jclass clazz)
{
    testing::InitGoogleTest();

    AXSN_GTEST_LOGCAT_REGISTER;

    return RUN_ALL_TESTS();
}
