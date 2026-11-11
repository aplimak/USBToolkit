//
// Created by aeliux on 11/10/26.
//

#include "ptrw.h"

#include <jni.h>

JNIEXPORT void JNICALL
Java_ir_aeliux_ptrw_Native_close(JNIEnv *env, jclass cls,
                                 jlong handle)
{
    usbtk_ptrw_t *ptrw = usbtk_ptrw_resolve(env, handle);
    if (!ptrw) return;

    usbtk_ptrw_free(ptrw);
}
