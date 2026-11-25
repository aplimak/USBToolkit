//
// Created by aeliux on 11/24/26.
//

#pragma once

#include "axsn/jni.h"
#include <axsn/export.h>

AXSN_LOCAL void _axsn_jni_register_hook(axsn_jni_hook hook);
AXSN_LOCAL int _axsn_jni_call_hooks(const axsn_jni_env *jenv);
