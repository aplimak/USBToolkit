# cmake/libusbgx/CMakeLists.txt
#
# CMake replacement for libusbgx 0.3.0's autotools pipeline
# (configure.ac + src/Makefile.am). Same sources, same defines, same
# generated usbg_version.h. Cross-compile flags/toolchain are inherited
# from the parent project.
#
# Inputs (set by parent before add_subdirectory):
#   USBGX_ROOT             path to the vendored libusbgx tree

set(USBGX_VERSION     "0.3.0")
set(USBGX_VERSION_HEX "0x00030000")   # (major<<24)|(minor<<16)|micro, matches configure.ac

if(NOT USBGX_ROOT OR NOT EXISTS "${USBGX_ROOT}/src/usbg.c")
    message(FATAL_ERROR
            "USBGX_ROOT must point at the vendored libusbgx before "
            "add_subdirectory(cmake/libusbgx)")
endif()

# ---- 1. usbg_version.h  (AC_CONFIG_FILES(...include/usbg/usbg_version.h)) --
# usbg_version.h.in only substitutes @USBG_VERSION_HEX@.
set(_usbgx_gen "${CMAKE_CURRENT_BINARY_DIR}/gen")
file(MAKE_DIRECTORY "${_usbgx_gen}/usbg")
configure_file(
        "${USBGX_ROOT}/include/usbg/usbg_version.h.in"
        "${_usbgx_gen}/usbg/usbg_version.h"
        @ONLY)

add_library(usbgx STATIC
        "${USBGX_ROOT}/src/usbg.c"
        "${USBGX_ROOT}/src/usbg_error.c"
        "${USBGX_ROOT}/src/usbg_common.c"

        "${USBGX_ROOT}/src/usbg_schemes_libconfig.c"
        "${USBGX_ROOT}/src/usbg_common_libconfig.c"

        "${USBGX_ROOT}/src/function/9pfs.c"
        "${USBGX_ROOT}/src/function/ether.c"
        "${USBGX_ROOT}/src/function/ffs.c"
        "${USBGX_ROOT}/src/function/hid.c"
        "${USBGX_ROOT}/src/function/loopback.c"
        "${USBGX_ROOT}/src/function/midi.c"
        "${USBGX_ROOT}/src/function/ms.c"
        "${USBGX_ROOT}/src/function/phonet.c"
        "${USBGX_ROOT}/src/function/printer.c"
        "${USBGX_ROOT}/src/function/serial.c"
        "${USBGX_ROOT}/src/function/uac2.c"
        "${USBGX_ROOT}/src/function/uvc.c")

add_library(usbgx::usbgx ALIAS usbgx)

target_include_directories(usbgx SYSTEM
        PUBLIC
        "${USBGX_ROOT}/include"
        "${_usbgx_gen}/usbg"
        PRIVATE
        "${USBGX_ROOT}/src")

# AC_DEFINE([_GNU_SOURCE], ...)
target_compile_definitions(usbgx PRIVATE _GNU_SOURCE)

# Autotools builds with GNU extensions on (default -std=gnuNN).
set_target_properties(usbgx PROPERTIES
        C_STANDARD 11
        C_STANDARD_REQUIRED ON
        C_EXTENSIONS ON
        POSITION_INDEPENDENT_CODE ON)

if(NOT TARGET config)
    message(FATAL_ERROR
            "libconfig is required")
endif()

# configure.ac defines HAS_LIBCONFIG=1 and HAVE_LIBCONFIG_15={0,1};
# TEST_GADGET_SCHEMES then defines HAS_GADGET_SCHEMES=1. Vendored
# libconfig in-tree is >= 1.5 in practice.
target_compile_definitions(usbgx PRIVATE
        HAS_LIBCONFIG=1
        HAVE_LIBCONFIG_15=1
        HAS_GADGET_SCHEMES=1)
target_link_libraries(usbgx PRIVATE config)
