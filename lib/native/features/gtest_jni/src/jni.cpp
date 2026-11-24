#include <axsn/log.h>
#include <gtest/gtest.h>
#include <jni.h>
#include <mutex>
#include <string>
#include <vector>

namespace {
namespace axsn::GTest {
class LogcatPrinter final : public testing::EmptyTestEventListener {
public:
  void OnTestSuiteStart(const testing::TestSuite &info) override {
    LOG(ANDROID_LOG_INFO, "[SUITE] %s", info.name());
  }

  void OnTestStart(const testing::TestInfo &info) override {
    LOG(ANDROID_LOG_INFO, "[RUN] %s.%s", info.test_suite_name(), info.name());
  }

  void OnTestPartResult(const testing::TestPartResult &result) override {
    if (result.failed()) {
      LOG(ANDROID_LOG_ERROR, "[FAIL] %s:%d\n%s",
          result.file_name() ? result.file_name() : "<unknown>",
          result.line_number(), result.summary() ? result.summary() : "");
    }
  }

  void OnTestEnd(const testing::TestInfo &info) override {
    auto result = info.result();
    if (result->Failed()) {
      LOG(ANDROID_LOG_ERROR, "[FAILED] %s.%s (%ld ms)", info.test_suite_name(),
          info.name(), result->elapsed_time());
    } else if (result->Skipped()) {
      LOG(ANDROID_LOG_WARN, "[SKIP] %s.%s (%ld ms)", info.test_suite_name(),
          info.name(), result->elapsed_time());
    } else {
      LOG(ANDROID_LOG_INFO, "[OK] %s.%s (%ld ms)", info.test_suite_name(),
          info.name(), result->elapsed_time());
    }
  }
};

class GTestJUnitListener final : public testing::EmptyTestEventListener {

public:
  GTestJUnitListener(JNIEnv *env, jobject bridge) : env_(env), bridge_(bridge) {
    jclass cls = env_->GetObjectClass(bridge_);

    if (!cls) {
      return;
    }

    on_started_ =
        env_->GetMethodID(cls, "onTestStarted", "(Ljava/lang/String;)V");

    on_failure_ = env_->GetMethodID(cls, "onTestFailure",
                                    "(Ljava/lang/String;Ljava/lang/String;)V");

    on_finished_ =
        env_->GetMethodID(cls, "onTestFinished", "(Ljava/lang/String;)V");

    on_skipped_ = env_->GetMethodID(cls, "onTestSkipped",
                                    "(Ljava/lang/String;Ljava/lang/String;)V");

    env_->DeleteLocalRef(cls);
  }

  bool valid() const {
    return on_started_ && on_failure_ && on_finished_ && on_skipped_ &&
           !env_->ExceptionCheck();
  }

  void OnTestStart(const testing::TestInfo &test) override {
    callOne(on_started_, testName(test));
  }

  void OnTestPartResult(const testing::TestPartResult &result) override {

    if (!result.failed()) {
      return;
    }

    const auto *test = testing::UnitTest::GetInstance()->current_test_info();

    if (!test) {
      return;
    }

    std::string message;

    if (result.file_name()) {
      message += result.file_name();
      message += ':';
      message += std::to_string(result.line_number());
      message += ": ";
    }

    message += result.message();

    callTwo(on_failure_, testName(*test), message);
  }

  void OnTestEnd(const testing::TestInfo &test) override {
    if (test.result()->Skipped()) {
      std::string reason;

      for (int i = 0; i < test.result()->total_part_count(); ++i) {

        const auto &part = test.result()->GetTestPartResult(i);

        if (part.type() == testing::TestPartResult::kSkip) {
          reason = part.message();
          break;
        }
      }

      callTwo(on_skipped_, testName(test), reason);
    } else {
      callOne(on_finished_, testName(test));
    }
  }

private:
  JNIEnv *env_;
  jobject bridge_;

  jmethodID on_started_ = nullptr;
  jmethodID on_failure_ = nullptr;
  jmethodID on_finished_ = nullptr;
  jmethodID on_skipped_ = nullptr;

  static std::string testName(const testing::TestInfo &test) {

    return std::string(test.test_suite_name()) + "." + test.name();
  }

