//
// Created by aeliux on 11/11/26.
//

#ifndef PTRW_INTERNAL_H
#define PTRW_INTERNAL_H

#include <jni.h>
#include "ptrw.h"

#ifdef __cplusplus
extern "C" {
#endif

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
 * Register all manually declared native methods.
 * @param env JNIEnv to do registries.
 * @return JNI_OK if everything gone right, otherwise JNI_ERR.
 */
jint ptrw_register_native_methods(JNIEnv *env);

#ifdef __cplusplus
}
#endif

#endif //PTRW_INTERNAL_H
