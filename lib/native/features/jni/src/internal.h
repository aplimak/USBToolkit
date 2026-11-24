//
// Created by aeliux on 11/24/26.
//

#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include "axsn/jni.h"
#include <axsn/export.h>

/* Call all hooks */
AXSN_LOCAL int axsn_jni_call_hooks(JavaVM *vm, void *reserved, axsn_jni_hook_type type);

#ifdef __cplusplus
}
#endif
