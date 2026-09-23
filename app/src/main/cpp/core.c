//
// Created by aeliux on 9/23/26.
//

#include <jni.h>
#include "log.h"
#include "dto.h"

/* Cached VM for future use (attaching threads, calling back into Java,
 * etc.). NULL after JNI_OnUnload. */
static JavaVM *g_vm = NULL;

/* Returned to the VM. Android supports up to 1.6; this is the standard
 * value for a modern NDK library. */
#define USBTK_JNI_VERSION JNI_VERSION_1_6

/* Throw a RuntimeException with a descriptive message. Returns without
 * doing anything if it can't find the class (shouldn't happen — it's a
 * bootstrap class). */
static void usbtk_throw(JNIEnv *env, const char *msg)
{
    jclass cls = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (cls) {
        (*env)->ThrowNew(env, cls, msg);
        (*env)->DeleteLocalRef(env, cls);
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    (void)reserved;
    g_vm = vm;

    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **)&env, USBTK_JNI_VERSION) != JNI_OK) {
        /* No JNIEnv → no way to throw. Return JNI_ERR; the VM will
         * surface UnsatisfiedLinkError. */
        LOGE("JNI_OnLoad: GetEnv failed");
        return JNI_ERR;
    }

    if (usbtk_dto_init(env) != 0) {
        /* usbtk_dto_init already logs the specific failure. Roll back
         * anything it may have partially set, then bail. */
        usbtk_dto_release(env);
        usbtk_throw(env, "usbtoolkit: native init failed (dto)");
        return JNI_ERR;   /* still return ERR even with a pending exception */
    }

    LOGI("JNI_OnLoad: ok (v0x%x)", USBTK_JNI_VERSION);
    return USBTK_JNI_VERSION;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    (void)reserved;

    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **)&env, USBTK_JNI_VERSION) != JNI_OK) {
        /* Thread torn down / VM going away — best effort only. */
        LOGE("JNI_OnUnload: GetEnv failed, skipping cleanup");
        return;
    }

    usbtk_dto_release(env);
    g_vm = NULL;
    LOGI("JNI_OnUnload: ok");
}
