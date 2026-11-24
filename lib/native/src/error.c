//
// Created by aeliux on 11/24/26.
//

#include "axsn/error.h"

void axsn_throw(JNIEnv *env, const char *cls, const char *fmt, ...)
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

int axsn_get_pending_exception(JNIEnv *env, char *buf, size_t buf_size)
{
    if (!(*env)->ExceptionCheck(env)) {
        return 0;
    }

    /* 1. Grab the throwable, then CLEAR before any other JNI call. */
    jthrowable ex = (*env)->ExceptionOccurred(env);
    (*env)->ExceptionClear(env);

    if (ex == NULL) {
        snprintf(buf, buf_size, "unknown exception");
        return 1;
    }

    /* 2. Class name. */
    jclass exClass = (*env)->GetObjectClass(env, ex);
    jclass classClass = (*env)->FindClass(env, "java/lang/Class");
    jmethodID getName = (*env)->GetMethodID(env, classClass,
                                            "getName", "()Ljava/lang/String;");
    jstring nameStr = (jstring) (*env)->CallObjectMethod(env, exClass, getName);
    const char *name = nameStr
                       ? (*env)->GetStringUTFChars(env, nameStr, NULL)
                       : NULL;

    /* 3. Message (may be NULL — Throwable.getMessage can return null). */
    jclass throwableClass = (*env)->FindClass(env, "java/lang/Throwable");
    jmethodID getMessage = (*env)->GetMethodID(env, throwableClass,
                                               "getMessage", "()Ljava/lang/String;");
    jstring msgStr = (jstring) (*env)->CallObjectMethod(env, ex, getMessage);
    const char *msg = msgStr
                      ? (*env)->GetStringUTFChars(env, msgStr, NULL)
                      : NULL;

    /* 4. Log it. */
    snprintf(buf, buf_size, "%s: %s",
             name ? name : "(unknown)",
             msg ? msg : "(no message)");

    /* 5. Cleanup (in reverse order; guard against NULLs). */
    if (msg) {
        (*env)->ReleaseStringUTFChars(env, msgStr, msg);
        (*env)->DeleteLocalRef(env, msgStr);
    }
    if (name) {
        (*env)->ReleaseStringUTFChars(env, nameStr, name);
        (*env)->DeleteLocalRef(env, nameStr);
    }
    (*env)->DeleteLocalRef(env, throwableClass);
    (*env)->DeleteLocalRef(env, classClass);
    (*env)->DeleteLocalRef(env, exClass);
    (*env)->DeleteLocalRef(env, ex);

    return 1;
}
