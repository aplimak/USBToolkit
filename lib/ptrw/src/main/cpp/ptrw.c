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

long ptrw_wrap(JNIEnv *env, void *ptr)
{
    if (ptr == NULL) {
        AXSN_THROW_IAE(env, "PointerWrapper can't wrap null pointers");
        return 0;
    }

    ptrw_t *ptrw = malloc(sizeof(ptrw_t));
    if (!ptrw) {
        AXSN_THROW_OOM(env, "allocating ptrw_t");
        return 0;
    }
    ptrw->ptr = ptr;
    ptrw->owner = pthread_self();

    return (long) (intptr_t) ptrw;
}

ptrw_t *ptrw_resolve(JNIEnv *env, long wptr)
{
    ptrw_t *ptrw = (ptrw_t *) (intptr_t) wptr;

    if (!ptrw) {
        AXSN_THROW_ISE(env, "PointerWrapper is already closed");
        return NULL;
    }
    if (!ptrw_owner(ptrw)) {
        AXSN_THROW_ISE(env,
                       "PointerWrapper used from a non-owner thread");
        return NULL;
    }
    return ptrw;
}

ptrw_t *ptrw_get(JNIEnv *env, long wptr)
{
    ptrw_t *ptrw = ptrw_resolve(env, wptr);
    if (ptrw_closed(ptrw)) {
        AXSN_THROW_ISE(env, "PointerWrapper is already closed");
        return NULL;
    }
    return ptrw;
}

void *ptrw_unwrap(JNIEnv *env, long wptr)
{
    ptrw_t *ptrw = ptrw_get(env, wptr);
    if (ptrw == NULL) return NULL;
    return ptrw->ptr;
}

void *ptrw_pop(JNIEnv *env, long wptr)
{
    ptrw_t *ptrw = ptrw_get(env, wptr);
    if (ptrw == NULL) return NULL;
    void *ptr = ptrw->ptr;
    ptrw->ptr = NULL;
    ptrw->closed = true;
    return ptr;
}

bool ptrw_owner(ptrw_t *ptrw)
{
    return pthread_equal(ptrw->owner, pthread_self());
}

bool ptrw_closed(ptrw_t *ptrw)
{
    return ptrw->closed;
}

void ptrw_free(ptrw_t *ptrw)
{
    if (!ptrw) return;

    free(ptrw);
}
