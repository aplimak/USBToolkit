//
// Created by aeliux on 9/23/26.
//

#include <jni.h>
#include <usbg/usbg.h>
#include "dto.h"

JNIEXPORT jint JNICALL
Java_ir_aeliux_usbtoolkit_Native_usbCreateGadget(JNIEnv *env, jclass cls,
                                                jstring j_gadgetName,
                                                jobject j_gadgetAttrs,
                                                jobject j_gadgetStrs,
                                                jobject j_configAttrs,
                                                jstring j_configStr) {
    const char *gadget_name = (*env)->GetStringUTFChars(env, j_gadgetName, NULL);
    if (!gadget_name) return -1;

    int ret;

    usbg_state *s = NULL;
    usbg_gadget *g = NULL;
    usbg_config *c = NULL;

    struct usbg_gadget_attrs g_attrs;
    struct usbg_gadget_strs g_strs = {0};
    struct usbg_config_attrs c_attrs;
    struct usbg_config_strs c_strs = {0};

    ret = usbtk_usbg_gadget_attrs_from_kotlin(env, j_gadgetAttrs, &g_attrs);
    if (ret != 0) goto cleanup;

    ret = usbtk_usbg_gadget_strs_from_kotlin(env, j_gadgetStrs, &g_strs);
    if (ret != 0) goto cleanup;

    ret = usbtk_usbg_config_attrs_from_kotlin(env, j_configAttrs, &c_attrs);
    if (ret != 0) goto cleanup;

    ret = usbtk_usbg_config_strs_from_jstring(env, j_configStr, &c_strs);
    if (ret != 0) goto cleanup;

    ret = usbg_init("/config", &s);
    if (ret != 0) goto cleanup;

    ret = usbg_create_gadget(s, gadget_name, &g_attrs, &g_strs, &g);
    if (ret != 0) goto cleanup;

    ret = usbg_create_config(g, 1, "The only one", &c_attrs, &c_strs, &c);
    if (ret != 0) goto cleanup;

    ret = 0;

cleanup:
    if (gadget_name) {
        (*env)->ReleaseStringUTFChars(env, j_gadgetName, gadget_name);
    }

    if (s) {
        usbg_cleanup(s);
    }

    // They just always exist? the function just nullify them so it doesn't matter
    usbg_free_gadget_strs(&g_strs);
    usbg_free_config_strs(&c_strs);

    return ret;
}
