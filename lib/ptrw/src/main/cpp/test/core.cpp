//
// Created by aeliux on 11/20/26.
//

#include <axsn/gtest.hpp>
#include <jni.h>
#include <gtest/gtest.h>

#include <mutex>
#include <string>
#include <vector>

namespace {

    std::once_flag g_init;
    std::atomic_flag g_has_run = ATOMIC_FLAG_INIT;

    void initialize_gtest()
    {
        std::call_once(g_init, [] {
            testing::InitGoogleTest();
        });
    }

    std::string get_test_name(const testing::TestInfo& test)
    {
        return std::string(test.test_suite_name())
               + "."
               + test.name();
    }

    jobjectArray to_java_array(
            JNIEnv* env,
            const std::vector<std::string>& values)
    {
        jclass string_class = env->FindClass("java/lang/String");

        jobjectArray result = env->NewObjectArray(
                static_cast<jsize>(values.size()),
                string_class,
                nullptr
        );

        for (jsize i = 0; i < static_cast<jsize>(values.size()); ++i) {
            jstring value = env->NewStringUTF(values[i].c_str());

            env->SetObjectArrayElement(result, i, value);

            env->DeleteLocalRef(value);
        }

        env->DeleteLocalRef(string_class);

        return result;
    }

} // namespace

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_ir_aeliux_gtestrunner_GTestRunner_listTests(
        JNIEnv* env,
        jclass)
{
    initialize_gtest();

    std::vector<std::string> names;

    const auto* unit = testing::UnitTest::GetInstance();

    for (int i = 0; i < unit->total_test_suite_count(); ++i) {
        const auto* suite = unit->GetTestSuite(i);

        for (int j = 0; j < suite->total_test_count(); ++j) {
            const auto* test = suite->GetTestInfo(j);

            names.push_back(get_test_name(*test));
        }
    }

    return to_java_array(env, names);
}

extern "C"
JNIEXPORT jint JNICALL
Java_ir_aeliux_gtestrunner_GTestRunner_runAll(
        JNIEnv* env,
        jclass clazz,
        jobjectArray selected,
        jobject bridge)
{
    initialize_gtest();

    if (g_has_run.test_and_set()) {
        jclass exception = env->FindClass(
                "java/lang/IllegalStateException"
        );

        env->ThrowNew(
                exception,
                "GoogleTest has already executed in this process"
        );

        return 0;
    }

    std::string filter;

    const jsize count = env->GetArrayLength(selected);

    for (jsize i = 0; i < count; ++i) {
        auto value = static_cast<jstring>(
                env->GetObjectArrayElement(selected, i)
        );

        const char* chars =
                env->GetStringUTFChars(value, nullptr);

        if (!filter.empty())
            filter += ':';

        filter += chars;

        env->ReleaseStringUTFChars(value, chars);
        env->DeleteLocalRef(value);
    }

    if (filter.empty())
        return -1;

    GTEST_FLAG_SET(filter, filter);

    AXSN_GTEST_LOGCAT_REGISTER;

    // Install the JUnit event bridge.
    auto* listener = new GTestJUnitListener(env, bridge);

    if (!listener->valid()) {
        delete listener;
        return -1;
    }

    auto& listeners =
            testing::UnitTest::GetInstance()->listeners();

    listeners.Append(listener);

    // JUnit events are now emitted DURING native execution.
    const int result = RUN_ALL_TESTS();

    // Remove and destroy our listener after execution.
    delete listeners.Release(listener);

    return result;
}
