//
// Created by aeliux on 9/23/26.
//

#ifndef USBTOOLKIT_DTO_H
#define USBTOOLKIT_DTO_H

#include <jni.h>

struct usbg_gadget_attrs;
struct usbg_gadget_strs;
struct usbg_config_attrs;
struct usbg_config_strs;
struct usbg_f_ms_lun_attrs;
struct usbg_f_ms_attrs;

int usbtk_dto_init(JNIEnv *env);

void usbtk_dto_release(JNIEnv *env);

jobject usbtk_export_usbg_gadget_attrs(JNIEnv *env,
                                       const struct usbg_gadget_attrs *src);

int usbtk_import_usbg_gadget_attrs(JNIEnv *env,
                                   jobject obj,
                                   struct usbg_gadget_attrs *dst);

jobject usbtk_export_usbg_gadget_strs(JNIEnv *env,
                                      const struct usbg_gadget_strs *src);

int usbtk_import_usbg_gadget_strs(JNIEnv *env,
                                  jobject obj,
                                  struct usbg_gadget_strs *dst);

jobject usbtk_export_usbg_config_attrs(JNIEnv *env,
                                       const struct usbg_config_attrs *src);

int usbtk_import_usbg_config_attrs(JNIEnv *env,
                                   jobject obj,
                                   struct usbg_config_attrs *dst);

jstring usbtk_export_usbg_config_strs(JNIEnv *env,
                                      const struct usbg_config_strs *src);

int usbtk_import_usbg_config_strs(JNIEnv *env,
                                  jstring src,
                                  struct usbg_config_strs *dst);

jobject usbtk_export_usbg_f_mass_storage_lun(JNIEnv *env,
                                             const struct usbg_f_ms_lun_attrs *src);

int usbtk_import_usbg_f_mass_storage_lun(JNIEnv *env,
                                         jobject obj,
                                         struct usbg_f_ms_lun_attrs *dst);

jobject usbtk_export_usbg_f_mass_storage_attrs(JNIEnv *env,
                                               const struct usbg_f_ms_attrs *src);

// Structs created by this function must be freed using usbtk_usbg_f_mass_storage_attrs_free.
int usbtk_import_usbg_f_mass_storage_attrs(JNIEnv *env,
                                           jobject obj,
                                           struct usbg_f_ms_attrs *dst);

void usbtk_free_usbg_f_mass_storage_attrs(struct usbg_f_ms_attrs *ptr);

#endif //USBTOOLKIT_DTO_H
