//
// Created by aeliux on 9/23/26.
//

#ifndef USBTOOLKIT_LOG_H
#define USBTOOLKIT_LOG_H

#include <android/log.h>

/* Each .c can #define LOG_TAG before including this header.
   If it doesn't, the library-wide tag is used. */
#ifndef LOG_TAG
#define LOG_TAG "libusbtk"
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

#endif //USBTOOLKIT_LOG_H
