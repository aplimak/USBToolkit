//
// Created by aeliux on 9/23/26.
//

#ifndef AXSN_LOG_H
#define AXSN_LOG_H

#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Each .c can #define LOG_TAG before including this header.
   If it doesn't, the library-wide tag is used. */
#ifndef LOG_TAG
#define LOG_TAG "libaxsn"
#endif

#define LOG(prio, ...) \
    __android_log_print((prio), LOG_TAG, __VA_ARGS__)

#define LOGV(...) LOG(ANDROID_LOG_VERBOSE, __VA_ARGS__)
#define LOGD(...) LOG(ANDROID_LOG_DEBUG,   __VA_ARGS__)
#define LOGI(...) LOG(ANDROID_LOG_INFO,    __VA_ARGS__)
#define LOGW(...) LOG(ANDROID_LOG_WARN,    __VA_ARGS__)
#define LOGE(...) LOG(ANDROID_LOG_ERROR,   __VA_ARGS__)

/* Strip debug/verbose in release builds. */
#ifdef NDEBUG
#  undef  LOGD
#  undef  LOGV
#  define LOGD(...) ((void)0)
#  define LOGV(...) ((void)0)
#endif

#ifdef __cplusplus
#define AXSN_GTEST_LOGCAT \
class LogcatPrinter : public testing::EmptyTestEventListener {\
        public:\
        void OnTestSuiteStart(const testing::TestSuite& info) override {\
            LOG(ANDROID_LOG_INFO, "[SUITE] %s", info.name());\
        }\
        void OnTestStart(const testing::TestInfo& info) override {\
            LOG(ANDROID_LOG_INFO, "[RUN] %s.%s",\
                      info.test_suite_name(), info.name());\
        }\
        void OnTestPartResult(const testing::TestPartResult& result) override {\
            if (result.failed()) {\
                LOG(ANDROID_LOG_ERROR, "[FAIL] %s:%d\n%s",\
                          result.file_name() ? result.file_name() : "<unknown>",\
                          result.line_number(),\
                          result.summary() ? result.summary() : "");\
            }\
        }\
        void OnTestEnd(const testing::TestInfo& info) override {\
            auto result = info.result();\
            if (result->Failed()) {\
                LOG(ANDROID_LOG_ERROR, "[FAILED] %s.%s (%ld ms)",\
                          info.test_suite_name(), info.name(), result->elapsed_time());\
            } else if (result->Skipped()) {\
                LOG(ANDROID_LOG_WARN, "[SKIP] %s.%s (%ld ms)",\
                          info.test_suite_name(), info.name(), result->elapsed_time());\
            } else {\
                LOG(ANDROID_LOG_INFO, "[OK] %s.%s (%ld ms)",\
                          info.test_suite_name(), info.name(), result->elapsed_time());\
            }\
        }\
}
#define AXSN_GTEST_LOGCAT_REGISTER testing::UnitTest::GetInstance()->listeners().Append(new LogcatPrinter())
#endif

#ifdef __cplusplus
}
#endif

#endif //AXSN_LOG_H
