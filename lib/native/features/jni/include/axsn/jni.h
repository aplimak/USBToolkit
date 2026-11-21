#pragma once

/*
 * C-compatible JNI runtime access for one consuming ELF shared library.
 *
 * These declarations intentionally have hidden visibility. They are resolved
 * among translation units in the final consuming DSO, but are not added to
 * its public dynamic symbol table. JNI_OnLoad/JNI_OnUnload are the only
 * symbols implemented by AXSN that are intentionally exported.
 */
#include <jni.h>
#include <axsn/export.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct axsn_jni_env {
    JavaVM *vm;
    JNIEnv *env;
    int attached_by_axsn;
} axsn_jni_env;

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
