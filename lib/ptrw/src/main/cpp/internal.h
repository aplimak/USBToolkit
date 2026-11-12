//
// Created by aeliux on 11/11/26.
//

#ifndef PTRW_INTERNAL_H
#define PTRW_INTERNAL_H

#include <jni.h>
#include "ptrw.h"

/**
 * Cast long to ptrw_t.
 * @param wptr Pointer to ptrw_t.
 * @return Casted ptrw_t.
 * @note This function does not check anything, its just a convenient cast.
 */
static inline ptrw_t *ptrw_cast(long wptr)
{
    return (ptrw_t *) (intptr_t) wptr;
}

/**
 * Resolves ptrw and validates called from the owning thread.
 * @param env JNI env, used to queue a exception in case of any error.
 * @param wptr Pointer to the ptrw_t.
 * @return The resolved ptrw_t if it is valid and called from the owner thread, otherwise NULL.
 * @note This function does not check closed pointer wrapper.
 */
ptrw_t *ptrw_resolve(JNIEnv *env, long wptr);

/**
 * Try to get current thread's JNNIEnv or attach current thread to JVM.
 * @param env out JNIEnv.
 * @param did_attach out JNI_TRUE if attached and JNI_FALSE if already attached.
 * @return JNI_OK if everything gone right, otherwise, the error code reurned by vm call.
 * @note Thread must be detached on when did_attach.
 */
jint ptrw_aquire_env(JNIEnv **env, jboolean *did_attach);

/**
 * Detach current thread from JVM.
 * @return JNI_OK if everything gone right, otherwise, the error code reurned by vm call.
 */
jint ptrw_detach_thread(void);

/**
 * Register all manually declared native methods.
 * @param env JNIEnv to do registries.
 * @return JNI_OK if everything gone right, otherwise JNI_ERR.
 */
jint ptrw_register_native_methods(JNIEnv *env);

#endif //PTRW_INTERNAL_H
