//
// Created by aeliux on 11/24/26.
//

#include "axsn/jni.h"

#include "shared.hpp"

int axsn_jni_call_load_hooks(JavaVM *vm, void *reserved) {
    return _axsn_jni_call_hooks(vm, reserved);
}

void axsn_jni_register_load_hook(axsn_jni_hook hook) {
    _axsn_jni_register_hook(hook);
}
