#include <jni.h>

//
// Created by aeliux on 11/17/26.
//

#define LOG_TAG "libusbtktests:tests_jni.c"

#include <usbtk/dto.h>
#include <usbg/usbg.h>
#include <usbg/function/ms.h>

JNIEXPORT jobject JNICALL
Java_ir_aeliux_usbtoolkit_usb_NativeTests_processUsbgGadgetAttrs(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jobject j_gadgetAttrs)
{
    (void)clazz;
    struct usbg_gadget_attrs g_attrs = {0};
    if (usbtk_import_usbg_gadget_attrs(env, j_gadgetAttrs, &g_attrs) != 0) return NULL;
    return usbtk_export_usbg_gadget_attrs(env, &g_attrs);
}

JNIEXPORT jobject JNICALL
Java_ir_aeliux_usbtoolkit_usb_NativeTests_processUsbgGadgetStrs(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jobject j_gadgetStrs)
{
    (void)clazz;
    struct usbg_gadget_strs g_strs = {0};
    if (usbtk_import_usbg_gadget_strs(env, j_gadgetStrs, &g_strs) != 0) return NULL;
    jobject out = usbtk_export_usbg_gadget_strs(env, &g_strs);
    usbtk_free_usbg_gadget_strs(&g_strs);
    return out;
}

JNIEXPORT jobject JNICALL
Java_ir_aeliux_usbtoolkit_usb_NativeTests_processUsbgConfigAttrs(JNIEnv *env,
                                                                 jclass clazz,
                                                                 jobject j_configAttrs)
{
    (void)clazz;
    struct usbg_config_attrs c_attrs = {0};
    if (usbtk_import_usbg_config_attrs(env, j_configAttrs, &c_attrs) != 0) return NULL;
    return usbtk_export_usbg_config_attrs(env, &c_attrs);
}

JNIEXPORT jobject JNICALL
Java_ir_aeliux_usbtoolkit_usb_NativeTests_processUsbgConfigStrs(JNIEnv *env,
                                                                jclass clazz,
                                                                jobject j_configStrs)
{
    (void)clazz;
    struct usbg_config_strs c_strs = {0};
    if (usbtk_import_usbg_config_strs(env, j_configStrs, &c_strs) != 0) return NULL;
    jobject out = usbtk_export_usbg_config_strs(env, &c_strs);
    usbtk_free_usbg_config_strs(&c_strs);
    return out;
}

JNIEXPORT jobject JNICALL
Java_ir_aeliux_usbtoolkit_usb_NativeTests_processUsbgMassStorageAttrs(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jobject j_massStorageAttrs)
{
    (void)clazz;
    struct usbg_f_ms_attrs ms_attrs = {0};
    if (usbtk_import_usbg_f_mass_storage_attrs(env, j_massStorageAttrs, &ms_attrs) != 0) return NULL;
    jobject out = usbtk_export_usbg_f_mass_storage_attrs(env, &ms_attrs);
    usbtk_free_usbg_f_mass_storage_attrs(&ms_attrs);
    return out;
}
