//
// Created by aeliux on 9/24/26.
//

#ifndef AXSN_ERROR_H
#define AXSN_ERROR_H

#ifdef __cplusplus
extern "C" {
#endif

/*
 * error.h -- JNI error reporting.
 *
 * Contract:
 *   - Every failure that crosses the JNI boundary is a Java exception.
 *   - axsn_throw() is a no-op when an exception is already pending, so it
 *     never clobbers a JNI-originated error (NoSuchFieldError, OOM, ...).
 *   - Every throw is mirrored to logcat at ERROR level, so the trail exists
 *     even if the exception is later swallowed.
 *   - Internal-only code (helpers below the JNI layer) keeps using log.h.
 *
 * Mapping:
 *   bad input from Java               -> IllegalArgumentException
 *   lifecycle / init not done / misuse-> IllegalStateException
 *   NULL passed where non-NULL needed -> NullPointerException
 *   filesystem / configfs / usbg / libc-> IOException
 *   allocation failure                -> OutOfMemoryError
 *   anything else                     -> RuntimeException
 *
 * After any of these throws, the entry point must stop calling JNI except
 * for the small "exception-safe" set:
 *   ExceptionCheck, ExceptionOccurred, ExceptionClear, ExceptionDescribe,
 *   DeleteLocalRef, DeleteGlobalRef, DeleteWeakGlobalRef, MonitorExit,
 *   ReleaseStringUTFChars, ReleaseStringChars,
 *   ReleasePrimitiveArrayCritical, ReleaseStringCritical.
 * i.e. jump to cleanup and only run C-side teardown there.
 */

#include <jni.h>
#include <errno.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>

#include "log.h"

/* --- Exception class names ------------------------------------------ */

#define AXSN_JC_IAE "java/lang/IllegalArgumentException"
#define AXSN_JC_ISE "java/lang/IllegalStateException"
#define AXSN_JC_NPE "java/lang/NullPointerException"
#define AXSN_JC_IOE "java/io/IOException"
#define AXSN_JC_OOM "java/lang/OutOfMemoryError"
#define AXSN_JC_RTE "java/lang/RuntimeException"

#ifndef AXSN_ERR_MSG_MAX
#define AXSN_ERR_MSG_MAX 512
#endif

/* --- Core ----------------------------------------------------------- */

/*
 * Throw a Java exception with a printf-formatted message.
 *
 *   - If an exception is already pending, log and return. We never replace
 *     an existing exception; a JNI-originated one is more specific than
 *     anything we could write here.
 *   - If FindClass fails, the JVM leaves a NoClassDefFoundError pending;
 *     we return without touching it.
 *   - Message is mirrored to logcat at ERROR, regardless of outcome.
 */
__attribute__((format(printf, 3, 4))) static inline void
axsn_throw(JNIEnv *env, const char *cls, const char *fmt, ...)
{
    char msg[AXSN_ERR_MSG_MAX];
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(msg, sizeof msg, fmt, ap);
    va_end(ap);

    if ((*env)->ExceptionCheck(env)) {
        LOGE("skip throw (pending) %s: %s", cls, msg);
        return;
    }

    LOGE("throw %s: %s", cls, msg);

    jclass c = (*env)->FindClass(env, cls);
    if (c) {
        (*env)->ThrowNew(env, c, msg);
        (*env)->DeleteLocalRef(env, c);
    }
    /* else: NoClassDefFoundError already pending; leave it alone. */
}

/* --- Per-class convenience macros ----------------------------------- */

#define AXSN_THROW(env, cls, ...)  axsn_throw((env), (cls), __VA_ARGS__)

#define AXSN_THROW_IAE(env, ...)   AXSN_THROW((env), AXSN_JC_IAE, __VA_ARGS__)
#define AXSN_THROW_ISE(env, ...)   AXSN_THROW((env), AXSN_JC_ISE, __VA_ARGS__)
#define AXSN_THROW_NPE(env, ...)   AXSN_THROW((env), AXSN_JC_NPE, __VA_ARGS__)
#define AXSN_THROW_IOE(env, ...)   AXSN_THROW((env), AXSN_JC_IOE, __VA_ARGS__)
#define AXSN_THROW_OOM(env, ...)   AXSN_THROW((env), AXSN_JC_OOM, __VA_ARGS__)
#define AXSN_THROW_RTE(env, ...)   AXSN_THROW((env), AXSN_JC_RTE, __VA_ARGS__)
/* --- Pending-exception helpers -------------------------------------- */

/* True if the JVM has an exception pending. */
#define AXSN_EXC_PENDING(env) ((*env)->ExceptionCheck(env))

/* `goto label` if an exception is pending. For the entry-point cleanup
 * pattern. Label must be inside the same function. */
#define AXSN_BAIL_IF_EXC_PENDING(env, label) \
    do { if (AXSN_EXC_PENDING(env)) goto label; } while (0)

/* Pure-C guard: if `p` is NULL, throw IAE and return -1, else 0.
 * Use at the top of DTO helpers for pointer preconditions. */
static inline int
axsn_throw_if_null(JNIEnv *env, const void *p, const char *what)
{
    if (p) return 0;
    AXSN_THROW_IAE(env, "%s is NULL", what);
    return -1;
}

/* DTO/lifecycle guard: if !cond, throw ISE and return -1, else 0.
 * Example: axsn_require(env, g_jni.gadget_attrs.cls != NULL, "dto not initialized"); */
static inline int
axsn_require(JNIEnv *env, int cond, const char *what)
{
    if (cond) return 0;
    AXSN_THROW_ISE(env, "%s", what);
    return -1;
}

/* --- Domain-specific throwers --------------------------------------- */

/* Throw IOException for a failed libc call, capturing errno.
 * Call immediately after the failing call, before any other libc call
 * that could clobber errno. */
static inline void
axsn_throw_errno(JNIEnv *env, const char *what)
{
    int e = errno;
    char buf[128];
    buf[0] = '\0';

    /* POSIX strerror_r (int-returning variant, which is what bionic uses).
     * On success it writes buf; on ERANGE the string may be truncated. */
    if (strerror_r(e, buf, sizeof buf) != 0) {
        snprintf(buf, sizeof buf, "errno %d", e);
    }

    AXSN_THROW_IOE(env, "%s failed: %s", what, buf);
}

/* Convenience: run an expression returning an int, and on nonzero rc
 * throw ISE and goto `label`. Doesn't check for pending exceptions; pair
 * with AXSN_BAIL_IF_PENDING where JNI was involved. */
#define AXSN_CHECK_RC(env, rc_expr, label, what)                \
    do {                                                         \
        int _rc_ = (rc_expr);                                    \
        if (_rc_ != 0) {                                         \
            AXSN_THROW_ISE(env, "%s failed (rc %d)", (what), _rc_); \
            goto label;                                          \
        }                                                        \
    } while (0)

#ifdef __cplusplus
}
#endif

#endif //AXSN_ERROR_H
