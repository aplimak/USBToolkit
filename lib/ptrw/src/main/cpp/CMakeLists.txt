cmake_minimum_required(VERSION 3.22.1)
project("ptrw" LANGUAGES C CXX)

include("${CMAKE_CURRENT_LIST_DIR}/common/common.cmake")
axsn_initialize()

add_library(${PROJECT_NAME} SHARED
        ptrw.c
        ptrw_jni.c
        src/PointerWrapper.cpp
        src/JniCallable.cpp
        src/native_register.cpp
)

target_include_directories(${PROJECT_NAME} PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_link_libraries(${PROJECT_NAME}
        axsn
        axsn::jni)

axsn_configure_target(${PROJECT_NAME})

if(AXSN_BUILD_TESTS)
  axsn_add_gtest_targets(
    NAME "${PROJECT_NAME}"
    SOURCES
      test/PointerWrapper.cpp
    JNI_SOURCES
          test/JniCallable.cpp
      test/tests_jni.c
    LIBRARIES
      ${PROJECT_NAME}
    )
endif()
