cmake_minimum_required(VERSION 3.22.1)
project("usbtk" LANGUAGES C CXX)

if (NOT DEFINED AXSN_ROOT)
  message(FATAL_ERROR "AXSN_ROOT is required")
endif ()
if (NOT EXISTS "${AXSN_ROOT}/common.cmake")
  message(FATAL_ERROR "AXSN_ROOT is invalid: ${AXSN_ROOT}")
endif ()

include("${AXSN_ROOT}/common.cmake")
axsn_initialize()

set(BUILD_SHARED_LIBS OFF CACHE BOOL "")

# set(TINYALSA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
# set(TINYALSA_BUILD_UTILS OFF CACHE BOOL "" FORCE)
# add_subdirectory(third_party/tinyalsa
#   "${CMAKE_CURRENT_BINARY_DIR}/_deps/tinyalsa")

set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_TESTS OFF CACHE BOOL "" FORCE)
add_subdirectory(third_party/libconfig
  "${CMAKE_CURRENT_BINARY_DIR}/_deps/libconfig")

set(USBGX_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/third_party/libusbgx")
add_subdirectory(cmake/libusbgx
  "${CMAKE_CURRENT_BINARY_DIR}/_deps/libusbgx")

# Main Library
add_library(${PROJECT_NAME} SHARED
        dto.c
        usb_jni.c
        src/dto.cpp
)

target_include_directories(${PROJECT_NAME} PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_link_libraries(${PROJECT_NAME}
        axsn
        axsn::jni
        #tinyalsa
        usbgx)

find_package(ptrw REQUIRED CONFIG)
target_link_libraries(${PROJECT_NAME} ptrw::ptrw)

axsn_configure_target(${PROJECT_NAME})

if(AXSN_BUILD_TESTS)
  axsn_add_gtest_targets(
    NAME "${PROJECT_NAME}"
    SOURCES
      test/Configfs.cpp
    JNI_SOURCES
      test/tests_jni.c
    LIBRARIES
      ${PROJECT_NAME}
    )
endif()
