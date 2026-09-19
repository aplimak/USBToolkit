
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

# Build bzip2

set(BZIP2_VERSION "1.0.8")
set(BZIP2_URL     "https://sourceware.org/pub/bzip2/bzip2-${BZIP2_VERSION}.tar.gz")
set(BZIP2_SHA256  "ab5a03176ee106d3f0fa90e381da478ddae405918153cca248e682cd0c4a2269")

set(BZIP2_PREFIX  "${CMAKE_CURRENT_BINARY_DIR}/deps/bzip2")

ExternalProject_Add(bzip2_build
        URL            "${BZIP2_URL}"
        URL_HASH       "SHA256=${BZIP2_SHA256}"
        DOWNLOAD_DIR   "${CMAKE_CURRENT_BINARY_DIR}/downloads"

        PREFIX         "${BZIP2_PREFIX}"

        CONFIGURE_COMMAND ""
        BUILD_IN_SOURCE 1
        BUILD_COMMAND
        ${CMAKE_COMMAND} -E env
        "CC=${CC}"
        "AR=${AR}"
        "RANLIB=${RANLIB}"
        make -j8
        "CC=${CC}"
        "AR=${AR}"
        "RANLIB=${RANLIB}"
        "CFLAGS=--sysroot=${SYSROOT} -fPIC -O2 -Wall -D_FILE_OFFSET_BITS=64"
        "LDFLAGS=--sysroot=${SYSROOT}"
        "PREFIX=${BZIP2_PREFIX}"
        libbz2.a

        INSTALL_COMMAND
        ${CMAKE_COMMAND} -E env
        "CC=${CC}"
        "AR=${AR}"
        "RANLIB=${RANLIB}"
        make install
        "CC=${CC}"
        "AR=${AR}"
        "RANLIB=${RANLIB}"
        "CFLAGS=--sysroot=${SYSROOT} -fPIC -O2 -Wall -D_FILE_OFFSET_BITS=64"
        "LDFLAGS=--sysroot=${SYSROOT}"
        "PREFIX=${BZIP2_PREFIX}"

        BUILD_BYPRODUCTS "${BZIP2_PREFIX}/lib/libbz2.a"

        LOG_BUILD 1
        LOG_INSTALL 1
)

file(MAKE_DIRECTORY "${BZIP2_PREFIX}/include")
file(MAKE_DIRECTORY "${BZIP2_PREFIX}/lib")
add_library(bzip2 STATIC IMPORTED GLOBAL)
add_dependencies(bzip2 bzip2_build)
set_target_properties(bzip2 PROPERTIES
        IMPORTED_LOCATION "${BZIP2_PREFIX}/lib/libbz2.a"
        INTERFACE_INCLUDE_DIRECTORIES "${BZIP2_PREFIX}/include"
)

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
        "CFLAGS=--sysroot=${SYSROOT} -fPIC -I${BZIP2_PREFIX}/include"
        "LDFLAGS=--sysroot=${SYSROOT} -L${BZIP2_PREFIX}/lib"
        "PKG_CONFIG_PATH=${BZIP2_PREFIX}/lib/pkgconfig"
        <SOURCE_DIR>/configure
        --host=${TRIPLET}
        --prefix=${MAGIC_PREFIX}
        --enable-shared
        --disable-static
        --enable-zlib
        --enable-bzlib

        BUILD_COMMAND   make -j8
        INSTALL_COMMAND make install

        DEPENDS bzip2_build

        BUILD_BYPRODUCTS "${MAGIC_PREFIX}/lib/libmagic.so"

        LOG_CONFIGURE 1
        LOG_BUILD 1
        LOG_INSTALL 1
)

file(MAKE_DIRECTORY "${MAGIC_PREFIX}/include")
file(MAKE_DIRECTORY "${MAGIC_PREFIX}/lib")
add_library(magic SHARED IMPORTED GLOBAL)
add_dependencies(magic libmagic_build)
set_target_properties(magic PROPERTIES
        IMPORTED_LOCATION "${MAGIC_PREFIX}/lib/libmagic.so"
        INTERFACE_INCLUDE_DIRECTORIES "${MAGIC_PREFIX}/include"
)

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
    usbtoolkit.cpp)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
    # List libraries link to the target library
        magic
    android
    log)
