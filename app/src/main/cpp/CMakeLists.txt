cmake_minimum_required(VERSION 3.22.1)
project("usbtoolkit")

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

# Build libmagic

set(MAGIC_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party/file")
set(MAGIC_PREFIX     "${CMAKE_CURRENT_BINARY_DIR}/deps/libmagic")
set(MAGIC_BUILD_DIR  "${CMAKE_CURRENT_BINARY_DIR}/build/libmagic")

ExternalProject_Add(libmagic_build
        SOURCE_DIR "${MAGIC_SOURCE_DIR}"
        BINARY_DIR "${MAGIC_BUILD_DIR}"
        PREFIX     "${MAGIC_PREFIX}"

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
        <SOURCE_DIR>/configure
        --host=${TRIPLET}
        "--prefix=${MAGIC_PREFIX}"
        "--with-sysroot=${CMAKE_SYSROOT}"
        --enable-static
        --disable-shared
        --enable-zlib
        --enable-landlock
        --with-pic

        BUILD_COMMAND   make -j8 SUBDIRS=src
        INSTALL_COMMAND make SUBDIRS=src install

        BUILD_BYPRODUCTS "${MAGIC_PREFIX}/lib/libmagic.a"

        LOG_CONFIGURE 1
        LOG_BUILD 1
        LOG_INSTALL 1
)

file(MAKE_DIRECTORY "${MAGIC_PREFIX}/include")
file(MAKE_DIRECTORY "${MAGIC_PREFIX}/lib")
add_library(magic STATIC IMPORTED GLOBAL)
add_dependencies(magic libmagic_build)
set_target_properties(magic PROPERTIES
        IMPORTED_LOCATION "${MAGIC_PREFIX}/lib/libmagic.a"
        INTERFACE_INCLUDE_DIRECTORIES "${MAGIC_PREFIX}/include"
)

# Compile magic files

set(MAGIC_MGDIR   "${MAGIC_SOURCE_DIR}/magic/Magdir")
set(MAGIC_MGC_OUT "${CMAKE_CURRENT_SOURCE_DIR}/../assets/magic.mgc")

# The host's `file` binary — required. Install it if missing.
find_program(HOST_FILE_EXECUTABLE file)
if(NOT HOST_FILE_EXECUTABLE)
    message(FATAL_ERROR
            "Host `file` binary not found. Install it: "
            "`apt install file` / `brew install file` / `pacman -S file`")
endif()

if(NOT EXISTS "${MAGIC_MGC_OUT}" OR
        "${MAGIC_MGDIR}" IS_NEWER_THAN "${MAGIC_MGC_OUT}")

    message(STATUS "Compiling magic.mgc from ${MAGIC_MGDIR}")

    # Work in the build tree so we never touch the vendored source
    set(_mgc_work "${CMAKE_CURRENT_BINARY_DIR}/mgc")

    execute_process(
            COMMAND ${CMAKE_COMMAND} -E make_directory "${_mgc_work}"
            RESULT_VARIABLE _rc)
    if(NOT _rc EQUAL 0)
        message(FATAL_ERROR "mkdir ${_mgc_work} failed")
    endif()

    execute_process(
            COMMAND ${CMAKE_COMMAND} -E copy_directory
            "${MAGIC_MGDIR}" "${_mgc_work}/Magdir"
            RESULT_VARIABLE _rc)
    if(NOT _rc EQUAL 0)
        message(FATAL_ERROR "copy Magdir failed")
    endif()

    # `file -C -m <dir>` writes <dir>.mgc next to it
    execute_process(
            COMMAND "${HOST_FILE_EXECUTABLE}" -C -m "${_mgc_work}/Magdir"
            WORKING_DIRECTORY "${_mgc_work}"
            RESULT_VARIABLE _rc
            OUTPUT_VARIABLE _out
            ERROR_VARIABLE  _err)
    if(NOT _rc EQUAL 0)
        message(FATAL_ERROR "file -C failed: ${_err}")
    endif()

    execute_process(
            COMMAND ${CMAKE_COMMAND} -E copy
            "${_mgc_work}/Magdir.mgc" "${MAGIC_MGC_OUT}"
            RESULT_VARIABLE _rc)
    if(NOT _rc EQUAL 0)
        message(FATAL_ERROR "copy Magdir.mgc to assets failed")
    endif()

    message(STATUS "Wrote ${MAGIC_MGC_OUT}")
endif()

# TinyALSA

set(TINYALSA_BUILD_EXAMPLES OFF CACHE BOOL "")
set(TINYALSA_BUILD_UTILS OFF CACHE BOOL "")

add_subdirectory(third_party/tinyalsa "${CMAKE_CURRENT_BINARY_DIR}/build/tinyalsa")

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
        magic.c
        dto.c)

target_compile_options(${CMAKE_PROJECT_NAME} PRIVATE
        $<$<NOT:$<CONFIG:Debug>>:-O3;-flto;-ffunction-sections;-fdata-sections>
        $<$<CONFIG:Debug>:-O0;-g3;-fno-omit-frame-pointer;-fno-lto>
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
    magic
    tinyalsa
    config
    usbgx
    android
    log
    z)
