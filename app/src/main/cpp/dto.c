//
// Created by aeliux on 9/22/26.
//

#include "dto.h"

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <usbg/usbg.h>

#define KOTLIN_USBG_GADGET_ATTRS_CLASS "ir/aeliux/usbtoolkit/dto/UsbgGadgetAttrs"
#define KOTLIN_USBG_GADGET_ATTRS_CTOR_SIG "(SBBBBSSS)V"

#define KOTLIN_USBG_GADGET_STRS_CLASS "ir/aeliux/usbtoolkit/dto/UsbgGadgetStrs"
#define KOTLIN_USBG_GADGET_STRS_CTOR_SIG "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"

#define KOTLIN_USBG_CONFIG_ATTRS_CLASS "ir/aeliux/usbtoolkit/dto/UsbgConfigAttrs"
#define KOTLIN_USBG_CONFIG_ATTRS_CTOR_SIG "(BB)V"

typedef struct {
    jclass    cls;
    jmethodID ctor;
    jfieldID  f_bcdUSB;
    jfieldID  f_bDeviceClass;
    jfieldID  f_bDeviceSubClass;
    jfieldID  f_bDeviceProtocol;
    jfieldID  f_bMaxPacketSize0;
    jfieldID  f_idVendor;
    jfieldID  f_idProduct;
    jfieldID  f_bcdDevice;
} usbtk_usbg_gadget_attrs_jni_t;

typedef struct {
    jclass    cls;
    jmethodID ctor;
    jfieldID  f_manufacturer;
    jfieldID  f_product;
    jfieldID  f_serial;
} usbtk_usbg_gadget_strs_jni_t;

typedef struct {
    jclass    cls;
    jmethodID ctor;
    jfieldID  f_bmAttributes;
    jfieldID  f_bMaxPower;
} usbtk_usbg_config_attrs_jni_t;

static struct {
    usbtk_usbg_gadget_attrs_jni_t gadget_attrs;
    usbtk_usbg_gadget_strs_jni_t gadget_strs;
    usbtk_usbg_config_attrs_jni_t config_attrs;
} g_jni = {0};

/* Reads obj.<field> as a String, returns a freshly malloc'd UTF-8 copy,
 * or NULL if the field is null. Caller frees with free(). */
static char *usbtk_jstring_dup(JNIEnv *env, jobject obj, jfieldID fid)
{
    jstring js = (jstring)(*env)->GetObjectField(env, obj, fid);
    if (!js) return NULL;

    const char *utf = (*env)->GetStringUTFChars(env, js, NULL);
    if (!utf) {
        (*env)->DeleteLocalRef(env, js);
        return NULL;
    }

    char *dup = strdup(utf);

    (*env)->ReleaseStringUTFChars(env, js, utf);
    (*env)->DeleteLocalRef(env, js);
    return dup;
}

static void usbtk_usbg_gadget_attrs_jni_release(JNIEnv *env)
{
    if (g_jni.gadget_attrs.cls) {
        (*env)->DeleteGlobalRef(env, g_jni.gadget_attrs.cls);
        g_jni.gadget_attrs.cls = NULL;
    }
}

