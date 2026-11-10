//
// Created by aeliux on 9/22/26.
//

#include "dto.h"

#include "error.h"
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <usbg/usbg.h>
#include <usbg/function/ms.h>

#define KOTLIN_USBG_GADGET_ATTRS_CLASS "ir/aeliux/usbtoolkit/shared/dto/UsbgGadgetAttrs"
#define KOTLIN_USBG_GADGET_ATTRS_CTOR_SIG "(SBBBBSSS)V"

#define KOTLIN_USBG_GADGET_STRS_CLASS "ir/aeliux/usbtoolkit/shared/dto/UsbgGadgetStrs"
#define KOTLIN_USBG_GADGET_STRS_CTOR_SIG "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"

#define KOTLIN_USBG_CONFIG_ATTRS_CLASS "ir/aeliux/usbtoolkit/shared/dto/UsbgConfigAttrs"
#define KOTLIN_USBG_CONFIG_ATTRS_CTOR_SIG "(BB)V"

#define KOTLIN_USBG_F_MASS_STORAGE_LUN_CLASS "ir/aeliux/usbtoolkit/shared/dto/function/UsbgMassStorageLun"
#define KOTLIN_USBG_F_MASS_STORAGE_LUN_CTOR_SIG "(IZZZZLjava/lang/String;Ljava/lang/String;)V"
#define KOTLIN_USBG_F_MASS_STORAGE_ATTRS_CLASS "ir/aeliux/usbtoolkit/shared/dto/function/UsbgMassStorageAttrs"
#define KOTLIN_USBG_F_MASS_STORAGE_ATTRS_CTOR_SIG "(ZLjava/util/List;)V"

static struct
{
    struct
    {
        jclass cls;
        jmethodID ctor;
        jfieldID f_bcdUSB;
        jfieldID f_bDeviceClass;
        jfieldID f_bDeviceSubClass;
        jfieldID f_bDeviceProtocol;
        jfieldID f_bMaxPacketSize0;
        jfieldID f_idVendor;
        jfieldID f_idProduct;
        jfieldID f_bcdDevice;
    } gadget_attrs;
    struct
    {
        jclass cls;
        jmethodID ctor;
        jfieldID f_manufacturer;
        jfieldID f_product;
        jfieldID f_serial;
    } gadget_strs;
    struct
    {
        jclass cls;
        jmethodID ctor;
        jfieldID f_bmAttributes;
        jfieldID f_bMaxPower;
    } config_attrs;
    struct
    {
        jclass cls;
        jmethodID ctor;
        jfieldID f_id;
        jfieldID f_cdrom;
        jfieldID f_ro;
        jfieldID f_nofua;
        jfieldID f_removable;
        jfieldID f_file;
        jfieldID f_inquiry_string;
    } f_mass_storage_lun;
    struct
    {
        jclass cls;
        jmethodID ctor;
        jfieldID f_stall;
        jfieldID f_luns;
    } f_mass_storage_attrs;
    struct
    {
        jclass cls;
        jmethodID ctor;
        jmethodID m_add;
        jmethodID m_size;
        jmethodID m_get;
    } java_list;
} g_jni = {0};

/* Reads obj.<field> as a String, returns a freshly malloc'd UTF-8 copy,
 * or NULL if the field is null. Caller frees with free(). */
