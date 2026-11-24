//
// Created by aeliux on 11/22/26.
//

#pragma once

#include <axsn/jni.h>
#include <gtest/gtest.h>

namespace axsn::gtest {

        class JniTestFixture : public testing::Test
        {
        protected:
            jni::ScopedEnv env;

            void SetUp() override {
                ASSERT_TRUE(env) << "Java context required in a jni test";
            }
        };

} // axsn::gtest
