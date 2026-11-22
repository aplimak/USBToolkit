# AXSN targets. This directory is added by axsn_initialize()
# it is not a project root and must not call project().

# Base is deliberately small and always available to ordinary AXSN consumers.

add_library(axsn INTERFACE)

target_include_directories(axsn INTERFACE
    "$<BUILD_INTERFACE:${AXSN_ROOT}/include>"
)
if(ANDROID)
  target_link_libraries(axsn INTERFACE log)
endif()

# JNI is an OBJECT library. Linking axsn::jni to a
# shared library embeds the compiled implementation into that DSO, giving
# every consuming DSO its own VM slot and JNI_OnLoad/Unload.
add_library(axsn_jni OBJECT
    features/jni/src/jni.c
)
add_library(axsn::jni ALIAS axsn_jni)
set_target_properties(axsn_jni PROPERTIES
    EXCLUDE_FROM_ALL TRUE
    POSITION_INDEPENDENT_CODE ON
    C_VISIBILITY_PRESET hidden
)
target_include_directories(axsn_jni PUBLIC
    "${AXSN_ROOT}/features/jni/include"
)
target_link_libraries(axsn_jni PUBLIC axsn)
axsn_configure_target(axsn_jni)

# Load the central registry on every module. Registration is metadata-only;
# each dependency's source tree is added only when requested.
include("${AXSN_ROOT}/third_party/Dependencies.cmake")

# GoogleTest support is optional. It is split into:
#   axsn::gtest       compiled reusable test support + its public header
#   compiled GTest JNI object, linked only into the Java-loadable test .so
# The standalone runner links axsn::gtest, but never receives JNI bridge code
# or axsn::jni unless the caller explicitly asks for it.
if(AXSN_BUILD_TESTS)
  # GoogleTest options belong to AXSN's centrally owned vendor policy.
  set(BUILD_GMOCK OFF CACHE BOOL "Build GoogleMock" FORCE)
  set(INSTALL_GTEST OFF CACHE BOOL "Install GoogleTest" FORCE)
  set(gtest_build_tests OFF CACHE BOOL "Build GoogleTest's own tests" FORCE)
  set(gtest_build_samples OFF CACHE BOOL "Build GoogleTest samples" FORCE)
  set(GTEST_HAS_ABSL OFF CACHE BOOL "Disable external Abseil integration" FORCE)
  axsn_add_registered_dependency(NAME googletest)

  if(NOT TARGET GTest::gtest OR NOT TARGET GTest::gtest_main)
    message(FATAL_ERROR
            "AXSN: the vendored GoogleTest tree must provide
            GTest::gtest and GTest::gtest_main")
  endif()

  foreach(_gtest_target IN ITEMS gtest gtest_main)
    if(TARGET "${_gtest_target}")
      set_target_properties("${_gtest_target}" PROPERTIES
                POSITION_INDEPENDENT_CODE ON
            )
    endif()
  endforeach()

  add_library(axsn_gtest OBJECT
        features/gtest/src/gtest.cpp
    )
  add_library(axsn::gtest ALIAS axsn_gtest)
  set_target_properties(axsn_gtest PROPERTIES
        EXCLUDE_FROM_ALL TRUE
        POSITION_INDEPENDENT_CODE ON
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
    )
  target_include_directories(axsn_gtest PUBLIC
        "${AXSN_ROOT}/features/gtest/include"
    )
  target_link_libraries(axsn_gtest PUBLIC
        axsn
        GTest::gtest
    )
  axsn_configure_target(axsn_gtest)

  # Java-facing GTest entrypoints are a separate compiled object feature.
  # They are deliberately not part of axsn::gtest, because the standalone
  # runner must not contain JNI entrypoints or depend on a JavaVM.
  add_library(axsn_gtest_jni OBJECT
        features/gtest/src/jni.cpp
    )
  add_library(axsn::gtest_jni ALIAS axsn_gtest_jni)
  set_target_properties(axsn_gtest_jni PROPERTIES
        EXCLUDE_FROM_ALL TRUE
        POSITION_INDEPENDENT_CODE ON
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
    )
  # pulls compiled object files
  target_sources(axsn_gtest_jni PUBLIC
        $<TARGET_OBJECTS:axsn_gtest>
        $<TARGET_OBJECTS:axsn_jni>
  )
  # pulls headers and etc
  target_link_libraries(axsn_gtest_jni PUBLIC
        axsn::gtest
        axsn::jni
    )
  axsn_configure_target(axsn_gtest_jni)
else()
  # This makes the intended opt-in behavior obvious at configure time.
  message(STATUS "AXSN: GoogleTest support is disabled (AXSN_BUILD_TESTS=OFF)")
endif()

message(STATUS "AXSN initialized: ${AXSN_ROOT}")
