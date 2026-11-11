//
// Created by aeliux on 9/23/26.
//

#ifndef AXSN_LOG_H
#define AXSN_LOG_H

#ifdef __cplusplus
extern "C" {
#endif

#include <android/log.h>

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
}
#endif

#endif //AXSN_LOG_H
