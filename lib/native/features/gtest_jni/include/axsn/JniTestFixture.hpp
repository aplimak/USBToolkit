//
// Created by aeliux on 11/22/26.
//

#pragma once

#include <axsn/jni.hpp>
#include <gtest/gtest.h>

namespace axsn::gtest {

        class JniTestFixture : public testing::Test
        {
        protected:
            jni::ScopedEnv java;

            void SetUp() override {
                ASSERT_TRUE(java) << "Java context required in a jni test";
            }
        };

} // axsn::gtest
