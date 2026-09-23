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

int usbtk_dto_init(JNIEnv *env);
void usbtk_dto_release(JNIEnv *env);

jobject usbtk_usbg_gadget_attrs_to_kotlin(JNIEnv *env,
                                          const struct usbg_gadget_attrs *src);
int usbtk_usbg_gadget_attrs_from_kotlin(JNIEnv *env,
                                        jobject obj,
                                        struct usbg_gadget_attrs *dst);

jobject usbtk_usbg_gadget_strs_to_kotlin(JNIEnv *env,
                                         const struct usbg_gadget_strs *src);
int usbtk_usbg_gadget_strs_from_kotlin(JNIEnv *env,
                                       jobject obj,
                                       struct usbg_gadget_strs *dst);

jobject usbtk_usbg_config_attrs_to_kotlin(JNIEnv *env,
                                          const struct usbg_config_attrs *src);
int usbtk_usbg_config_attrs_from_kotlin(JNIEnv *env,
                                        jobject obj,
                                        struct usbg_config_attrs *dst);

jstring usbtk_usbg_config_strs_to_jstring(JNIEnv *env,
                                          const struct usbg_config_strs *src);
int usbtk_usbg_config_strs_from_jstring(JNIEnv *env,
                                        jstring src,
                                        struct usbg_config_strs *dst);

#endif //USBTOOLKIT_DTO_H
