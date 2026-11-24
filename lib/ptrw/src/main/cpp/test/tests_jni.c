//
// Created by aeliux on 11/17/26.
//

#include "ptrw.h"
#include "../internal.h"
#include "axsn/error.h"
#include <stdlib.h>

struct dummy_data {
    int num;
    long num2;
};

JNIEXPORT void JNICALL
Java_ir_aeliux_ptrw_NativeTests_pop(JNIEnv *env, jclass clazz, jlong ptr)
{
    (void)clazz;
    ptrw_pop(env, ptr);
}

JNIEXPORT jlong JNICALL
Java_ir_aeliux_ptrw_DummyPointerWrapper_init(JNIEnv *env, jclass clazz)
{
    (void)clazz;
    struct dummy_data *ptr = calloc(1, sizeof(struct dummy_data));
    if (!ptr) {
        AXSN_THROW_OOM(env, "dummy_data");
        return 0;
    }

    return ptrw_wrap(env, ptr);
}

JNIEXPORT void JNICALL
Java_ir_aeliux_ptrw_DummyPointerWrapper_free(JNIEnv *env, jclass clazz, jlong wptr)
{
    (void)clazz;
    struct dummy_data *ptr = ptrw_pop(env, wptr);
    if (!ptr) return;

    free(ptr);
}
