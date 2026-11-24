# AXSN targets. This directory is added by axsn_initialize()

add_library(axsn OBJECT
        src/error.c)
set_target_properties(axsn PROPERTIES
        EXCLUDE_FROM_ALL TRUE
        POSITION_INDEPENDENT_CODE ON
        C_VISIBILITY_PRESET hidden
)
target_include_directories(axsn PUBLIC include)
target_link_libraries(axsn PUBLIC log)
axsn_configure_target(axsn)

add_subdirectory(features/jni)

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

  add_subdirectory(features/gtest)
  add_subdirectory(features/gtest_jni)

else()
  # This makes the intended opt-in behavior obvious at configure time.
  message(STATUS "AXSN: GoogleTest support is disabled (AXSN_BUILD_TESTS=OFF)")
endif()

message(STATUS "AXSN initialized: ${AXSN_ROOT}")
