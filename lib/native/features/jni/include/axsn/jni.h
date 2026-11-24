#pragma once

#include <jni.h>
#include <axsn/export.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    JavaVM *vm;
    JNIEnv *env;
    int attached_by_axsn;
} axsn_jni_env;

/**
 * JNI Hook function signature.
 * @note return non-zero to fail the load phase, ignored in unload phase.
 * @note exceptions will be treated as fail.
 */
typedef int (*axsn_jni_hook)(JavaVM *vm, void *reserved);

/**
 * Type that indicates when the hook is called.
 */
typedef enum {
    AXSN_JNI_LOAD,
    AXSN_JNI_UNLOAD,
} axsn_jni_hook_type;

/**
 * Register a function to be called on jni load or unload phase.
 * @param hook Pointer to the hood function.
 * @param type Type of the hook, either load or unload.
 * @note Hook is able to fail the load phase by throwing exception or returning non zero value.
 */
AXSN_LOCAL void axsn_jni_register_hook(axsn_jni_hook hook, axsn_jni_hook_type type);

/* Returns the VM captured by this DSO's JNI_OnLoad, or NULL before load. */
AXSN_LOCAL JavaVM *axsn_jni_get_vm(void);

/* Returns this thread's JNIEnv only when it is already attached. */
AXSN_LOCAL JNIEnv *axsn_jni_get_env(void);

/*
 * Obtains the current thread's JNIEnv, attaching it if necessary.
 * On success, out_env->attached_by_axsn records whether release must detach.
 * Pass NULL as thread_name to use the VM's default thread name.
 */
AXSN_LOCAL jint axsn_jni_env_acquire(
    axsn_jni_env *out_env,
    const char *thread_name
);

/* Detaches only when axsn_jni_env_acquire() attached the current thread. */
AXSN_LOCAL void axsn_jni_env_release(axsn_jni_env *env);

#ifdef __cplusplus
} /* extern "C" */
#endif
