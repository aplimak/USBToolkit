cmake_minimum_required(VERSION 3.22.1)
project("usbtk" LANGUAGES C)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)

option(AXSN_STRICT     "Enable aggressive warnings and -Werror on the main target" ON)

include(${CMAKE_CURRENT_SOURCE_DIR}/common/common.cmake)
axsn_set_external_build_flags()

set(ANDROID_CMAKE_ARGS
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM}
        -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
        -DANDROID_STL=${ANDROID_STL}
)

include(ExternalProject)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "")

# Shared library
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/common
        ${CMAKE_CURRENT_BINARY_DIR}/build/axsn_common)

# TinyALSA

set(TINYALSA_BUILD_EXAMPLES OFF CACHE BOOL "")
set(TINYALSA_BUILD_UTILS OFF CACHE BOOL "")

add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/third_party/tinyalsa" "${CMAKE_CURRENT_BINARY_DIR}/build/tinyalsa")

# libconfig

set(BUILD_EXAMPLES OFF CACHE BOOL "")
set(BUILD_TESTS OFF CACHE BOOL "")
set(BUILD_CXX OFF CACHE BOOL "")

set(LIBCONFIG_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party/libconfig")
set(LIBCONFIG_BUILD_DIR  "${CMAKE_CURRENT_BINARY_DIR}/build/libconfig")
set(LIBCONFIG_OUT_DIR  "${CMAKE_CURRENT_BINARY_DIR}/out")
add_subdirectory("${LIBCONFIG_SOURCE_DIR}" "${LIBCONFIG_BUILD_DIR}")

# libusbgx

set(USBGX_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/third_party/libusbgx")
add_subdirectory(
        "${CMAKE_CURRENT_SOURCE_DIR}/cmake/libusbgx"
        "${CMAKE_CURRENT_BINARY_DIR}/build/libusbgx")

# Main Library

add_library(${CMAKE_PROJECT_NAME} SHARED
        core.c
        dto.c
        usb_jni.c
)

axsn_apply_warnings(${CMAKE_PROJECT_NAME})
axsn_apply_build_flags(${CMAKE_PROJECT_NAME})

target_link_libraries(${CMAKE_PROJECT_NAME}
        axsn_common
        usbgx
        config
        tinyalsa
        android
        log
        z)

find_package(ptrw REQUIRED CONFIG)
target_link_libraries(${CMAKE_PROJECT_NAME} ptrw::ptrw)
