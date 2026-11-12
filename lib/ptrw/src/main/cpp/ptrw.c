//
// Created by aeliux on 10/1/26.
//

#define LOG_TAG "libptrw:"__FILE_NAME__

#include "ptrw.h"
#include "internal.h"
#include <axsn/error.h>
#include <stdlib.h>

struct ptrw
{
    void *ptr;
    pthread_t owner;
    bool closed;
};

ptrw_t *ptrw_resolve(JNIEnv *env, long ptr)
{
    ptrw_t *ptrw = (ptrw_t *) (intptr_t) ptr;

    if (!ptrw) {
        AXSN_THROW_ISE(env, "PointerWrapper is already closed");
        return NULL;
    }
    if (!pthread_equal(ptrw->owner, pthread_self())) {
        AXSN_THROW_ISE(env,
                       "PointerWrapper used from a non-owner thread");
        return NULL;
    }
    return ptrw;
}

long ptrw_wrap(JNIEnv *env, void *ptr)
{
    ptrw_t *ptrw = malloc(sizeof(ptrw_t));
    if (!ptrw) {
        AXSN_THROW_OOM(env, "allocating ptrw_t");
        return 0;
    }
    ptrw->ptr = ptr;
    ptrw->owner = pthread_self();

    return (long) (intptr_t) ptrw;
}

void *ptrw_unwrap(JNIEnv *env, long ptr)
{
    ptrw_t *ptrw = ptrw_resolve(env, ptr);
    if (ptrw->closed) {
        AXSN_THROW_ISE(env, "PointerWrapper is already closed");
        return NULL;
    }
    if (ptrw == NULL) return NULL;
    return ptrw->ptr;
}

void ptrw_free(ptrw_t *ptrw)
{
    if (!ptrw) return;

    free(ptrw);
}
