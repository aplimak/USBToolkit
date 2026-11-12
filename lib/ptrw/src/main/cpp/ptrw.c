//
// Created by aeliux on 10/1/26.
//

#define LOG_TAG "libptrw:"__FILE_NAME__

#include "ptrw.h"
#include "internal.h"
#include <axsn/error.h>
#include <axsn/log.h>
#include <stdlib.h>

struct ptrw_jni_call
{
    jobject obj;
    jmethodID method;
};

struct ptrw
{
    void *ptr;
    pthread_t owner;
    bool closed;
    struct ptrw_jni_call free_callback;
};

static jboolean jni_isOwnerThread(jlong wptr)
{
    ptrw_t *ptrw = ptrw_cast(wptr);
    if (ptrw == NULL) return true;
    return ptrw_owner(ptrw);
}

static JNINativeMethod methods[] = {
        {"isOwnerThread", "(J)Z", (void *) jni_isOwnerThread}
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

    LOGV("ptrw_wrap(%p) -> %p", ptr, (void *) ptrw);

    return (long) (intptr_t) ptrw;
}

ptrw_t *ptrw_resolve(JNIEnv *env, long wptr)
{
    ptrw_t *ptrw = ptrw_cast(wptr);

    if (!ptrw) {
        AXSN_THROW_ISE(env, "PointerWrapper is already closed");
        return NULL;
    }
    if (!ptrw_owner(ptrw)) {
        AXSN_THROW_ISE(env,
                       "PointerWrapper used from a non-owner thread");
        return NULL;
    }

    LOGD("ptrw_resolve %p from %p", ptrw->ptr, (void *) ptrw);

    return ptrw;
}

ptrw_t *ptrw_get(JNIEnv *env, long wptr)
{
    ptrw_t *ptrw = ptrw_resolve(env, wptr);
    if (ptrw_closed(ptrw)) {
        AXSN_THROW_ISE(env, "PointerWrapper is already closed");
        return NULL;
    }

    LOGD("ptrw_get %p from %p", ptrw->ptr, (void *) ptrw);

    return ptrw;
}

void *ptrw_unwrap(JNIEnv *env, long wptr)
{
    ptrw_t *ptrw = ptrw_get(env, wptr);
    if (ptrw == NULL) return NULL;

    LOGD("ptrw_unwrap %p from %p", ptrw->ptr, (void *) ptrw);

    return ptrw->ptr;
}

void *ptrw_pop(JNIEnv *env, long wptr)
{
    ptrw_t *ptrw = ptrw_get(env, wptr);
    if (ptrw == NULL) return NULL;
    void *ptr = ptrw->ptr;
    ptrw->ptr = NULL;
    ptrw->closed = true;

    LOGD("ptrw_pop %p from %p", ptr, (void *) ptrw);

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

void ptrw_set_free_callback(ptrw_t *ptrw, jobject obj, jmethodID method)
{
    ptrw->free_callback = (struct ptrw_jni_call) {
            .obj = obj,
            .method = method
    };

    LOGD("ptrw_set_free_callback(%p)", (void *) ptrw);
}

void ptrw_free(ptrw_t *ptrw)
{
    if (!ptrw) return;

    if (!ptrw_owner(ptrw)) {
        LOGW("ptrw_free(%p) called from a non-owner thread", (void *) ptrw);
        return;
    }

    JNIEnv *env = NULL;
    jboolean did_attach = JNI_FALSE;

    if (ptrw->free_callback.obj && ptrw->free_callback.method &&
        ptrw_aquire_env(&env, &did_attach) == JNI_OK) {
        (*env)->CallVoidMethod(env, ptrw->free_callback.obj, ptrw->free_callback.method);
        if ((*env)->ExceptionCheck(env)) {
            char error[256];
            axsn_get_pending_exception(env, error, sizeof(error));
            LOGE("error while calling free_callback of %p: %s", (void *) ptrw, error);
        }
        (*env)->DeleteGlobalRef(env, ptrw->free_callback.obj);
        if (did_attach && ptrw_detach_thread() != JNI_OK) {
            LOGE("error while detaching thread from JVM");
        }
        env = NULL;
        LOGD("ptrw_free(%p) call free_callback", (void *) ptrw);
    }

    free(ptrw);

    LOGV("ptrw_free(%p)", (void *) ptrw);
}

jint ptrw_register_native_methods(JNIEnv *env)
{
    jclass clazz = (*env)->FindClass(env, "ir/aeliux/ptrw/Native");
    if (clazz == NULL) {
        return JNI_ERR;
    }

    if ((*env)->RegisterNatives(env, clazz, methods, 1) < 0) {
        return JNI_ERR;
    }

    return JNI_OK;
}
