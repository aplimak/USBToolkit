//
// Created by aeliux on 11/24/26.
//

#include "axsn/jni.h"
#include "axsn/jni_internal.h"
#include "shared.hpp"

int axsn_jni_call_load_hooks(const axsn_jni_env *jenv) {
    return _axsn_jni_call_hooks(jenv);
}

void axsn_jni_register_load_hook(axsn_jni_hook hook) {
    _axsn_jni_register_hook(hook);
}