static int usbtk_usbg_gadget_attrs_jni_init(JNIEnv *env)
{
    if (g_jni.gadget_attrs.cls) return 0;

    jclass local = (*env)->FindClass(env, KOTLIN_USBG_GADGET_ATTRS_CLASS);
    if (!local) return -1;

    g_jni.gadget_attrs.cls = (jclass)(*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (!g_jni.gadget_attrs.cls) return -1;

    g_jni.gadget_attrs.ctor = (*env)->GetMethodID(env, g_jni.gadget_attrs.cls, "<init>",
                                                  KOTLIN_USBG_GADGET_ATTRS_CTOR_SIG);
    if (!g_jni.gadget_attrs.ctor) goto fail;

    g_jni.gadget_attrs.f_bcdUSB          = (*env)->GetFieldID(env, g_jni.gadget_attrs.cls, "bcdUSB",          "S");
    g_jni.gadget_attrs.f_bDeviceClass    = (*env)->GetFieldID(env, g_jni.gadget_attrs.cls, "bDeviceClass",    "B");
    g_jni.gadget_attrs.f_bDeviceSubClass = (*env)->GetFieldID(env, g_jni.gadget_attrs.cls, "bDeviceSubClass", "B");
    g_jni.gadget_attrs.f_bDeviceProtocol = (*env)->GetFieldID(env, g_jni.gadget_attrs.cls, "bDeviceProtocol", "B");
    g_jni.gadget_attrs.f_bMaxPacketSize0 = (*env)->GetFieldID(env, g_jni.gadget_attrs.cls, "bMaxPacketSize0", "B");
    g_jni.gadget_attrs.f_idVendor        = (*env)->GetFieldID(env, g_jni.gadget_attrs.cls, "idVendor",        "S");
    g_jni.gadget_attrs.f_idProduct       = (*env)->GetFieldID(env, g_jni.gadget_attrs.cls, "idProduct",       "S");
    g_jni.gadget_attrs.f_bcdDevice       = (*env)->GetFieldID(env, g_jni.gadget_attrs.cls, "bcdDevice",       "S");

    if (!g_jni.gadget_attrs.f_bcdUSB
        || !g_jni.gadget_attrs.f_bDeviceClass
        || !g_jni.gadget_attrs.f_bDeviceSubClass
        || !g_jni.gadget_attrs.f_bDeviceProtocol
        || !g_jni.gadget_attrs.f_bMaxPacketSize0
        || !g_jni.gadget_attrs.f_idVendor
        || !g_jni.gadget_attrs.f_idProduct
        || !g_jni.gadget_attrs.f_bcdDevice) {
        goto fail;
    }
    return 0;

    fail:
    usbtk_usbg_gadget_attrs_jni_release(env);
    return -1;
}

static void usbtk_usbg_gadget_strs_jni_release(JNIEnv *env)
{
    if (g_jni.gadget_strs.cls) {
        (*env)->DeleteGlobalRef(env, g_jni.gadget_strs.cls);
        g_jni.gadget_strs.cls = NULL;
    }
}

static int usbtk_usbg_gadget_strs_jni_init(JNIEnv *env)
{
    if (g_jni.gadget_strs.cls) return 0;

    jclass local = (*env)->FindClass(env, KOTLIN_USBG_GADGET_STRS_CLASS);
    if (!local) return -1;

    g_jni.gadget_strs.cls = (jclass)(*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (!g_jni.gadget_strs.cls) return -1;

    g_jni.gadget_strs.ctor = (*env)->GetMethodID(
            env, g_jni.gadget_strs.cls, "<init>",
            KOTLIN_USBG_GADGET_STRS_CTOR_SIG);
    if (!g_jni.gadget_strs.ctor) goto fail;

    g_jni.gadget_strs.f_manufacturer = (*env)->GetFieldID(
            env, g_jni.gadget_strs.cls, "manufacturer", "Ljava/lang/String;");
    g_jni.gadget_strs.f_product = (*env)->GetFieldID(
            env, g_jni.gadget_strs.cls, "product",      "Ljava/lang/String;");
    g_jni.gadget_strs.f_serial = (*env)->GetFieldID(
            env, g_jni.gadget_strs.cls, "serial",       "Ljava/lang/String;");

    if (!g_jni.gadget_strs.f_manufacturer
        || !g_jni.gadget_strs.f_product
        || !g_jni.gadget_strs.f_serial) {
        goto fail;
    }
    return 0;

    fail:
    usbtk_usbg_gadget_strs_jni_release(env);
    return -1;
}

static void usbtk_usbg_config_attrs_jni_release(JNIEnv *env)
{
    if (g_jni.config_attrs.cls) {
        (*env)->DeleteGlobalRef(env, g_jni.config_attrs.cls);
        g_jni.config_attrs.cls = NULL;
    }
}

static int usbtk_usbg_config_attrs_jni_init(JNIEnv *env)
{
    if (g_jni.config_attrs.cls) return 0;

    jclass local = (*env)->FindClass(env, KOTLIN_USBG_CONFIG_ATTRS_CLASS);
    if (!local) return -1;

    g_jni.config_attrs.cls = (jclass)(*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (!g_jni.config_attrs.cls) return -1;

    g_jni.config_attrs.ctor = (*env)->GetMethodID(env, g_jni.config_attrs.cls, "<init>",
                                                  KOTLIN_USBG_CONFIG_ATTRS_CTOR_SIG);
    if (!g_jni.config_attrs.ctor) goto fail;

    g_jni.config_attrs.f_bmAttributes    = (*env)->GetFieldID(env, g_jni.config_attrs.cls, "bmAttributes",    "B");
    g_jni.config_attrs.f_bMaxPower       = (*env)->GetFieldID(env, g_jni.config_attrs.cls, "bMaxPower",       "B");

    if (!g_jni.config_attrs.f_bmAttributes || !g_jni.config_attrs.f_bMaxPower) {
        goto fail;
    }
    return 0;

    fail:
    usbtk_usbg_config_attrs_jni_release(env);
    return -1;
}

int usbtk_dto_init(JNIEnv *env) {
    if (usbtk_usbg_gadget_attrs_jni_init(env)
        || usbtk_usbg_gadget_strs_jni_init(env)
        || usbtk_usbg_config_attrs_jni_init(env)) {
        usbtk_dto_release(env);
        return -1;
    }

    return 0;
}

void usbtk_dto_release(JNIEnv *env) {
    usbtk_usbg_gadget_attrs_jni_release(env);
    usbtk_usbg_gadget_strs_jni_release(env);
    usbtk_usbg_config_attrs_jni_release(env);
}

jobject usbtk_usbg_gadget_attrs_to_kotlin(JNIEnv *env,
                                          const struct usbg_gadget_attrs *src)
{
    if (!g_jni.gadget_attrs.cls || !src) return NULL;

    return (*env)->NewObject(env, g_jni.gadget_attrs.cls, g_jni.gadget_attrs.ctor,
                             (jshort) src->bcdUSB,
                             (jbyte)  src->bDeviceClass,
                             (jbyte)  src->bDeviceSubClass,
                             (jbyte)  src->bDeviceProtocol,
                             (jbyte)  src->bMaxPacketSize0,
                             (jshort) src->idVendor,
                             (jshort) src->idProduct,
                             (jshort) src->bcdDevice);
}

int usbtk_usbg_gadget_attrs_from_kotlin(JNIEnv *env,
                                        jobject obj,
                                        struct usbg_gadget_attrs *dst)
{
    if (!g_jni.gadget_attrs.cls || !obj || !dst) return -1;

    dst->bcdUSB          = (uint16_t)(*env)->GetShortField(env, obj, g_jni.gadget_attrs.f_bcdUSB);
    dst->bDeviceClass    = (uint8_t) (*env)->GetByteField (env, obj, g_jni.gadget_attrs.f_bDeviceClass);
    dst->bDeviceSubClass = (uint8_t) (*env)->GetByteField (env, obj, g_jni.gadget_attrs.f_bDeviceSubClass);
    dst->bDeviceProtocol = (uint8_t) (*env)->GetByteField (env, obj, g_jni.gadget_attrs.f_bDeviceProtocol);
    dst->bMaxPacketSize0 = (uint8_t) (*env)->GetByteField (env, obj, g_jni.gadget_attrs.f_bMaxPacketSize0);
    dst->idVendor        = (uint16_t)(*env)->GetShortField(env, obj, g_jni.gadget_attrs.f_idVendor);
    dst->idProduct       = (uint16_t)(*env)->GetShortField(env, obj, g_jni.gadget_attrs.f_idProduct);
    dst->bcdDevice       = (uint16_t)(*env)->GetShortField(env, obj, g_jni.gadget_attrs.f_bcdDevice);

    return 0;
}

jobject usbtk_usbg_gadget_strs_to_kotlin(JNIEnv *env,
                                         const struct usbg_gadget_strs *src)
{
    if (!g_jni.gadget_strs.cls || !src) return NULL;

    jstring j_manufacturer = src->manufacturer
                             ? (*env)->NewStringUTF(env, src->manufacturer) : NULL;
    jstring j_product = src->product
                        ? (*env)->NewStringUTF(env, src->product) : NULL;
    jstring j_serial = src->serial
                       ? (*env)->NewStringUTF(env, src->serial) : NULL;

    jobject obj = (*env)->NewObject(env, g_jni.gadget_strs.cls, g_jni.gadget_strs.ctor,
                                    j_manufacturer, j_product, j_serial);

    if (j_manufacturer) (*env)->DeleteLocalRef(env, j_manufacturer);
    if (j_product)      (*env)->DeleteLocalRef(env, j_product);
    if (j_serial)       (*env)->DeleteLocalRef(env, j_serial);

    return obj;
}

int usbtk_usbg_gadget_strs_from_kotlin(JNIEnv *env,
                                       jobject obj,
                                       struct usbg_gadget_strs *dst)
{
    if (!g_jni.gadget_strs.cls || !obj || !dst) return -1;

    dst->manufacturer = usbtk_jstring_dup(env, obj, g_jni.gadget_strs.f_manufacturer);
    dst->product      = usbtk_jstring_dup(env, obj, g_jni.gadget_strs.f_product);
    dst->serial       = usbtk_jstring_dup(env, obj, g_jni.gadget_strs.f_serial);

    return 0;
}

jobject usbtk_usbg_config_attrs_to_kotlin(JNIEnv *env,
                                          const struct usbg_config_attrs *src)
{
    if (!g_jni.config_attrs.cls || !src) return NULL;

    return (*env)->NewObject(env, g_jni.config_attrs.cls, g_jni.config_attrs.ctor,
                             (jbyte)  src->bmAttributes,
                             (jbyte)  src->bMaxPower);
}

int usbtk_usbg_config_attrs_from_kotlin(JNIEnv *env,
                                        jobject obj,
                                        struct usbg_config_attrs *dst)
{
    if (!g_jni.config_attrs.cls || !obj || !dst) return -1;

    dst->bmAttributes    = (uint8_t) (*env)->GetByteField (env, obj, g_jni.config_attrs.f_bmAttributes);
    dst->bMaxPower       = (uint8_t) (*env)->GetByteField (env, obj, g_jni.config_attrs.f_bMaxPower);

    return 0;
}

jstring usbtk_usbg_config_strs_to_jstring(JNIEnv *env,
                                         const struct usbg_config_strs *src)
{
    if (!src || !src->configuration) return NULL;
    return (*env)->NewStringUTF(env, src->configuration);
}

int usbtk_usbg_config_strs_from_jstring(JNIEnv *env,
                                       jstring src,
                                       struct usbg_config_strs *dst)
{
    if (!dst) return -1;

    if (!src) {
        dst->configuration = NULL;
        return 0;
    }

    const char *utf = (*env)->GetStringUTFChars(env, src, NULL);
    if (!utf) return -1;

    dst->configuration = strdup(utf);

    (*env)->ReleaseStringUTFChars(env, src, utf);
    return dst->configuration ? 0 : -1;
}
