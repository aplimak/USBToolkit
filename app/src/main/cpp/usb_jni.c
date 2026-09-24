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

#define USBTK_DEFAULT_CONFIGFS_PATH "/config"

static char *configfs_path = NULL;
static pthread_mutex_t configfs_path_lock = PTHREAD_MUTEX_INITIALIZER;

static void usbtk_configfs_path_get(char *out, size_t out_sz)
{
    pthread_mutex_lock(&configfs_path_lock);
    const char *src = configfs_path ? configfs_path : USBTK_DEFAULT_CONFIGFS_PATH;
    snprintf(out, out_sz, "%s", src);
    pthread_mutex_unlock(&configfs_path_lock);
}

JNIEXPORT void JNICALL
Java_ir_aeliux_usbtoolkit_Native_usbtkUsbCreateGadget(JNIEnv *env, jclass cls,
                                                jstring j_gadgetName,
                                                jobject j_gadgetAttrs,
                                                jobject j_gadgetStrs,
                                                jobject j_configAttrs,
                                                jstring j_configStr) {
    usbg_state *s = NULL;
    usbg_gadget *g = NULL;
    usbg_config *c = NULL;

    struct usbg_gadget_attrs g_attrs;
    struct usbg_gadget_strs g_strs = {0};
    struct usbg_config_attrs c_attrs;
    struct usbg_config_strs c_strs = {0};

    LOGI("Creating Gadget");
    if (usbtk_throw_if_null(env, j_gadgetName, "gadgetName") != 0) return;
    const char *gadget_name = (*env)->GetStringUTFChars(env, j_gadgetName, NULL);
    if (!gadget_name) return;
    LOGV("Gadget name is ok");

    if (usbtk_usbg_gadget_attrs_from_kotlin(env, j_gadgetAttrs, &g_attrs) != 0) goto cleanup;
    if (usbtk_usbg_gadget_strs_from_kotlin(env, j_gadgetStrs, &g_strs) != 0) goto cleanup;
    if (usbtk_usbg_config_attrs_from_kotlin(env, j_configAttrs, &c_attrs) != 0) goto cleanup;
    if (usbtk_usbg_config_strs_from_jstring(env, j_configStr, &c_strs) != 0) goto cleanup;

    {
        char path[PATH_MAX];
        usbtk_configfs_path_get(path, sizeof path);
        int rc = usbg_init(path, &s);
        if (rc != USBG_SUCCESS) {
            usbtk_throw_usbg(env, "usbg_init", rc);
            goto cleanup;
        }
        LOGD("usbg init done");
    }

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
    if (s) usbg_cleanup(s);
    if (gadget_name) (*env)->ReleaseStringUTFChars(env, j_gadgetName, gadget_name);
    usbg_free_gadget_strs(&g_strs);
    usbg_free_config_strs(&c_strs);
}

JNIEXPORT void JNICALL
Java_ir_aeliux_usbtoolkit_Native_usbtkUsbSetConfigfsPath(JNIEnv *env, jclass cls,
                                                        jstring j_configfsPath) {
    LOGI("Updating ConfigFS Path");
    if (usbtk_throw_if_null(env, j_configfsPath, "configfsPath") != 0) return;
    const char *p_configfs_path = (*env)->GetStringUTFChars(env, j_configfsPath, NULL);
    if (!p_configfs_path) return;

    char *dup = strdup(p_configfs_path);
    if (dup == NULL) {
        usbtk_throw_errno(env, "clone configfsPath");
        goto cleanup;
    }
    LOGV("Duplicated the configfs path");

    pthread_mutex_lock(&configfs_path_lock);
    char *old = configfs_path;
    configfs_path = dup;
    pthread_mutex_unlock(&configfs_path_lock);

    free(old);

cleanup:
    (*env)->ReleaseStringUTFChars(env, j_configfsPath, p_configfs_path);
}
