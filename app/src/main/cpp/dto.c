//
// Created by aeliux on 9/22/26.
//

#include "dto.h"

#include <jni.h>
#include <usbg/usbg.h>

#define KOTLIN_USBG_GADGET_ATTRS_CLASS "ir/aeliux/usbtoolkit/dto/UsbgGadgetAttrs"

/* Constructor descriptor: (S B B B B S S S)V
 *   bcdUSB(S) bDeviceClass(B) bDeviceSubClass(B) bDeviceProtocol(B)
 *   bMaxPacketSize0(B) idVendor(S) idProduct(S) bcdDevice(S) */
#define KOTLIN_USBG_GADGET_ATTRS_CTOR_SIG "(SBBBBSSS)V"

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

static struct {
    usbtk_usbg_gadget_attrs_jni_t gadget_attrs;
    //usbtk_usbg_config_attrs_jni_t config_attrs;
} g_jni = {0};

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
    if (g_jni.gadget_attrs.cls) {
        (*env)->DeleteGlobalRef(env, g_jni.gadget_attrs.cls);
        g_jni.gadget_attrs.cls = NULL;
    }
    return -1;
}

static void usbtk_usbg_gadget_attrs_jni_release(JNIEnv *env)
{
    if (g_jni.gadget_attrs.cls) {
        (*env)->DeleteGlobalRef(env, g_jni.gadget_attrs.cls);
        g_jni.gadget_attrs.cls = NULL;
    }
}

int usbtk_dto_init(JNIEnv *env) {
    int ret = usbtk_usbg_gadget_attrs_jni_init(env);
    if (ret != 0) {
        return ret;
    }

    return 0;
}

void usbtk_dto_release(JNIEnv *env) {
    usbtk_usbg_gadget_attrs_jni_release(env);
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