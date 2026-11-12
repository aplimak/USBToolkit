//
// Created by aeliux on 11/12/26.
//

#define LOG_TAG "libptrw:"__FILE_NAME__

#include "internal.h"

#include <jni.h>
#include <axsn/log.h>
#include <axsn/error.h>

/* Cached VM for future use (attaching threads, calling back into Java,
 * etc.). NULL after JNI_OnUnload. */
static JavaVM *g_vm = NULL;

/* Returned to the VM. Android supports up to 1.6; this is the standard
 * value for a modern NDK library. */
#define PTRW_JNI_VERSION JNI_VERSION_1_6

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    (void) reserved;
    g_vm = vm;

    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **) &env, PTRW_JNI_VERSION) != JNI_OK) {
        /* No JNIEnv → no way to throw. Return JNI_ERR; the VM will
         * surface UnsatisfiedLinkError. */
        LOGE("JNI_OnLoad: GetEnv failed");
        return JNI_ERR;
    }

    if (ptrw_register_native_methods(env) != JNI_OK) return JNI_ERR;

    LOGI("JNI_OnLoad");
    return PTRW_JNI_VERSION;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    (void) reserved;

    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **) &env, PTRW_JNI_VERSION) != JNI_OK) {
        /* Thread torn down / VM going away — best effort only. */
        LOGE("JNI_OnUnload: GetEnv failed, skipping cleanup");
        return;
    }

    g_vm = NULL;
    LOGI("JNI_OnUnload: ok");
}

jint ptrw_aquire_env(JNIEnv **env, jboolean *did_attach)
{
    *env = NULL;
    *did_attach = JNI_FALSE;

    JavaVM *jvm = g_vm;
    if (!jvm) return JNI_ERR;

    jint result = (*jvm)->GetEnv(jvm, (void **) env, PTRW_JNI_VERSION);
    if (result == JNI_EDETACHED) {
        // Attach as a daemon so this thread doesn't block VM shutdown
        result = (*jvm)->AttachCurrentThreadAsDaemon(jvm, env, NULL);
        if (result == JNI_OK) {
            *did_attach = JNI_TRUE;
        }
    }
    return result;
}

jint ptrw_detach_thread(void)
{
    if (g_vm == NULL) return JNI_ERR;
    return (*g_vm)->DetachCurrentThread(g_vm);
}
