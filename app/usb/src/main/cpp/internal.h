//
// Created by aeliux on 11/16/26.
//

#ifndef USBTK_INTERNAL_H
#define USBTK_INTERNAL_H

#include <axsn/error.h>
#include <usbg/usbg.h>

#define USBTK_JC_UE "ir/aeliux/usbtoolkit/usb/UsbException"
#define USBTK_JC_EAEE "ir/aeliux/usbtoolkit/usb/EntityAlreadyExistsException"
#define USBTK_JC_ENFE "ir/aeliux/usbtoolkit/usb/EntityNotFoundException"
#define USBTK_JC_UFE "ir/aeliux/usbtoolkit/usb/UnsupportedFunctionException"

#define USBTK_THROW_UE(env, ...)   AXSN_THROW((env), USBTK_JC_UE, __VA_ARGS__)
#define USBTK_THROW_EAEE(env, ...)   AXSN_THROW((env), USBTK_JC_EAEE, __VA_ARGS__)
#define USBTK_THROW_ENFE(env, ...)   AXSN_THROW((env), USBTK_JC_ENFE, __VA_ARGS__)
#define USBTK_THROW_UFE(env, ...)   AXSN_THROW((env), USBTK_JC_UFE, __VA_ARGS__)

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

#endif //USBTK_INTERNAL_H
