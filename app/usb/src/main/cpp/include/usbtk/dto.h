//
// Created by aeliux on 9/23/26.
//

#ifndef USBTK_DTO_H
#define USBTK_DTO_H

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

#define USBTK_API __attribute__((visibility("default")))

struct usbg_gadget_attrs;
struct usbg_gadget_strs;
struct usbg_config_attrs;
struct usbg_config_strs;
struct usbg_f_ms_lun_attrs;
struct usbg_f_ms_attrs;

USBTK_API jobject usbtk_export_usbg_gadget_attrs(JNIEnv *env,
                                       const struct usbg_gadget_attrs *src);

USBTK_API int usbtk_import_usbg_gadget_attrs(JNIEnv *env,
                                   jobject obj,
                                   struct usbg_gadget_attrs *dst);

USBTK_API jobject usbtk_export_usbg_gadget_strs(JNIEnv *env,
                                      const struct usbg_gadget_strs *src);

// Structs created by this function must be freed using usbtk_free_usbg_gadget_strs.
USBTK_API int usbtk_import_usbg_gadget_strs(JNIEnv *env,
                                  jobject obj,
                                  struct usbg_gadget_strs *dst);

USBTK_API void usbtk_free_usbg_gadget_strs(struct usbg_gadget_strs *ptr);

USBTK_API jobject usbtk_export_usbg_config_attrs(JNIEnv *env,
                                       const struct usbg_config_attrs *src);

USBTK_API int usbtk_import_usbg_config_attrs(JNIEnv *env,
                                   jobject obj,
                                   struct usbg_config_attrs *dst);

USBTK_API jobject usbtk_export_usbg_config_strs(JNIEnv *env,
                                      const struct usbg_config_strs *src);

// Structs created by this function must be freed using usbtk_free_usbg_config_strs.
USBTK_API int usbtk_import_usbg_config_strs(JNIEnv *env,
                                  jobject obj,
                                  struct usbg_config_strs *dst);

USBTK_API void usbtk_free_usbg_config_strs(struct usbg_config_strs *ptr);

USBTK_API jobject usbtk_export_usbg_f_mass_storage_lun(JNIEnv *env,
                                             const struct usbg_f_ms_lun_attrs *src);

USBTK_API int usbtk_import_usbg_f_mass_storage_lun(JNIEnv *env,
                                         jobject obj,
                                         struct usbg_f_ms_lun_attrs *dst);

USBTK_API jobject usbtk_export_usbg_f_mass_storage_attrs(JNIEnv *env,
                                               const struct usbg_f_ms_attrs *src);

// Structs created by this function must be freed using usbtk_usbg_f_mass_storage_attrs_free.
USBTK_API int usbtk_import_usbg_f_mass_storage_attrs(JNIEnv *env,
                                           jobject obj,
                                           struct usbg_f_ms_attrs *dst);

USBTK_API void usbtk_free_usbg_f_mass_storage_attrs(struct usbg_f_ms_attrs *ptr);

#ifdef __cplusplus
}
#endif

#endif //USBTK_DTO_H
