//
// Created by aeliux on 10/1/26.
//

#ifndef USBTOOLKIT_PTRW_H
#define USBTOOLKIT_PTRW_H

#include <pthread.h>
#include <jni.h>

typedef struct
{
    void *ptr;
    pthread_t owner;
} usbtk_ptrw_t;

/**
 * Resolves and validates a pointer not null and called from th owning thread.
 * @param env JNI env, used to queue a exception in case of any error.
 * @param ptr Pointer to the wrapped usbtk_ptrw_t.
 * @return The resolved usbtk_ptrw_t if it is valid and called from the owner thread, otherwise NULL.
 */
usbtk_ptrw_t *usbtk_ptrw_resolve(JNIEnv *env, long ptr);

/**
 * Wraps a pointer in a usbtk_ptrw_t linked to the current thread and returns the pointer to it.
 * @param env JNI env, used to queue a exception in case of any error.
 * @param ptr Pointer that is goin to be wraped.
 * @return The pointer to the wrapped strcut, or 0 if it failed.
 * @note Do Not use jni calls if the function returns 0, as it already queued a exception, only do cleanups.
 */
long usbtk_ptrw_wrap(JNIEnv *env, void *ptr);

#endif //USBTOOLKIT_PTRW_H
