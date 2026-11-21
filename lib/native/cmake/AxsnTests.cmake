include_guard(GLOBAL)

function(axsn_add_gtest_targets)
  cmake_parse_arguments(PARSE_ARGV 0 ARG
        "NO_JNI_LIFECYCLE;NO_GTEST_JNI_BRIDGE"
        "NAME"
        "SOURCES;SUPPORT_SOURCES;JNI_SOURCES;LIBRARIES;INCLUDE_DIRECTORIES;COMPILE_DEFINITIONS"
    )

  if(ARG_UNPARSED_ARGUMENTS)
    message(FATAL_ERROR "axsn_add_gtest_targets: unknown arguments: ${ARG_UNPARSED_ARGUMENTS}")
  endif()
  if(NOT ARG_NAME)
    message(FATAL_ERROR "axsn_add_gtest_targets requires NAME")
  endif()
  if(NOT ARG_SOURCES)
    message(FATAL_ERROR "axsn_add_gtest_targets(${ARG_NAME}) requires SOURCES")
  endif()
  if(NOT AXSN_BUILD_TESTS OR NOT TARGET axsn::gtest)
    message(FATAL_ERROR "GoogleTest support is disabled; configure with -DAXSN_BUILD_TESTS=ON")
  endif()
  if(NOT ARG_NO_GTEST_JNI_BRIDGE AND NOT TARGET axsn::gtest_jni)
    message(FATAL_ERROR "AXSN GTest JNI bridge is unavailable")
  endif()

  set(_objects "${ARG_NAME}_test_objects")
  set(_shared "${ARG_NAME}tests")
  set(_runner "${ARG_NAME}tests_runner")

  foreach(_name IN ITEMS "${_objects}" "${_shared}" "${_runner}")
    if(TARGET "${_name}")
      message(FATAL_ERROR "AXSN test target already exists: ${_name}")
    endif()
  endforeach()

  # Common test cases compile once. These sources must be runnable in the
  # standalone runner; JVM-dependent tests belong in JNI_SOURCES instead.
  add_library("${_objects}" OBJECT
        ${ARG_SOURCES}
        ${ARG_SUPPORT_SOURCES}
    )
  target_link_libraries("${_objects}" PRIVATE
        axsn::gtest
        ${ARG_LIBRARIES}
    )
  if(ARG_INCLUDE_DIRECTORIES)
    target_include_directories("${_objects}" PRIVATE ${ARG_INCLUDE_DIRECTORIES})
  endif()
  if(ARG_COMPILE_DEFINITIONS)
    target_compile_definitions("${_objects}" PRIVATE ${ARG_COMPILE_DEFINITIONS})
  endif()
  axsn_configure_target("${_objects}")

  # Java-loadable test DSO: only this target receives GTest's JNI bridge and
  # (by default) AXSN's JNI_OnLoad/Unload + per-DSO JavaVM implementation.
  add_library("${_shared}" SHARED
        ${ARG_JNI_SOURCES}
        $<TARGET_OBJECTS:${_objects}>
    )
  target_link_libraries("${_shared}" PRIVATE
        ${ARG_LIBRARIES}
        axsn::gtest
    )
  if(NOT ARG_NO_GTEST_JNI_BRIDGE)
    target_link_libraries("${_shared}" PRIVATE axsn::gtest_jni)
  endif()
  if(NOT ARG_NO_JNI_LIFECYCLE)
    target_link_libraries("${_shared}" PRIVATE axsn::jni)
  endif()
  if(ARG_INCLUDE_DIRECTORIES)
    target_include_directories("${_shared}" PRIVATE ${ARG_INCLUDE_DIRECTORIES})
  endif()
  if(ARG_COMPILE_DEFINITIONS)
    target_compile_definitions("${_shared}" PRIVATE ${ARG_COMPILE_DEFINITIONS})
  endif()
  axsn_configure_target("${_shared}")

  # Standalone runner: shared test sources + core GTest implementation only.
  # It receives neither Java bridge functions nor JNI_OnLoad/Unload.
  add_executable("${_runner}"
        $<TARGET_OBJECTS:${_objects}>
    )
  target_link_libraries("${_runner}" PRIVATE
        ${ARG_LIBRARIES}
        axsn::gtest
        GTest::gtest_main
    )
  if(ARG_INCLUDE_DIRECTORIES)
    target_include_directories("${_runner}" PRIVATE ${ARG_INCLUDE_DIRECTORIES})
  endif()
  if(ARG_COMPILE_DEFINITIONS)
    target_compile_definitions("${_runner}" PRIVATE ${ARG_COMPILE_DEFINITIONS})
  endif()
  axsn_configure_target("${_runner}")

  set_property(TARGET "${_shared}" PROPERTY AXSN_TEST_RUNNER "${_runner}")
  set_property(TARGET "${_shared}" PROPERTY AXSN_TEST_OBJECTS "${_objects}")
  message(STATUS "AXSN tests: ${ARG_NAME} -> ${_shared} (JNI shared library) + ${_runner} (standalone)")
endfunction()
