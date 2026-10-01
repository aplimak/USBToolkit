//
// Created by aeliux on 9/23/26.
//

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <string.h>
#include <pthread.h>
#include <usbg/usbg.h>
#include "dto.h"
#include "log.h"
#include "error.h"
#include "ptrw.h"

JNIEXPORT jlong JNICALL
Java_ir_aeliux_usbtoolkit_Native_usbtkUsbInit(JNIEnv *env, jclass cls,
                                              jstring j_configfsPath)
{
    if (usbtk_throw_if_null(env, j_configfsPath, "configfsPath") != 0) {
        return 0;
    }

    const char *path = (*env)->GetStringUTFChars(env, j_configfsPath, NULL);
    if (!path) return 0;   /* OOM already pending */

    usbg_state *s = NULL;
    int rc = usbg_init(path, &s);
    (*env)->ReleaseStringUTFChars(env, j_configfsPath, path);

    if (rc != USBG_SUCCESS) {
        usbtk_throw_usbg(env, "usbg_init", rc);
        return 0;
    }

    long ptrw = usbtk_ptrw_wrap(env, s);
    if (!ptrw) {
        usbg_cleanup(s);
        return 0;
    }

    LOGD("usbg_init ok (handle %p)", (void *) ptrw);
    return ptrw;
}

JNIEXPORT void JNICALL
Java_ir_aeliux_usbtoolkit_Native_usbtkUsbClose(JNIEnv *env, jclass cls,
                                               jlong handle)
{
    usbtk_ptrw_t *ptrw = usbtk_ptrw_resolve(env, handle);
    if (!ptrw) return;

    LOGD("usbg_cleanup (handle %p)", (void *) ptrw);
    usbg_cleanup(ptrw->ptr);
    free(ptrw);
}

JNIEXPORT jlong JNICALL
Java_ir_aeliux_usbtoolkit_Native_usbtkUsbCreateGadget(JNIEnv *env, jclass cls,
                                                      jlong j_ptrwConfigfs,
                                                      jstring j_gadgetName,
                                                      jobject j_gadgetAttrs,
                                                      jobject j_gadgetStrs,
                                                      jobject j_configAttrs,
                                                      jstring j_configStr)
{
    usbtk_ptrw_t *st = usbtk_ptrw_resolve(env, j_ptrwConfigfs);
    if (!st) return 0;
    usbg_state *s = st->ptr;

    usbg_gadget *g = NULL;
    usbg_config *c = NULL;

    struct usbg_gadget_attrs g_attrs;
    struct usbg_gadget_strs g_strs = {0};
    struct usbg_config_attrs c_attrs;
    struct usbg_config_strs c_strs = {0};

    LOGI("Creating Gadget");
    if (usbtk_throw_if_null(env, j_gadgetName, "gadgetName") != 0) return 0;
    const char *gadget_name = (*env)->GetStringUTFChars(env, j_gadgetName, NULL);
    if (!gadget_name) return 0;
    LOGV("Gadget name is ok");

    if (usbtk_usbg_gadget_attrs_from_kotlin(env, j_gadgetAttrs, &g_attrs) != 0) goto cleanup;
    if (usbtk_usbg_gadget_strs_from_kotlin(env, j_gadgetStrs, &g_strs) != 0) goto cleanup;
    if (usbtk_usbg_config_attrs_from_kotlin(env, j_configAttrs, &c_attrs) != 0) goto cleanup;
    if (usbtk_usbg_config_strs_from_jstring(env, j_configStr, &c_strs) != 0) goto cleanup;

    {
        int rc = usbg_create_gadget(s, gadget_name, &g_attrs, &g_strs, &g);
        if (rc != USBG_SUCCESS) {
            usbtk_throw_usbg(env, "usbg_create_gadget", rc);
            goto cleanup;
        }
    }

    {
        int rc = usbg_create_config(g, 1, "c", &c_attrs, &c_strs, &c);
        if (rc != USBG_SUCCESS) {
            usbtk_throw_usbg(env, "usbg_create_config", rc);
            goto cleanup;
        }
    }

    cleanup:
    (*env)->ReleaseStringUTFChars(env, j_gadgetName, gadget_name);
    usbg_free_gadget_strs(&g_strs);
    usbg_free_config_strs(&c_strs);

    if (g) {
        return usbtk_ptrw_wrap(env, g);
    }

    return 0;
}

JNIEXPORT jlong JNICALL
Java_ir_aeliux_usbtoolkit_Native_usbtkUsbOpenGadget(JNIEnv *env, jclass cls,
                                                    jlong j_ptrwConfigfs,
                                                    jstring j_gadgetName)
{
    usbtk_ptrw_t *st = usbtk_ptrw_resolve(env, j_ptrwConfigfs);
    if (!st) return 0;
    usbg_state *s = st->ptr;

    if (usbtk_throw_if_null(env, j_gadgetName, "gadgetName") != 0) return 0;
    const char *gadget_name = (*env)->GetStringUTFChars(env, j_gadgetName, NULL);
    if (!gadget_name) return 0;
    LOGV("Gadget name is ok");

    usbg_gadget *g = NULL:

    {
        g = usbg_get_gadget(s, gadget_name);
        if (g == NULL) {
            usbtk_throw(env, USBTK_JC_IAE, "gadget not found: %s", gadget_name);
            goto cleanup;
        }
    }

    cleanup:
    (*env)->ReleaseStringUTFChars(env, j_gadgetName, gadget_name);

    if (g) {
        return usbtk_ptrw_wrap(env, g);
    }

    return 0;
}
