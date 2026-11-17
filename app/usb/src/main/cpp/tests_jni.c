#include <jni.h>

//
// Created by aeliux on 11/17/26.
//

#define LOG_TAG "libusbtk:tests_jni.c"

#include "dto.h"
#include <usbg/usbg.h>

JNIEXPORT jobject JNICALL
Java_ir_aeliux_usbtoolkit_usb_NativeTests_processUsbgGadgetAttrs(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jobject j_gadgetAttrs)
{
    struct usbg_gadget_attrs g_attrs = {0};
    if (usbtk_import_usbg_gadget_attrs(env, j_gadgetAttrs, &g_attrs) != 0) return NULL;
    return usbtk_export_usbg_gadget_attrs(env, &g_attrs);
}
