cmake_minimum_required(VERSION 3.22.1)
project("magic" LANGUAGES C CXX)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

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

        BUILD_COMMAND   make -j8 "SUBDIRS=src magic"
        INSTALL_COMMAND make "SUBDIRS=src magic" install

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

# Magic db

if(NOT DEFINED MAGIC_ASSET_OUTPUT_DIR)
    message(FATAL_ERROR "MAGIC_ASSET_OUTPUT_DIR must be passed from Gradle")
endif()
set(MAGIC_ASSET_STAGE "${MAGIC_ASSET_OUTPUT_DIR}")
file(MAKE_DIRECTORY "${MAGIC_ASSET_STAGE}")

# Main Library

add_library(${CMAKE_PROJECT_NAME} SHARED
        magic_jni.c
)

# Copy magic db
add_custom_command(
        TARGET ${CMAKE_PROJECT_NAME} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        "${MAGIC_PREFIX}/share/misc/magic.mgc"
        "${MAGIC_ASSET_STAGE}/magic.mgc"
        COMMENT "Staging magic.mgc as an Android asset"
        VERBATIM
)

axsn_apply_warnings(${CMAKE_PROJECT_NAME})
axsn_apply_build_flags(${CMAKE_PROJECT_NAME})

target_link_libraries(${CMAKE_PROJECT_NAME}
        axsn_common
        magiclib
        log
        z)

find_package(ptrw REQUIRED CONFIG)
target_link_libraries(${CMAKE_PROJECT_NAME} ptrw::ptrw)
