//
// Created by aeliux on 11/10/26.
//

#include "ptrw.h"
#include "internal.h"
#include <jni.h>

JNIEXPORT void JNICALL
Java_ir_aeliux_ptrw_Native_setFreeCallback(JNIEnv *env,
                                           jclass clazz,
                                           jlong j_handle,
                                           jobject j_callback)
{
    ptrw_t *ptrw = ptrw_resolve(env, j_handle);
    if (!ptrw) return;

    jobject g_callback = (*env)->NewGlobalRef(env, j_callback);
    if (g_callback == NULL) {
        return;
    }

    jclass callbackClass = (*env)->GetObjectClass(env, g_callback);
    jmethodID mid = (*env)->GetMethodID(env, callbackClass, "run", "()V");
    (*env)->DeleteLocalRef(env, callbackClass);

    ptrw_set_free_callback(ptrw, g_callback, mid);
}

JNIEXPORT void JNICALL
Java_ir_aeliux_ptrw_Native_close(JNIEnv *env, jclass cls,
                                 jlong j_handle)
{
    ptrw_t *ptrw = ptrw_resolve(env, j_handle);
    if (!ptrw) return;

    ptrw_free(ptrw);
}
