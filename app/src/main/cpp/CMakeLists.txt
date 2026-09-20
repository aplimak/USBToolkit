
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("usbtoolkit")

# This list is passed to every CMake-based ExternalProject
set(ANDROID_CMAKE_ARGS
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM}
        -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
        -DANDROID_STL=${ANDROID_STL}
)

include(ExternalProject)

# NDK toolchain root (works for Linux/macOS; adjust for Windows)
if(CMAKE_HOST_WIN32)
    set(NDK_TOOLCHAIN "${CMAKE_ANDROID_NDK}/toolchains/llvm/prebuilt/windows-x86_64")
elseif(CMAKE_HOST_APPLE)
    set(NDK_TOOLCHAIN "${CMAKE_ANDROID_NDK}/toolchains/llvm/prebuilt/darwin-x86_64")
else()
    set(NDK_TOOLCHAIN "${CMAKE_ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64")
endif()

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

# API level without the "android-" prefix
string(REPLACE "android-" "" API_LEVEL "${ANDROID_PLATFORM}")

# Cross-compiler paths
set(CC  "${NDK_TOOLCHAIN}/bin/${TRIPLET}${API_LEVEL}-clang")
set(CXX "${NDK_TOOLCHAIN}/bin/${TRIPLET}${API_LEVEL}-clang++")
set(AR  "${NDK_TOOLCHAIN}/bin/llvm-ar")
set(RANLIB "${NDK_TOOLCHAIN}/bin/llvm-ranlib")
set(STRIP "${NDK_TOOLCHAIN}/bin/llvm-strip")

# Sysroot for include/lib paths
set(SYSROOT "${NDK_TOOLCHAIN}/sysroot")

# Build bzip2 (Not needed but kept for template for other make targets)

#set(BZIP2_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party/bzip2")
#set(BZIP2_PREFIX     "${CMAKE_CURRENT_BINARY_DIR}/deps/bzip2")
#set(BZIP2_BUILD_DIR  "${CMAKE_CURRENT_BINARY_DIR}/build/libbz2")
#file(MAKE_DIRECTORY "${BZIP2_BUILD_DIR}")
#
#ExternalProject_Add(bzip2_build
#        SOURCE_DIR "${BZIP2_BUILD_DIR}"
#        PREFIX     "${BZIP2_PREFIX}"
#
#        DOWNLOAD_COMMAND
#        ${CMAKE_COMMAND} -E copy_directory
#        "${BZIP2_SOURCE_DIR}" "${BZIP2_BUILD_DIR}"
#        CONFIGURE_COMMAND ""
#        BUILD_COMMAND
#        ${CMAKE_COMMAND} -E env
#        make -j8
#        "CC=${CC}"
#        "AR=${AR}"
#        "RANLIB=${RANLIB}"
#        "CFLAGS=--sysroot=${SYSROOT} -fPIC -O2 -Wall -D_FILE_OFFSET_BITS=64"
#        "LDFLAGS=--sysroot=${SYSROOT}"
#        "PREFIX=${BZIP2_PREFIX}"
#        libbz2.a
#
#        INSTALL_COMMAND
#        ${CMAKE_COMMAND} -E env
#        make install
#        "PREFIX=${BZIP2_PREFIX}"
#
#        BUILD_IN_SOURCE 1
#
#        BUILD_BYPRODUCTS "${BZIP2_PREFIX}/lib/libbz2.a"
#
#        LOG_BUILD 1
#        LOG_INSTALL 1
#)
#
#file(MAKE_DIRECTORY "${BZIP2_PREFIX}/include")
#file(MAKE_DIRECTORY "${BZIP2_PREFIX}/lib")
#add_library(bzip2 STATIC IMPORTED GLOBAL)
#add_dependencies(bzip2 bzip2_build)
#set_target_properties(bzip2 PROPERTIES
#        IMPORTED_LOCATION "${BZIP2_PREFIX}/lib/libbz2.a"
#        INTERFACE_INCLUDE_DIRECTORIES "${BZIP2_PREFIX}/include"
#)

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
        "CC=${CC}"
        "CXX=${CXX}"
        "AR=${AR}"
        "RANLIB=${RANLIB}"
        "STRIP=${STRIP}"
        "PKG_CONFIG_PATH="
        <SOURCE_DIR>/configure
        --host=${TRIPLET}
        "--prefix=${MAGIC_PREFIX}"
        "--with-sysroot=${SYSROOT}"
        --enable-static
        --disable-shared
        --enable-zlib
        --enable-landlock
        --with-pic

        BUILD_COMMAND   make -j8
        INSTALL_COMMAND make install

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

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(${CMAKE_PROJECT_NAME} SHARED
    # List C/C++ source files with relative paths to this CMakeLists.txt.
    magic.c)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
    # List libraries link to the target library
    magic
    android
    log
    z)
