//
// Created by aeliux on 11/20/26.
//

#pragma once

#ifndef AXSN_HAVE_GTEST
#error "GTest is required"
#endif

#include "log.h"

#include <gtest/gtest.h>

#define AXSN_GTEST_LOGCAT_REGISTER testing::UnitTest::GetInstance()->listeners().Append(new LogcatPrinter())

class LogcatPrinter : public testing::EmptyTestEventListener {
        public:
        void OnTestSuiteStart(const testing::TestSuite& info) override {
            LOG(ANDROID_LOG_INFO, "[SUITE] %s", info.name());
        }
        void OnTestStart(const testing::TestInfo& info) override {
            LOG(ANDROID_LOG_INFO, "[RUN] %s.%s",
                      info.test_suite_name(), info.name());
        }
        void OnTestPartResult(const testing::TestPartResult& result) override {
            if (result.failed()) {
                LOG(ANDROID_LOG_ERROR, "[FAIL] %s:%d\n%s",
                          result.file_name() ? result.file_name() : "<unknown>",
                          result.line_number(),
                          result.summary() ? result.summary() : "");
            }
        }
        void OnTestEnd(const testing::TestInfo& info) override {
            auto result = info.result();
            if (result->Failed()) {
                LOG(ANDROID_LOG_ERROR, "[FAILED] %s.%s (%ld ms)",
                          info.test_suite_name(), info.name(), result->elapsed_time());
            } else if (result->Skipped()) {
                LOG(ANDROID_LOG_WARN, "[SKIP] %s.%s (%ld ms)",
                          info.test_suite_name(), info.name(), result->elapsed_time());
            } else {
                LOG(ANDROID_LOG_INFO, "[OK] %s.%s (%ld ms)",
                          info.test_suite_name(), info.name(), result->elapsed_time());
            }
        }
};
