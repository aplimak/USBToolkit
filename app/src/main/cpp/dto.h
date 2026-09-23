//
// Created by aeliux on 9/23/26.
//

#ifndef USBTOOLKIT_DTO_H
#define USBTOOLKIT_DTO_H

#include <jni.h>

struct usbg_gadget_attrs;

int usbtk_dto_init(JNIEnv *env);
void usbtk_dto_release(JNIEnv *env);
jobject usbtk_usbg_gadget_attrs_to_kotlin(JNIEnv *env,
                                          const struct usbg_gadget_attrs *src);
int usbtk_usbg_gadget_attrs_from_kotlin(JNIEnv *env,
                                        jobject obj,
                                        struct usbg_gadget_attrs *dst);

#endif //USBTOOLKIT_DTO_H
