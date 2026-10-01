//
// Created by aeliux on 10/1/26.
//

#include "ptrw.h"
#include "error.h"

usbtk_ptrw_t *usbtk_ptrw_resolve(JNIEnv *env, jlong ptr)
{
    usbtk_ptrw_t *ptrw = (usbtk_ptrw_t *) (intptr_t) ptr;

    if (!ptrw) {
        USBTK_THROW_ISE(env, "PointerWrapper is already closed");
        return NULL;
    }
    if (!pthread_equal(ptrw->owner, pthread_self())) {
        USBTK_THROW_ISE(env,
                        "PointerWrapper used from a non-owner thread");
        return NULL;
    }
    return ptrw;
}

long usbtk_ptrw_wrap(JNIEnv *env, void *ptr)
{
    usbtk_ptrw_t *ptrw = malloc(sizeof(usbtk_ptrw_t));
    if (!ptrw) {
        USBTK_THROW_OOM(env, "allocating usbtk_ptrw_t");
        return 0;
    }
    ptrw->ptr = ptr;
    ptrw->owner = pthread_self();

    return (long) (intptr_t) ptrw;
}