static char *usbtk_jstring_dup(JNIEnv *env, jobject obj, jfieldID fid)
{
    jstring js = (jstring) (*env)->GetObjectField(env, obj, fid);
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

    g_jni.gadget_attrs.cls = (jclass) (*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (!g_jni.gadget_attrs.cls) return -1;

    g_jni.gadget_attrs.ctor = (*env)->GetMethodID(env, g_jni.gadget_attrs.cls, "<init>",
                                                  KOTLIN_USBG_GADGET_ATTRS_CTOR_SIG);
    if (!g_jni.gadget_attrs.ctor) goto fail;

    g_jni.gadget_attrs.f_bcdUSB = (*env)->GetFieldID(env, g_jni.gadget_attrs.cls, "bcdUSB", "S");
    g_jni.gadget_attrs.f_bDeviceClass = (*env)->GetFieldID(env,
                                                           g_jni.gadget_attrs.cls,
                                                           "bDeviceClass",
                                                           "B");
    g_jni.gadget_attrs.f_bDeviceSubClass = (*env)->GetFieldID(env,
                                                              g_jni.gadget_attrs.cls,
                                                              "bDeviceSubClass",
                                                              "B");
    g_jni.gadget_attrs.f_bDeviceProtocol = (*env)->GetFieldID(env,
                                                              g_jni.gadget_attrs.cls,
                                                              "bDeviceProtocol",
                                                              "B");
    g_jni.gadget_attrs.f_bMaxPacketSize0 = (*env)->GetFieldID(env,
                                                              g_jni.gadget_attrs.cls,
                                                              "bMaxPacketSize0",
                                                              "B");
    g_jni.gadget_attrs.f_idVendor = (*env)->GetFieldID(env,
                                                       g_jni.gadget_attrs.cls,
                                                       "idVendor",
                                                       "S");
    g_jni.gadget_attrs.f_idProduct = (*env)->GetFieldID(env,
                                                        g_jni.gadget_attrs.cls,
                                                        "idProduct",
                                                        "S");
    g_jni.gadget_attrs.f_bcdDevice = (*env)->GetFieldID(env,
                                                        g_jni.gadget_attrs.cls,
                                                        "bcdDevice",
                                                        "S");

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

    g_jni.gadget_strs.cls = (jclass) (*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (!g_jni.gadget_strs.cls) return -1;

    g_jni.gadget_strs.ctor = (*env)->GetMethodID(
            env, g_jni.gadget_strs.cls, "<init>",
            KOTLIN_USBG_GADGET_STRS_CTOR_SIG);
    if (!g_jni.gadget_strs.ctor) goto fail;

    g_jni.gadget_strs.f_manufacturer = (*env)->GetFieldID(
            env, g_jni.gadget_strs.cls, "manufacturer", "Ljava/lang/String;");
    g_jni.gadget_strs.f_product = (*env)->GetFieldID(
            env, g_jni.gadget_strs.cls, "product", "Ljava/lang/String;");
    g_jni.gadget_strs.f_serial = (*env)->GetFieldID(
            env, g_jni.gadget_strs.cls, "serial", "Ljava/lang/String;");

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

    g_jni.config_attrs.cls = (jclass) (*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (!g_jni.config_attrs.cls) return -1;

    g_jni.config_attrs.ctor = (*env)->GetMethodID(env, g_jni.config_attrs.cls, "<init>",
                                                  KOTLIN_USBG_CONFIG_ATTRS_CTOR_SIG);
    if (!g_jni.config_attrs.ctor) goto fail;

    g_jni.config_attrs.f_bmAttributes = (*env)->GetFieldID(env,
                                                           g_jni.config_attrs.cls,
                                                           "bmAttributes",
                                                           "B");
    g_jni.config_attrs.f_bMaxPower = (*env)->GetFieldID(env,
                                                        g_jni.config_attrs.cls,
                                                        "bMaxPower",
                                                        "B");

    if (!g_jni.config_attrs.f_bmAttributes || !g_jni.config_attrs.f_bMaxPower) {
        goto fail;
    }
    return 0;

    fail:
    usbtk_usbg_config_attrs_jni_release(env);
    return -1;
}

static void usbtk_usbg_f_mass_storage_lun_jni_release(JNIEnv *env)
{
    if (g_jni.f_mass_storage_lun.cls) {
        (*env)->DeleteGlobalRef(env, g_jni.f_mass_storage_lun.cls);
        g_jni.f_mass_storage_lun.cls = NULL;
    }
}

static int usbtk_usbg_f_mass_storage_lun_jni_init(JNIEnv *env)
{
    if (g_jni.f_mass_storage_lun.cls) return 0;

    jclass local = (*env)->FindClass(env, KOTLIN_USBG_F_MASS_STORAGE_LUN_CLASS);
    if (!local) return -1;

    g_jni.f_mass_storage_lun.cls = (jclass) (*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (!g_jni.f_mass_storage_lun.cls) return -1;

    g_jni.f_mass_storage_lun.ctor = (*env)->GetMethodID(
            env, g_jni.f_mass_storage_lun.cls, "<init>",
            KOTLIN_USBG_F_MASS_STORAGE_LUN_CTOR_SIG);
    if (!g_jni.f_mass_storage_lun.ctor) goto fail;

    g_jni.f_mass_storage_lun.f_id = (*env)->GetFieldID(
            env, g_jni.f_mass_storage_lun.cls, "id", "I");
    g_jni.f_mass_storage_lun.f_cdrom = (*env)->GetFieldID(
            env, g_jni.f_mass_storage_lun.cls, "cdrom", "Z");
    g_jni.f_mass_storage_lun.f_ro = (*env)->GetFieldID(
            env, g_jni.f_mass_storage_lun.cls, "ro", "Z");
    g_jni.f_mass_storage_lun.f_nofua = (*env)->GetFieldID(
            env, g_jni.f_mass_storage_lun.cls, "nofua", "Z");
    g_jni.f_mass_storage_lun.f_removable = (*env)->GetFieldID(
            env, g_jni.f_mass_storage_lun.cls, "removable", "Z");
    g_jni.f_mass_storage_lun.f_file = (*env)->GetFieldID(
            env, g_jni.f_mass_storage_lun.cls, "file", "Ljava/lang/String;");
    g_jni.f_mass_storage_lun.f_inquiry_string = (*env)->GetFieldID(
            env, g_jni.f_mass_storage_lun.cls, "inquiryString", "Ljava/lang/String;");

    if (!g_jni.f_mass_storage_lun.f_id
        || !g_jni.f_mass_storage_lun.f_cdrom
        || !g_jni.f_mass_storage_lun.f_ro
        || !g_jni.f_mass_storage_lun.f_nofua
        || !g_jni.f_mass_storage_lun.f_removable
        || !g_jni.f_mass_storage_lun.f_file
        || !g_jni.f_mass_storage_lun.f_inquiry_string) {
        goto fail;
    }
    return 0;

    fail:
    usbtk_usbg_f_mass_storage_lun_jni_release(env);
    return -1;
}

static void usbtk_usbg_f_mass_storage_attrs_jni_release(JNIEnv *env)
{
    if (g_jni.f_mass_storage_attrs.cls) {
        (*env)->DeleteGlobalRef(env, g_jni.f_mass_storage_attrs.cls);
        g_jni.f_mass_storage_attrs.cls = NULL;
    }
}

static int usbtk_usbg_f_mass_storage_attrs_jni_init(JNIEnv *env)
{
    if (g_jni.f_mass_storage_attrs.cls) return 0;

    jclass local = (*env)->FindClass(env, KOTLIN_USBG_F_MASS_STORAGE_ATTRS_CLASS);
    if (!local) return -1;

    g_jni.f_mass_storage_attrs.cls = (jclass) (*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (!g_jni.f_mass_storage_attrs.cls) return -1;

    g_jni.f_mass_storage_attrs.ctor = (*env)->GetMethodID(
            env, g_jni.f_mass_storage_attrs.cls, "<init>",
            KOTLIN_USBG_F_MASS_STORAGE_ATTRS_CTOR_SIG);
    if (!g_jni.f_mass_storage_attrs.ctor) goto fail;

    g_jni.f_mass_storage_attrs.f_stall = (*env)->GetFieldID(
            env, g_jni.f_mass_storage_attrs.cls, "stall", "Z");
    g_jni.f_mass_storage_attrs.f_luns = (*env)->GetFieldID(
            env, g_jni.f_mass_storage_attrs.cls, "luns", "Ljava/util/List;");

    if (!g_jni.f_mass_storage_attrs.f_stall
        || !g_jni.f_mass_storage_attrs.f_luns) {
        goto fail;
    }
    return 0;

    fail:
    usbtk_usbg_f_mass_storage_attrs_jni_release(env);
    return -1;
}

static void usbtk_java_list_jni_release(JNIEnv *env)
{
    if (g_jni.java_list.cls) {
        (*env)->DeleteGlobalRef(env, g_jni.java_list.cls);
        g_jni.java_list.cls = NULL;
    }
}

static int usbtk_java_list_jni_init(JNIEnv *env)
{
    if (g_jni.java_list.cls) return 0;

    jclass local = (*env)->FindClass(env, "java/util/List");
    if (!local) return -1;

    g_jni.java_list.cls = (jclass) (*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    if (!g_jni.java_list.cls) return -1;

    g_jni.java_list.ctor = (*env)->GetMethodID(
            env, g_jni.java_list.cls, "<init>",
            "()V");
    if (!g_jni.java_list.ctor) goto fail;

    g_jni.java_list.m_add = (*env)->GetMethodID(env,
                                                g_jni.java_list.cls,
                                                "add",
                                                "(Ljava/lang/Object;)Z");
    g_jni.java_list.m_size = (*env)->GetMethodID(env, g_jni.java_list.cls, "size", "()I");
    g_jni.java_list.m_get = (*env)->GetMethodID(env,
                                                g_jni.java_list.cls,
                                                "get",
                                                "(I)Ljava/lang/Object;");

    if (!g_jni.java_list.m_add
        || !g_jni.java_list.m_size
        || !g_jni.java_list.m_get) {
        goto fail;
    }
    return 0;

    fail:
    usbtk_java_list_jni_release(env);
    return -1;
}

int usbtk_dto_init(JNIEnv *env)
{
    if (usbtk_usbg_gadget_attrs_jni_init(env)
        || usbtk_usbg_gadget_strs_jni_init(env)
        || usbtk_usbg_config_attrs_jni_init(env)
        || usbtk_usbg_f_mass_storage_lun_jni_init(env)
        || usbtk_usbg_f_mass_storage_attrs_jni_init(env)
        || usbtk_java_list_jni_init(env)) {
        usbtk_dto_release(env);
        return -1;
    }

    return 0;
}

void usbtk_dto_release(JNIEnv *env)
{
    usbtk_usbg_gadget_attrs_jni_release(env);
    usbtk_usbg_gadget_strs_jni_release(env);
    usbtk_usbg_config_attrs_jni_release(env);
    usbtk_usbg_f_mass_storage_lun_jni_release(env);
    usbtk_usbg_f_mass_storage_attrs_jni_release(env);
    usbtk_java_list_jni_release(env);
}

jobject usbtk_usbg_gadget_attrs_to_kotlin(JNIEnv *env,
                                          const struct usbg_gadget_attrs *src)
{
    if (!g_jni.gadget_attrs.cls || !src) return NULL;

    return (*env)->NewObject(env, g_jni.gadget_attrs.cls, g_jni.gadget_attrs.ctor,
                             (jshort) src->bcdUSB,
                             (jbyte) src->bDeviceClass,
                             (jbyte) src->bDeviceSubClass,
                             (jbyte) src->bDeviceProtocol,
                             (jbyte) src->bMaxPacketSize0,
                             (jshort) src->idVendor,
                             (jshort) src->idProduct,
                             (jshort) src->bcdDevice);
}

int usbtk_usbg_gadget_attrs_from_kotlin(JNIEnv *env,
                                        jobject obj,
                                        struct usbg_gadget_attrs *dst)
{
    if (!g_jni.gadget_attrs.cls || !obj || !dst) return -1;

    dst->bcdUSB = (uint16_t) (*env)->GetShortField(env, obj, g_jni.gadget_attrs.f_bcdUSB);
    dst->bDeviceClass = (uint8_t) (*env)->GetByteField(env, obj, g_jni.gadget_attrs.f_bDeviceClass);
    dst->bDeviceSubClass = (uint8_t) (*env)->GetByteField(env,
                                                          obj,
                                                          g_jni.gadget_attrs.f_bDeviceSubClass);
    dst->bDeviceProtocol = (uint8_t) (*env)->GetByteField(env,
                                                          obj,
                                                          g_jni.gadget_attrs.f_bDeviceProtocol);
    dst->bMaxPacketSize0 = (uint8_t) (*env)->GetByteField(env,
                                                          obj,
                                                          g_jni.gadget_attrs.f_bMaxPacketSize0);
    dst->idVendor = (uint16_t) (*env)->GetShortField(env, obj, g_jni.gadget_attrs.f_idVendor);
    dst->idProduct = (uint16_t) (*env)->GetShortField(env, obj, g_jni.gadget_attrs.f_idProduct);
    dst->bcdDevice = (uint16_t) (*env)->GetShortField(env, obj, g_jni.gadget_attrs.f_bcdDevice);

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
    if (j_product) (*env)->DeleteLocalRef(env, j_product);
    if (j_serial) (*env)->DeleteLocalRef(env, j_serial);

    return obj;
}

int usbtk_usbg_gadget_strs_from_kotlin(JNIEnv *env,
                                       jobject obj,
                                       struct usbg_gadget_strs *dst)
{
    if (!g_jni.gadget_strs.cls || !obj || !dst) return -1;

    dst->manufacturer = usbtk_jstring_dup(env, obj, g_jni.gadget_strs.f_manufacturer);
    dst->product = usbtk_jstring_dup(env, obj, g_jni.gadget_strs.f_product);
    dst->serial = usbtk_jstring_dup(env, obj, g_jni.gadget_strs.f_serial);

    return 0;
}

jobject usbtk_usbg_config_attrs_to_kotlin(JNIEnv *env,
                                          const struct usbg_config_attrs *src)
{
    if (!g_jni.config_attrs.cls || !src) return NULL;

    return (*env)->NewObject(env, g_jni.config_attrs.cls, g_jni.config_attrs.ctor,
                             (jbyte) src->bmAttributes,
                             (jbyte) src->bMaxPower);
}

int usbtk_usbg_config_attrs_from_kotlin(JNIEnv *env,
                                        jobject obj,
                                        struct usbg_config_attrs *dst)
{
    if (!g_jni.config_attrs.cls || !obj || !dst) return -1;

    dst->bmAttributes = (uint8_t) (*env)->GetByteField(env, obj, g_jni.config_attrs.f_bmAttributes);
    dst->bMaxPower = (uint8_t) (*env)->GetByteField(env, obj, g_jni.config_attrs.f_bMaxPower);

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

jobject usbtk_usbg_f_mass_storage_lun_to_kotlin(JNIEnv *env,
                                                const struct usbg_f_ms_lun_attrs *src)
{
    if (!g_jni.f_mass_storage_lun.cls || !src) return NULL;

    jstring j_file = src->file
                     ? (*env)->NewStringUTF(env, src->file) : NULL;
    jstring j_inquiry_string = src->inquiry_string
                               ? (*env)->NewStringUTF(env, src->inquiry_string) : NULL;

    jobject obj = (*env)->NewObject(env,
                                    g_jni.f_mass_storage_lun.cls,
                                    g_jni.f_mass_storage_lun.ctor,
                                    src->id,
                                    src->cdrom,
                                    src->ro,
                                    src->nofua,
                                    src->removable,
                                    j_file,
                                    j_inquiry_string);

    if (j_file) (*env)->DeleteLocalRef(env, j_file);
    if (j_inquiry_string) (*env)->DeleteLocalRef(env, j_inquiry_string);

    return obj;
}

int usbtk_usbg_f_mass_storage_lun_from_kotlin(JNIEnv *env,
                                              jobject obj,
                                              struct usbg_f_ms_lun_attrs *dst)
{
    if (!g_jni.f_mass_storage_lun.cls || !obj || !dst) return -1;

    dst->id = (*env)->GetIntField(env, obj, g_jni.f_mass_storage_lun.f_id);
    dst->cdrom = (*env)->GetBooleanField(env, obj, g_jni.f_mass_storage_lun.f_cdrom);
    dst->ro = (*env)->GetBooleanField(env, obj, g_jni.f_mass_storage_lun.f_ro);
    dst->nofua = (*env)->GetBooleanField(env, obj, g_jni.f_mass_storage_lun.f_nofua);
    dst->removable = (*env)->GetBooleanField(env, obj, g_jni.f_mass_storage_lun.f_removable);
    dst->file = usbtk_jstring_dup(env, obj, g_jni.f_mass_storage_lun.f_file);
    dst->inquiry_string = usbtk_jstring_dup(env, obj, g_jni.f_mass_storage_lun.f_inquiry_string);

    return 0;
}

jobject usbtk_usbg_f_mass_storage_attrs_to_kotlin(JNIEnv *env,
                                                  const struct usbg_f_ms_attrs *src)
{
    if (!g_jni.f_mass_storage_attrs.cls || !src) return NULL;
    jobject obj = NULL;

    jobject j_luns = (*env)->NewObject(env, g_jni.java_list.cls, g_jni.java_list.ctor);
    if (j_luns == NULL) {
        if (!(*env)->ExceptionCheck(env)) {
            USBTK_THROW_RTE(env,
                            "usbtk_usbg_f_mass_storage_attrs_to_kotlin: Failed to create list");
        }
        goto done;
    }
    if (src->luns) {
        for (int i = 0; i < src->nluns; i++) {
            jobject lun = usbtk_usbg_f_mass_storage_lun_to_kotlin(env, src->luns[i]);
            (*env)->CallBooleanMethod(env, j_luns, g_jni.java_list.m_add, lun);
            (*env)->DeleteLocalRef(env, lun);
        }
    }

    obj = (*env)->NewObject(env,
                            g_jni.f_mass_storage_attrs.cls,
                            g_jni.f_mass_storage_attrs.ctor,
                            src->stall,
                            j_luns);

    done:
    if (j_luns) (*env)->DeleteLocalRef(env, j_luns);
    return obj;
}

int usbtk_usbg_f_mass_storage_attrs_from_kotlin(JNIEnv *env,
                                                jobject obj,
                                                struct usbg_f_ms_attrs *dst)
{
    if (!g_jni.f_mass_storage_attrs.cls || !obj || !dst) return -1;
    bool failed = false;
    memset(dst, 0, sizeof(*dst));

    dst->stall = (*env)->GetBooleanField(env, obj, g_jni.f_mass_storage_attrs.f_stall);

    jobject luns = (*env)->GetObjectField(env, obj, g_jni.f_mass_storage_attrs.f_luns);
    if (luns == NULL) {
        if (!(*env)->ExceptionCheck(env)) {
            USBTK_THROW_NPE(env, "UsbgMassStorageAttrs.luns");
        }
        goto fail;
    }

    jint n = (*env)->CallIntMethod(env, luns, g_jni.java_list.m_size);
    dst->nluns = n;

    if (n == 0) {
        dst->luns = NULL;
        goto done;
    }

    dst->luns = calloc(n + 1, sizeof(struct usbg_f_ms_lun_attrs *));
    if (!dst->luns) {
        USBTK_THROW_OOM(env, "usbtk_usbg_f_mass_storage_attrs_from_kotlin: %s", "dst->luns");
        goto fail;
    }
    for (jint i = 0; i < n; i++) {
        jobject lun = (*env)->CallObjectMethod(env, luns, g_jni.java_list.m_get, i);
        if (lun == NULL) {
            if (!(*env)->ExceptionCheck(env)) {
                USBTK_THROW_RTE(env,
                                "usbtk_usbg_f_mass_storage_attrs_from_kotlin: Failed to get the element index %d from luns list",
                                i);
            }
            goto fail;
        }
        dst->luns[i] = calloc(1, sizeof(*dst->luns[i]));
        if (usbtk_usbg_f_mass_storage_lun_from_kotlin(env, lun, dst->luns[i]) == -1) {
            if (!(*env)->ExceptionCheck(env)) {
                USBTK_THROW_RTE(env,
                                "usbtk_usbg_f_mass_storage_attrs_from_kotlin: Failed to read the element index %d from luns list",
                                i);
            }
            goto fail;
        }
        (*env)->DeleteLocalRef(env, lun);
    }
    dst->luns[n] = NULL;

    goto done;

    fail:
    failed = true;
    dst->nluns = 0;
    usbtk_usbg_f_mass_storage_attrs_free(dst);
    if (!(*env)->ExceptionCheck(env)) {
        USBTK_THROW_RTE(env, "usbtk_usbg_f_mass_storage_attrs_from_kotlin");
    }
    done:
    if (luns) (*env)->DeleteLocalRef(env, luns);
    return failed ? -1 : 0;
}

void usbtk_usbg_f_mass_storage_attrs_free(struct usbg_f_ms_attrs *ptr)
{
    if (!ptr || ptr->nluns == 0) return;
    for (int i = 0; i < ptr->nluns; i++) {
        free((void *) (uintptr_t) ptr->luns[i]->file);
        free((void *) (uintptr_t) ptr->luns[i]->inquiry_string);
        free(ptr->luns[i]);
    }
    free(ptr->luns);

    memset(ptr, 0, sizeof(*ptr));
}
