//
// Created by aeliux on 11/25/26.
//

#pragma once

#include <axsn/export.h>
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Call all load hooks */
AXSN_EXPORT int axsn_jni_call_load_hooks(JavaVM *vm, void *reserved);

/* Automatically called by entry */
AXSN_EXPORT jint axsn_jni_load(JavaVM *vm, void *reserved);

/* Automatically called by entry */
AXSN_EXPORT void axsn_jni_unload(JavaVM *vm, void *reserved);

#ifdef __cplusplus
}
#endif
