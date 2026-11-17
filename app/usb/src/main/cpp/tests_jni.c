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

JNIEXPORT jobject JNICALL
Java_ir_aeliux_usbtoolkit_usb_NativeTests_processUsbgGadgetStrs(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jobject j_gadgetStrs)
{
    struct usbg_gadget_strs g_strs = {0};
    if (usbtk_import_usbg_gadget_strs(env, j_gadgetStrs, &g_strs) != 0) return NULL;
    return usbtk_export_usbg_gadget_strs(env, &g_strs);
}

JNIEXPORT jobject JNICALL
Java_ir_aeliux_usbtoolkit_usb_NativeTests_processUsbgConfigAttrs(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jobject j_configAttrs)
{
    struct usbg_config_attrs c_attrs = {0};
    if (usbtk_import_usbg_config_attrs(env, j_configAttrs, &c_attrs) != 0) return NULL;
    return usbtk_export_usbg_config_attrs(env, &c_attrs);
}

JNIEXPORT jobject JNICALL
Java_ir_aeliux_usbtoolkit_usb_NativeTests_processUsbgConfigStrs(JNIEnv *env,
                                                                jclass clazz,
                                                                jobject j_configStrs)
{
    struct usbg_config_strs c_strs = {0};
    if (usbtk_import_usbg_config_strs(env, j_configStrs, &c_strs) != 0) return NULL;
    return usbtk_export_usbg_config_strs(env, &c_strs);
}
