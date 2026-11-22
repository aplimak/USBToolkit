# AXSN public CMake entry point.
# Include after project() and before creating first-party targets.
include_guard(GLOBAL)

get_filename_component(AXSN_ROOT "${CMAKE_CURRENT_LIST_DIR}" REALPATH)

if(NOT EXISTS "${AXSN_ROOT}/CMakeLists.txt")
  message(FATAL_ERROR "AXSN_ROOT is invalid: ${AXSN_ROOT}")
endif()

# Project language defaults. Respect values deliberately chosen by the module.
if(NOT DEFINED CMAKE_C_STANDARD)
  set(CMAKE_C_STANDARD 11)
endif()
if(NOT DEFINED CMAKE_C_STANDARD_REQUIRED)
  set(CMAKE_C_STANDARD_REQUIRED ON)
endif()
if(NOT DEFINED CMAKE_C_EXTENSIONS)
  set(CMAKE_C_EXTENSIONS OFF)
endif()
if(NOT DEFINED CMAKE_CXX_STANDARD)
  set(CMAKE_CXX_STANDARD 17)
endif()
if(NOT DEFINED CMAKE_CXX_STANDARD_REQUIRED)
  set(CMAKE_CXX_STANDARD_REQUIRED ON)
endif()
if(NOT DEFINED CMAKE_CXX_EXTENSIONS)
  set(CMAKE_CXX_EXTENSIONS OFF)
endif()

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

option(AXSN_ENABLE_WARNINGS
  "Enable warnings on AXSN/first-party targets"
  "${_AXSN_WARNINGS_DEFAULT}")
option(AXSN_WARNINGS_AS_ERRORS
  "Treat first-party warnings as errors"
  "${_AXSN_WERROR_DEFAULT}")

set(AXSN_RELEASE_OPTIMIZATION
  "2" CACHE STRING
  "AXSN Release optimization level: 0, 1, 2, 3, s, z")
set_property(CACHE AXSN_RELEASE_OPTIMIZATION PROPERTY
  STRINGS 0 1 2 3 s z)
if(NOT AXSN_RELEASE_OPTIMIZATION MATCHES "^(0|1|2|3|s|z)$")
  message(FATAL_ERROR
    "Invalid AXSN_RELEASE_OPTIMIZATION='${AXSN_RELEASE_OPTIMIZATION}'")
endif()

option(AXSN_ENABLE_SECTION_GC
  "Use function/data sections and linker garbage collection on Android"
  ON)
option(AXSN_ENABLE_BUILD_ID
  "Emit SHA-1 ELF build IDs"
  ON)
option(AXSN_ENABLE_ELF_NOTE
  "Embed a .note.axsn identity note"
  ON)
option(AXSN_ENABLE_16K_PAGE_SIZE
  "Use 16 KB ELF alignment with NDKs older than r28"
  ON)
option(AXSN_ENABLE_PREFIX_MAP
  "Map source-root paths in debug information"
  ON)
option(AXSN_GENERATE_LINK_MAP
  "Generate linker map files"
  OFF)
option(AXSN_GENERATE_SIZE_TARGETS
  "Add per-target llvm-size targets"
  ON)
option(AXSN_ENABLE_CLANG_TIDY
  "Enable clang-tidy on configured targets"
  OFF)
option(AXSN_ENABLE_TIME_TRACE
  "Enable Clang -ftime-trace"
  OFF)
option(AXSN_USE_CCACHE
  "Use ccache as compiler launcher when available"
  OFF)
option(AXSN_ENABLE_ASAN
  "Enable AddressSanitizer"
  OFF)
option(AXSN_ENABLE_UBSAN
  "Enable UndefinedBehaviorSanitizer"
  OFF)

if(CMAKE_BUILD_TYPE STREQUAL "Debug")
  set(_AXSN_TESTS_DEFAULT ON)
else()
  set(_AXSN_TESTS_DEFAULT OFF)
endif()
option(AXSN_BUILD_TESTS
  "Build AXSN GoogleTest support and native test targets"
  "${_AXSN_TESTS_DEFAULT}")
set(AXSN_SUPPRESSION_FILE
  "${AXSN_ROOT}/suppression_mappings.txt" CACHE FILEPATH
  "Optional Clang warning-suppression mappings file")

include("${AXSN_ROOT}/cmake/AxsnWarnings.cmake")
include("${AXSN_ROOT}/cmake/AxsnTargets.cmake")
include("${AXSN_ROOT}/cmake/AxsnDependencies.cmake")
include("${AXSN_ROOT}/cmake/AxsnAndroid.cmake")
include("${AXSN_ROOT}/cmake/AxsnElfIdentity.cmake")
include("${AXSN_ROOT}/cmake/AxsnTests.cmake")

# Add AXSN's targets to this project.
# Call once in each CMake root,
# after project() and before target_link_libraries().
function(axsn_initialize)
  get_property(_axsn_initialized GLOBAL PROPERTY AXSN_INITIALIZED)
  if(_axsn_initialized)
    return()
  endif()
  add_subdirectory("${AXSN_ROOT}" "${CMAKE_BINARY_DIR}/_axsn")
  set_property(GLOBAL PROPERTY AXSN_INITIALIZED TRUE)
endfunction()