  void callOne(jmethodID method, const std::string &name) {

    if (env_->ExceptionCheck()) {
      return;
    }

    jstring jname = env_->NewStringUTF(name.c_str());

    if (!jname) {
      return;
    }

    env_->CallVoidMethod(bridge_, method, jname);

    env_->DeleteLocalRef(jname);
  }

  void callTwo(jmethodID method, const std::string &name,
               const std::string &message) {

    if (env_->ExceptionCheck()) {
      return;
    }

    jstring jname = env_->NewStringUTF(name.c_str());
    jstring jmessage = env_->NewStringUTF(message.c_str());

    if (!jname || !jmessage) {
      if (jname) {
        env_->DeleteLocalRef(jname);
      }

      if (jmessage) {
        env_->DeleteLocalRef(jmessage);
      }

      return;
    }

    env_->CallVoidMethod(bridge_, method, jname, jmessage);

    env_->DeleteLocalRef(jname);
    env_->DeleteLocalRef(jmessage);
  }
};

std::once_flag g_init;
std::atomic_flag g_has_run = ATOMIC_FLAG_INIT;

void initialize_gtest() {
  std::call_once(g_init, [] { testing::InitGoogleTest(); });
}

std::string get_test_name(const testing::TestInfo &test) {
  return std::string(test.test_suite_name()) + "." + test.name();
}

jobjectArray to_java_array(JNIEnv *env,
                           const std::vector<std::string> &values) {
  jclass string_class = env->FindClass("java/lang/String");

  jobjectArray result = env->NewObjectArray(static_cast<jsize>(values.size()),
                                            string_class, nullptr);

  for (jsize i = 0; i < static_cast<jsize>(values.size()); ++i) {
    jstring value = env->NewStringUTF(values[static_cast<size_t>(i)].c_str());

    env->SetObjectArrayElement(result, i, value);

    env->DeleteLocalRef(value);
  }

  env->DeleteLocalRef(string_class);

  return result;
}

} // namespace axsn::GTest
} // namespace

extern "C" JNIEXPORT jobjectArray JNICALL
Java_ir_aeliux_gtestrunner_GTestRunner_listTests(JNIEnv *env, jclass) {
  axsn::GTest::initialize_gtest();

  std::vector<std::string> names;

  const auto *unit = testing::UnitTest::GetInstance();

  for (int i = 0; i < unit->total_test_suite_count(); ++i) {
    const auto *suite = unit->GetTestSuite(i);

    for (int j = 0; j < suite->total_test_count(); ++j) {
      const auto *test = suite->GetTestInfo(j);

      names.push_back(axsn::GTest::get_test_name(*test));
    }
  }

  return axsn::GTest::to_java_array(env, names);
}

extern "C" JNIEXPORT jint JNICALL Java_ir_aeliux_gtestrunner_GTestRunner_runAll(
    JNIEnv *env, jclass clazz, jobjectArray selected, jobject bridge) {

  (void)clazz;
  axsn::GTest::initialize_gtest();
  if (axsn::GTest::g_has_run.test_and_set()) {
    jclass exception = env->FindClass("java/lang/IllegalStateException");

    env->ThrowNew(exception, "GoogleTest has already executed in this process");

    return 0;
  }

  std::string filter;

  const jsize count = env->GetArrayLength(selected);

  for (jsize i = 0; i < count; ++i) {
    auto value = static_cast<jstring>(env->GetObjectArrayElement(selected, i));

    const char *chars = env->GetStringUTFChars(value, nullptr);

    if (!filter.empty())
      filter += ':';

    filter += chars;

    env->ReleaseStringUTFChars(value, chars);
    env->DeleteLocalRef(value);
  }

  if (filter.empty())
    return -1;

  GTEST_FLAG_SET(filter, filter);

  // Install logcat logger.
  testing::UnitTest::GetInstance()->listeners().Append(
      new axsn::GTest::LogcatPrinter());

  // Install the JUnit event bridge.
  auto *listener = new axsn::GTest::GTestJUnitListener(env, bridge);

  if (!listener->valid()) {
    delete listener;
    return -1;
  }

  auto &listeners = testing::UnitTest::GetInstance()->listeners();

  listeners.Append(listener);

  // JUnit events are now emitted DURING native execution.
  const int result = RUN_ALL_TESTS();

  // Remove and destroy our listener after execution.
  delete listeners.Release(listener);

  return result;
}
