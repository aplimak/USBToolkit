//
// Created by aeliux on 10/1/26.
//

#ifndef PTRW_PTRW_H
#define PTRW_PTRW_H

#ifdef __cplusplus
extern "C" {
#endif

#include <pthread.h>
#include <jni.h>
#include <stdbool.h>

#define PTRW_API __attribute__((visibility("default")))

typedef struct ptrw ptrw_t;

/**
 * Wraps a pointer in a ptrw_t linked to the current thread and returns the pointer to it.
 * @param env JNI env, used to queue a exception in case of any error.
 * @param ptr Pointer that is going to be wraped.
 * @return The pointer to the wrapped strcut, or 0 if it failed.
 * @note Do Not use jni calls if the function returns 0, as it already queued a exception, only do cleanups.
 */
PTRW_API long ptrw_wrap(JNIEnv *env, void *ptr);

/**
 * Resolves ptrw and validates called from th owning thread and not closed.
 * @param env JNI env, used to queue a exception in case of any error.
 * @param wptr Pointer to the ptrw_t.
 * @return The resolved ptrw_t if it is valid and called from the owner thread and not closed, otherwise NULL.
 * @note This function checks closed pointer wrapper.
 */
PTRW_API ptrw_t *ptrw_get(JNIEnv *env, long wptr);

/**
 * Unwraps a pointer, if the wrapper is not closed and called from the owner thread.
 * @param env JNI env, used to queue a exception in case of any error.
 * @param wptr Pointer to the ptrw_t.
 * @return unwrapped pointer, or NULL if there is any errors.
 * @note Do Not use jni calls if the function returns NULL, as it already queued a exception, only do cleanups.
 */
PTRW_API void *ptrw_unwrap(JNIEnv *env, long wptr);

/**
 * Unwraps a pointer and marks the wrapper as closed.
 * @param env JNI env, used to queue a exception in case of any error.
 * @param wptr Pointer to the ptrw_t.
 * @return unwrapped pointer, or NULL if there is any errors.
 * @note This won't free the pointer itself.
 */
PTRW_API void *ptrw_pop(JNIEnv *env, long wptr);

/**
 * Check if the calling thread is the wrapper's owner thread.
 * @param ptrw The pointer to the ptrw_t.
 * @return true if owning and false if not.
 */
PTRW_API bool ptrw_owner(ptrw_t *ptrw);

/**
 * Check if the wrapper is closed.
 * @param ptrw The pointer to the ptrw_t.
 * @return true if closed and false if not.
 */
PTRW_API bool ptrw_closed(ptrw_t *ptrw);

/**
 * Frees the memory used by the wrapper.
 * @param ptrw The pointer to the wrapper to be freed.
 * @note This won't free the pointer itself.
 */
PTRW_API void ptrw_free(ptrw_t *ptrw);

#ifdef __cplusplus
}
#endif

#endif //PTRW_PTRW_H
