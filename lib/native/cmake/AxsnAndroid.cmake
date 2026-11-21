include_guard(GLOBAL)

function(axsn_get_android_host_triplet output)
  if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(_triplet aarch64-linux-android)
  elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(_triplet armv7a-linux-androideabi)
  elseif(ANDROID_ABI STREQUAL "x86_64")
    set(_triplet x86_64-linux-android)
  elseif(ANDROID_ABI STREQUAL "x86")
    set(_triplet i686-linux-android)
  else()
    message(FATAL_ERROR "AXSN: unsupported ANDROID_ABI='${ANDROID_ABI}'")
  endif()
  set(${output} "${_triplet}" PARENT_SCOPE)
endfunction()

function(axsn_get_android_external_compilers out_cc out_cxx)
  if(NOT ANDROID)
    message(FATAL_ERROR
          "axsn_get_android_external_compilers
          requires the Android NDK toolchain")
  endif()

  set(_target_args "")
  if(CMAKE_C_COMPILER_TARGET)
    string(APPEND _target_args " --target=${CMAKE_C_COMPILER_TARGET}")
  endif()
  if(CMAKE_SYSROOT)
    string(APPEND _target_args " --sysroot=${CMAKE_SYSROOT}")
  endif()
  set(${out_cc} "${CMAKE_C_COMPILER}${_target_args}" PARENT_SCOPE)
  set(${out_cxx} "${CMAKE_CXX_COMPILER}${_target_args}" PARENT_SCOPE)
endfunction()

# Base compiler flags from CMake/NDK plus AXSN's configuration policy, for
# separately configured Autotools projects. This deliberately does not copy
# first-party warnings or private target flags.
function(axsn_get_external_build_flags out_cflags out_cxxflags out_ldflags)
  string(TOUPPER "${CMAKE_BUILD_TYPE}" _config)
  set(_cflags "${CMAKE_C_FLAGS}")
  set(_cxxflags "${CMAKE_CXX_FLAGS}")
  set(_ldflags "${CMAKE_EXE_LINKER_FLAGS} ${CMAKE_SHARED_LINKER_FLAGS}")

  if(_config)
    foreach(_lang IN ITEMS C CXX)
      set(_var "CMAKE_${_lang}_FLAGS_${_config}")
      if(DEFINED ${_var} AND NOT "${${_var}}" STREQUAL "")
        if(_lang STREQUAL "C")
          string(APPEND _cflags " ${${_var}}")
        else()
          string(APPEND _cxxflags " ${${_var}}")
        endif()
      endif()
    endforeach()
    foreach(_kind IN ITEMS EXE SHARED)
      set(_var "CMAKE_${_kind}_LINKER_FLAGS_${_config}")
      if(DEFINED ${_var} AND NOT "${${_var}}" STREQUAL "")
        string(APPEND _ldflags " ${${_var}}")
      endif()
    endforeach()
  endif()

  # NDK r28+ uses 16 KB max page size by default. For older/unknown NDKs,
  # pass matching linker flags to separately built ELF dependencies.
  if(ANDROID AND AXSN_ENABLE_16K_PAGE_SIZE
       AND (ANDROID_ABI STREQUAL "arm64-v8a" OR ANDROID_ABI STREQUAL "x86_64"))
    set(_ndk_version "${CMAKE_ANDROID_NDK_VERSION}")
    if(NOT _ndk_version AND DEFINED ANDROID_NDK_REVISION)
      set(_ndk_version "${ANDROID_NDK_REVISION}")
    endif()
    if(NOT _ndk_version OR _ndk_version VERSION_LESS "28.0")
      string(APPEND _ldflags
        " -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")
    endif()
  endif()

  string(STRIP "${_cflags}" _cflags)
  string(STRIP "${_cxxflags}" _cxxflags)
  string(STRIP "${_ldflags}" _ldflags)
  set(${out_cflags} "${_cflags}" PARENT_SCOPE)
  set(${out_cxxflags} "${_cxxflags}" PARENT_SCOPE)
  set(${out_ldflags} "${_ldflags}" PARENT_SCOPE)
endfunction()

# Compatibility wrapper for existing modules.
function(axsn_set_external_build_flags)
  axsn_get_android_host_triplet(_triplet)
  axsn_get_external_build_flags(_cflags _cxxflags _ldflags)
  set(TRIPLET "${_triplet}" PARENT_SCOPE)
  set(LIB_CFLAGS "${_cflags}" PARENT_SCOPE)
  set(LIB_CXXFLAGS "${_cxxflags}" PARENT_SCOPE)
  set(LIB_LDFLAGS "${_ldflags}" PARENT_SCOPE)
endfunction()
