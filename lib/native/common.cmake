if(DEFINED AXSN_COMMON_CMAKE_INCLUDED)
  return()
endif()
set(AXSN_COMMON_CMAKE_INCLUDED TRUE)

set(CFLAGS_DEBUG   -O0 -g3 -fno-omit-frame-pointer -fno-lto)
set(CFLAGS_RELEASE -O3 -flto -ffunction-sections -fdata-sections
                       -fvisibility=hidden -DNDEBUG)
set(LDFLAGS_DEBUG   "")                 # intentionally empty
set(LDFLAGS_RELEASE -Wl,--gc-sections -Wl,-s)

function(axsn_set_external_build_flags)
  if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(TRIPLET "aarch64-linux-android" PARENT_SCOPE)
  elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(TRIPLET "armv7a-linux-androideabi" PARENT_SCOPE)
  elseif(ANDROID_ABI STREQUAL "x86_64")
    set(TRIPLET "x86_64-linux-android" PARENT_SCOPE)
  elseif(ANDROID_ABI STREQUAL "x86")
    set(TRIPLET "i686-linux-android" PARENT_SCOPE)
  else()
    message(FATAL_ERROR "axsn_set_triplet: unsupported ANDROID_ABI='${ANDROID_ABI}'")
  endif()

  if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(_c "${CFLAGS_DEBUG}")
    set(_l "${LDFLAGS_DEBUG}")
  else()
    set(_c "${CFLAGS_RELEASE}")
    set(_l "${LDFLAGS_RELEASE}")
  endif()
  list(JOIN _c " " _c_str)
  list(JOIN _l " " _l_str)
  set(LIB_CFLAGS  "${_c_str}" PARENT_SCOPE)
  set(LIB_LDFLAGS "${_l_str}" PARENT_SCOPE)
endfunction()

function(axsn_apply_warnings target)
  if(NOT AXSN_STRICT)
    return()
  endif()
  target_compile_options(${target} PRIVATE
        -Wall -Wextra -Wpedantic -Wshadow -Wpointer-arith -Wcast-qual
        -Wcast-align -Wstrict-prototypes -Wmissing-prototypes
        -Wmissing-declarations -Wold-style-definition -Wredundant-decls
        -Wformat=2 -Wformat-security -Wnull-dereference -Wdouble-promotion
        -Wfloat-equal -Wwrite-strings -Wvla -Wswitch-enum -Wundef
        -Winit-self -Wuninitialized -Wno-unused-parameter
        -Werror
    )
endfunction()

function(axsn_apply_build_flags target)
  if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(${target} PRIVATE ${CFLAGS_DEBUG})
    target_link_options(${target}    PRIVATE ${LDFLAGS_DEBUG})
  else()
    target_compile_options(${target} PRIVATE ${CFLAGS_RELEASE})
    target_link_options(${target}    PRIVATE ${LDFLAGS_RELEASE})
  endif()

  if(NOT DEFINED AXSN_SUPPRESSION_FILE)
    set(AXSN_SUPPRESSION_FILE
              "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/suppression_mappings.txt")
  endif()
  if(EXISTS "${AXSN_SUPPRESSION_FILE}")
    target_compile_options(${target} PRIVATE
              "--warning-suppression-mappings=${AXSN_SUPPRESSION_FILE}")
  endif()

  target_link_options(${target} PRIVATE
        -Wl,--exclude-libs,ALL
        -Wl,--as-needed
    )

  set_target_properties(${target} PROPERTIES
        C_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
        POSITION_INDEPENDENT_CODE ON
    )
endfunction()
