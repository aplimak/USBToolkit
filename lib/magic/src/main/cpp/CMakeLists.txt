cmake_minimum_required(VERSION 3.22.1)
project("magic" LANGUAGES C CXX)

include("${CMAKE_CURRENT_LIST_DIR}/common/common.cmake")
axsn_initialize()

if(NOT ANDROID)
  message(FATAL_ERROR
      "This module is expected to be configured with the Android NDK toolchain")
endif()

include(ExternalProject)
include(ProcessorCount)
find_program(MAGIC_MAKE_EXECUTABLE NAMES make gmake REQUIRED)

axsn_get_android_host_triplet(MAGIC_HOST_TRIPLET)
axsn_get_android_external_compilers(MAGIC_CC MAGIC_CXX)
axsn_get_external_build_flags(MAGIC_CFLAGS MAGIC_CXXFLAGS MAGIC_LDFLAGS)

# Build libmagic

set(MAGIC_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party/file")
set(MAGIC_PREFIX     "${CMAKE_CURRENT_BINARY_DIR}/deps/libmagic")
set(MAGIC_BUILD_DIR  "${CMAKE_CURRENT_BINARY_DIR}/_deps/libmagic")

ProcessorCount(MAGIC_JOBS)
if(NOT MAGIC_JOBS OR MAGIC_JOBS LESS 1)
  set(MAGIC_JOBS 1)
endif()

ExternalProject_Add(libmagic_build
    SOURCE_DIR "${MAGIC_SOURCE_DIR}"
    BINARY_DIR "${MAGIC_BUILD_DIR}"
    PREFIX "${CMAKE_CURRENT_BINARY_DIR}/_external/libmagic"
    CONFIGURE_COMMAND
        "${CMAKE_COMMAND}" -E env
        "CC=${MAGIC_CC}"
        "CXX=${MAGIC_CXX}"
        "AR=${CMAKE_AR}"
        "RANLIB=${CMAKE_RANLIB}"
        "STRIP=${CMAKE_STRIP}"
        "PKG_CONFIG_PATH="
        "CFLAGS=${MAGIC_CFLAGS} -fPIC"
        "CXXFLAGS=${MAGIC_CXXFLAGS} -fPIC"
        "LDFLAGS=${MAGIC_LDFLAGS}"
        "${MAGIC_SOURCE_DIR}/configure"
        "--host=${MAGIC_HOST_TRIPLET}"
        "--prefix=${MAGIC_PREFIX}"
        "--with-sysroot=${CMAKE_SYSROOT}"
        --enable-static
        --disable-shared
        --enable-zlib
        --with-pic
    BUILD_COMMAND "${MAGIC_MAKE_EXECUTABLE}" "-j${MAGIC_JOBS}"
        "SUBDIRS=src magic"
    INSTALL_COMMAND "${MAGIC_MAKE_EXECUTABLE}" "SUBDIRS=src magic" install
    BUILD_BYPRODUCTS "${MAGIC_PREFIX}/lib/libmagic.a"
    LOG_CONFIGURE ON
    LOG_BUILD ON
    LOG_INSTALL ON
)

file(MAKE_DIRECTORY "${MAGIC_PREFIX}/include" "${MAGIC_PREFIX}/lib")
add_library(magic_magiclib STATIC IMPORTED GLOBAL)
add_dependencies(magic_magiclib libmagic_build)
target_link_libraries(magic_magiclib INTERFACE z)
set_target_properties(magic_magiclib PROPERTIES
    IMPORTED_LOCATION "${MAGIC_PREFIX}/lib/libmagic.a"
    INTERFACE_INCLUDE_DIRECTORIES "${MAGIC_PREFIX}/include"
)

# Magic db
if(NOT DEFINED MAGIC_ASSET_OUTPUT_DIR)
  message(FATAL_ERROR "MAGIC_ASSET_OUTPUT_DIR must be passed from Gradle")
endif()

file(MAKE_DIRECTORY "${MAGIC_ASSET_OUTPUT_DIR}")
add_custom_command(
        TARGET libmagic_build POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        "${MAGIC_PREFIX}/share/misc/magic.mgc"
        "${MAGIC_ASSET_OUTPUT_DIR}/magic.mgc"
        COMMENT "Staging magic.mgc as an Android asset"
        VERBATIM
)

# Main Library
add_library(${PROJECT_NAME} SHARED magic_jni.c)

target_link_libraries(${PROJECT_NAME}
        axsn
        magic_magiclib)

find_package(ptrw REQUIRED CONFIG)
target_link_libraries(${PROJECT_NAME} ptrw::ptrw)

axsn_configure_target(${PROJECT_NAME})
