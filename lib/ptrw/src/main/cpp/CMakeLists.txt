cmake_minimum_required(VERSION 3.22.1)
project("ptrw" LANGUAGES C)
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

# Shared library
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/common
        ${CMAKE_CURRENT_BINARY_DIR}/build/axsn_common)

# Main Library

add_library(${CMAKE_PROJECT_NAME} SHARED
        core.c
        ptrw.c
        ptrw_jni.c
)

target_include_directories(${CMAKE_PROJECT_NAME} PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

axsn_apply_warnings(${CMAKE_PROJECT_NAME})
axsn_apply_build_flags(${CMAKE_PROJECT_NAME})

target_link_libraries(${CMAKE_PROJECT_NAME}
        axsn_common
        android
        log)
