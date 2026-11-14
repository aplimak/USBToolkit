//
// Created by aeliux on 9/23/26.
//

#define LOG_TAG "libusbtk:"__FILE_NAME__

#include "dto.h"

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <string.h>
#include <pthread.h>
#include <usbg/function/ms.h>
#include <usbg/usbg.h>
#include <axsn/log.h>
#include <axsn/error.h>
#include <ptrw.h>

/* Throw IOException for a failed libusbgx call. */
static inline void
usbtk_throw_usbg(JNIEnv *env, const char *what, int rc)
{
    AXSN_THROW_IOE(env,
                   "%s failed: error %s: %s (%d)",
                   what,
                   usbg_error_name(rc),
                   usbg_strerror(rc),
                   rc);
}

JNIEXPORT jlong JNICALL
Java_ir_aeliux_usbtoolkit_usb_Native_init(JNIEnv *env, jclass cls,
                                          jstring j_configfsPath)
{
    if (axsn_throw_if_null(env, j_configfsPath, "configfsPath") != 0) {
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

    LOGV("usbg_init -> %p", (void *) s);

    long ptrw = ptrw_wrap(env, s);
    if (!ptrw) {
        usbg_cleanup(s);
        return 0;
    }

    return ptrw;
}

JNIEXPORT void JNICALL
Java_ir_aeliux_usbtoolkit_usb_Native_close(JNIEnv *env, jclass cls,
                                           jlong handle)
{
    usbg_state *s = ptrw_pop(env, handle);
    if (s == NULL) return;

    LOGV("usbg_cleanup(%p)", (void *) s);
    usbg_cleanup(s);
}

JNIEXPORT jlong JNICALL
Java_ir_aeliux_usbtoolkit_usb_Native_createGadget(JNIEnv *env, jclass cls,
                                                  jlong j_ptrwConfigfs,
                                                  jstring j_gadgetName,
                                                  jobject j_gadgetAttrs,
                                                  jobject j_gadgetStrs,
                                                  jobject j_configAttrs,
                                                  jstring j_configStr)
{
    usbg_state *s = ptrw_unwrap(env, j_ptrwConfigfs);
    if (s == NULL) return 0;

    usbg_gadget *g = NULL;
    usbg_config *c = NULL;

    struct usbg_gadget_attrs g_attrs = {0};
    struct usbg_gadget_strs g_strs = {0};
    struct usbg_config_attrs c_attrs = {0};
    struct usbg_config_strs c_strs = {0};

    if (axsn_throw_if_null(env, j_gadgetName, "gadgetName") != 0) return 0;
    const char *gadget_name = (*env)->GetStringUTFChars(env, j_gadgetName, NULL);
    if (!gadget_name) return 0;

    if (usbtk_import_usbg_gadget_attrs(env, j_gadgetAttrs, &g_attrs) != 0) goto cleanup;
    if (usbtk_import_usbg_gadget_strs(env, j_gadgetStrs, &g_strs) != 0) goto cleanup;
    if (usbtk_import_usbg_config_attrs(env, j_configAttrs, &c_attrs) != 0) goto cleanup;
    if (usbtk_import_usbg_config_strs(env, j_configStr, &c_strs) != 0) goto cleanup;

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

    // TODO: There must be dto functions to free them, but usbg also calls free, so ok for now, but technically wrong.
    usbg_free_gadget_strs(&g_strs);
    usbg_free_config_strs(&c_strs);

    if (g) {
        return ptrw_wrap(env, g);
    }

    return 0;
}

JNIEXPORT jlong JNICALL
Java_ir_aeliux_usbtoolkit_usb_Native_openGadget(JNIEnv *env, jclass cls,
                                                jlong j_ptrwConfigfs,
                                                jstring j_gadgetName)
{
    usbg_state *s = ptrw_unwrap(env, j_ptrwConfigfs);
    if (s == NULL) return 0;

    if (axsn_throw_if_null(env, j_gadgetName, "gadgetName") != 0) return 0;
    const char *gadget_name = (*env)->GetStringUTFChars(env, j_gadgetName, NULL);
    if (!gadget_name) return 0;

    usbg_gadget *g = usbg_get_gadget(s, gadget_name);

    (*env)->ReleaseStringUTFChars(env, j_gadgetName, gadget_name);

    if (g == NULL) {
        AXSN_THROW_IAE(env, "gadget not found: %s", gadget_name);
        return 0;
    }

    return ptrw_wrap(env, g);
}

JNIEXPORT void JNICALL
Java_ir_aeliux_usbtoolkit_usb_Native_deleteGadget(JNIEnv *env, jclass cls,
                                                  jlong j_ptrwGadget)
{
    usbg_gadget *g = ptrw_pop(env, j_ptrwGadget);
    if (g == NULL) return;

    int rc = usbg_rm_gadget(g, USBG_RM_RECURSE);
    if (rc != 0) {
        usbtk_throw_usbg(env, "delete gadget", rc);
        return;
    }
}

JNIEXPORT jlong JNICALL
Java_ir_aeliux_usbtoolkit_usb_Native_createFunction(JNIEnv *env,
                                                    jclass cls,
                                                    jlong j_ptrwGadget,
                                                    jint j_functionType,
                                                    jstring j_functionInstance,
                                                    jobject j_functionAttrs)
{
    usbg_gadget *g = ptrw_unwrap(env, j_ptrwGadget);
    if (g == NULL) return 0;

    const char *instance = (*env)->GetStringUTFChars(env, j_functionInstance, NULL);
    void *attrs = NULL;
    usbg_function *f = NULL;

    switch (j_functionType) {
        case USBG_F_MASS_STORAGE:
            attrs = malloc(sizeof(struct usbg_f_ms_attrs));
            usbtk_import_usbg_f_mass_storage_attrs(env, j_functionAttrs, attrs);
            break;
        default:
            AXSN_THROW_IAE(env,
                           "usbtkUsbCreateFunction: Function type %d is currently not supported",
                           j_functionType);
            goto clean;
    }

    int rc = usbg_create_function(g, j_functionType, instance, attrs, &f);
    if (rc != 0) {
        usbtk_throw_usbg(env, "create function", rc);
        goto clean;
    }

    clean:
    switch (j_functionType) {
        case USBG_F_MASS_STORAGE:
            usbtk_free_usbg_f_mass_storage_attrs(attrs);
            break;
        default:
            break;
    }

    free(attrs);
    if (instance) (*env)->ReleaseStringUTFChars(env, j_functionInstance, instance);
    if (f) {
        return ptrw_wrap(env, f);
    }
    return 0;
}
