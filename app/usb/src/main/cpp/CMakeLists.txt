cmake_minimum_required(VERSION 3.22.1)
project("usb")
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

option(USBTK_STRICT     "Enable aggressive warnings and -Werror on the main target" ON)

set(ANDROID_CMAKE_ARGS
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM}
        -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
        -DANDROID_STL=${ANDROID_STL}
)

include(ExternalProject)

# Map Android ABI to the clang target triplet
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(TRIPLET "aarch64-linux-android")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(TRIPLET "armv7a-linux-androideabi")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(TRIPLET "x86_64-linux-android")
elseif(ANDROID_ABI STREQUAL "x86")
    set(TRIPLET "i686-linux-android")
endif()

if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(UTK_LIB_CFLAGS  "-O0 -g3 -fno-omit-frame-pointer -fno-lto")
    set(UTK_LIB_LDFLAGS "")
else()
    set(UTK_LIB_CFLAGS  "-O3 -flto -ffunction-sections -fdata-sections -fvisibility=hidden -DNDEBUG")
    set(UTK_LIB_LDFLAGS "-Wl,--gc-sections -Wl,-s")
endif()

# Most cmake libraries respect it
set(BUILD_SHARED_LIBS OFF CACHE BOOL "")

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

set(USBGX_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party/libusbgx")
set(USBGX_PREFIX     "${CMAKE_CURRENT_BINARY_DIR}/deps/libusbgx")
set(USBGX_BUILD_DIR  "${CMAKE_CURRENT_BINARY_DIR}/build/libusbgx")

ExternalProject_Add(libusbgx_build
        SOURCE_DIR "${USBGX_SOURCE_DIR}"
        BINARY_DIR "${USBGX_BUILD_DIR}"
        PREFIX     "${USBGX_PREFIX}"

        CONFIGURE_COMMAND
        ${CMAKE_COMMAND} -E env
        "CC=${CMAKE_C_COMPILER} --target=${CMAKE_C_COMPILER_TARGET} --sysroot=${CMAKE_SYSROOT}"
        "CXX=${CMAKE_CXX_COMPILER} --target=${CMAKE_C_COMPILER_TARGET} --sysroot=${CMAKE_SYSROOT}"
        "AR=${CMAKE_AR}"
        "RANLIB=${CMAKE_RANLIB}"
        "STRIP=${CMAKE_STRIP}"
        "PKG_CONFIG_PATH="
        "CFLAGS=${UTK_LIB_CFLAGS}"
        "LDFLAGS=${UTK_LIB_LDFLAGS}"
        "LIBCONFIG_CFLAGS=-I${LIBCONFIG_SOURCE_DIR}/lib"
        "LIBCONFIG_LIBS=-L${LIBCONFIG_OUT_DIR} -lconfig"
        <SOURCE_DIR>/configure
        --host=${TRIPLET}
        "--prefix=${USBGX_PREFIX}"
        "--with-sysroot=${CMAKE_SYSROOT}"
        --enable-static
        --disable-shared
        --with-pic

        BUILD_COMMAND   make -j8
        INSTALL_COMMAND make install

        DEPENDS config

        BUILD_BYPRODUCTS "${USBGX_PREFIX}/lib/libusbgx.a"

        LOG_CONFIGURE 1
        LOG_BUILD 1
        LOG_INSTALL 1
)

file(MAKE_DIRECTORY "${USBGX_PREFIX}/include")
file(MAKE_DIRECTORY "${USBGX_PREFIX}/lib")
add_library(usbgx STATIC IMPORTED GLOBAL)
add_dependencies(usbgx libusbgx_build)
set_target_properties(usbgx PROPERTIES
        IMPORTED_LOCATION "${USBGX_PREFIX}/lib/libusbgx.a"
        INTERFACE_INCLUDE_DIRECTORIES "${USBGX_PREFIX}/include"
)

# Main Library

add_library(${CMAKE_PROJECT_NAME} SHARED
        core.c
        dto.c
        usb_jni.c
        ptrw.c
        ptrw_jni.c
)

if(USBTK_STRICT)
    target_compile_options(${CMAKE_PROJECT_NAME} PRIVATE
            -Wall
            -Wextra
            -Wpedantic
            -Wshadow
            -Wpointer-arith
            -Wcast-qual
            -Wcast-align
            -Wstrict-prototypes
            -Wmissing-prototypes
            -Wmissing-declarations
            -Wold-style-definition
            -Wredundant-decls
            -Wformat=2
            -Wformat-security
            -Wnull-dereference
            -Wdouble-promotion
            -Wfloat-equal
            -Wwrite-strings
            -Wvla
            -Wswitch-enum
            -Wundef
            -Winit-self
            -Wuninitialized
            -Wno-unused-parameter   # JNI env/cls often unused; or use (void)env;
            -Werror)
endif()

target_compile_options(${CMAKE_PROJECT_NAME} PRIVATE
        $<$<NOT:$<CONFIG:Debug>>:-O3;-flto;-ffunction-sections;-fdata-sections>
        $<$<CONFIG:Debug>:-O0;-g3;-fno-omit-frame-pointer;-fno-lto>
        "--warning-suppression-mappings=${CMAKE_CURRENT_SOURCE_DIR}/suppression_mappings.txt"
)
target_link_options(${CMAKE_PROJECT_NAME} PRIVATE
        $<$<NOT:$<CONFIG:Debug>>:-Wl,--gc-sections;-Wl,-s>
        -Wl,--exclude-libs,ALL
        -Wl,--as-needed
)
set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES
        C_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
        POSITION_INDEPENDENT_CODE ON
)

target_link_libraries(${CMAKE_PROJECT_NAME}
        usbgx
        config
        tinyalsa
        android
        log
        z)
