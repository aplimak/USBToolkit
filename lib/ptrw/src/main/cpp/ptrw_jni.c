//
// Created by aeliux on 11/10/26.
//

#include "ptrw.h"
#include "internal.h"
#include <jni.h>

JNIEXPORT void JNICALL
Java_ir_aeliux_ptrw_Native_close(JNIEnv *env, jclass cls,
                                 jlong handle)
{
    ptrw_t *ptrw = ptrw_resolve(env, handle);
    if (!ptrw) return;

    ptrw_free(ptrw);
}
