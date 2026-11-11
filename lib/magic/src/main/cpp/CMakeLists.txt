cmake_minimum_required(VERSION 3.22.1)
project("magic")
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

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
        "CFLAGS=${LIB_CFLAGS}"
        "LDFLAGS=${LIB_LDFLAGS}"
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
add_library(magiclib STATIC IMPORTED GLOBAL)
add_dependencies(magiclib libmagic_build)
set_target_properties(magiclib PROPERTIES
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

# Main Library

add_library(${CMAKE_PROJECT_NAME} SHARED
        magic_jni.c
)

axsn_apply_warnings(${CMAKE_PROJECT_NAME})
axsn_apply_build_flags(${CMAKE_PROJECT_NAME})

target_link_libraries(${CMAKE_PROJECT_NAME}
        axsn_common
        magiclib
        android
        log
        z)
