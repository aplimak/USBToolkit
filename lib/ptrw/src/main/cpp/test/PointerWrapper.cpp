//
// Created by aeliux on 11/18/26.
//

#define LOG_TAG "libptrwtests:PointerWrapper.cpp"

#include "PointerWrapper.hpp"
#include <gtest/gtest.h>

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
                                                                 std::nullopt,
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
    EXPECT_THROW((void)w.getRawPointer(), std::runtime_error);
    EXPECT_TRUE(data.deleted);
    EXPECT_TRUE(w.isClosed());
    EXPECT_TRUE(w.isOwnerThread());
}
