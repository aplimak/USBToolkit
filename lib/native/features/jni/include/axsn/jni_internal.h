//
// Created by aeliux on 11/25/26.
//

#pragma once

#include <axsn/export.h>
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Call all not started registered load hooks.
 * @param vm
 * @param reserved
 * @return 0 if succeeded, otherwise non-zero.
 * @note Automatically called by axsn_jni_load.
 */
AXSN_EXPORT int axsn_jni_call_load_hooks(const axsn_jni_env *jenv);

/**
 * Call runtime jni load handler, and increase lib counter.
 * @param vm
 * @param reserved
 * @return jni version if succeeded, otherwise JNI_ERR.
 * @note Automatically called by entry object library, if linked.
 */
AXSN_EXPORT jint axsn_jni_load(JavaVM *vm, void *reserved);

/**
 * Call runtime jni unload handler, and decrease lib counter.
 * @param vm
 * @param reserved
 * @note Automatically called by entry object library, if linked.
 */
AXSN_EXPORT void axsn_jni_unload(JavaVM *vm, void *reserved);

#ifdef __cplusplus
}
#endif
