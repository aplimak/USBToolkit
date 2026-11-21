include_guard(GLOBAL)

set(_axsn_ipo_langs)
if(CMAKE_C_COMPILER_LOADED AND CMAKE_C_COMPILER_ID MATCHES "Clang|GNU")
  list(APPEND _axsn_ipo_langs C)
endif()
if(CMAKE_CXX_COMPILER_LOADED AND CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
  list(APPEND _axsn_ipo_langs CXX)
endif()

if(AXSN_USE_CCACHE)
  find_program(AXSN_CCACHE_EXECUTABLE NAMES ccache)
  if(AXSN_CCACHE_EXECUTABLE)
    if(NOT CMAKE_C_COMPILER_LAUNCHER)
      set(CMAKE_C_COMPILER_LAUNCHER "${AXSN_CCACHE_EXECUTABLE}")
    endif()
    if(NOT CMAKE_CXX_COMPILER_LAUNCHER)
      set(CMAKE_CXX_COMPILER_LAUNCHER "${AXSN_CCACHE_EXECUTABLE}")
    endif()
  else()
    message(WARNING "AXSN_USE_CCACHE is ON but ccache was not found")
  endif()
endif()

if(AXSN_ENABLE_CLANG_TIDY)
  find_program(AXSN_CLANG_TIDY_EXECUTABLE NAMES clang-tidy)
  if(NOT AXSN_CLANG_TIDY_EXECUTABLE)
    message(WARNING "AXSN_ENABLE_CLANG_TIDY is ON but clang-tidy was not found")
  endif()
endif()

function(_axsn_apply_compile_policy target)
  get_target_property(_type "${target}" TYPE)
  if(_type STREQUAL "INTERFACE_LIBRARY")
    message(FATAL_ERROR "AXSN cannot configure INTERFACE target '${target}' as a compiled target")
  endif()

  set_target_properties("${target}" PROPERTIES
        POSITION_INDEPENDENT_CODE ON
        C_VISIBILITY_PRESET hidden
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
    )

  if(ANDROID AND AXSN_ENABLE_SECTION_GC)
    target_compile_options("${target}" PRIVATE -ffunction-sections -fdata-sections)
  endif()

  target_compile_options("${target}" PRIVATE
        "$<$<CONFIG:Release>:-O${AXSN_RELEASE_OPTIMIZATION}>")

  if(ANDROID)
    target_compile_options("${target}" PRIVATE
            "$<$<OR:$<CONFIG:Debug>,$<CONFIG:RelWithDebInfo>>:-fno-omit-frame-pointer>")
  endif()

  if(AXSN_ENABLE_TIME_TRACE)
    target_compile_options("${target}" PRIVATE
            "$<$<COMPILE_LANG_AND_ID:C,Clang>:-ftime-trace>"
            "$<$<COMPILE_LANG_AND_ID:CXX,Clang>:-ftime-trace>")
  endif()

  if(AXSN_ENABLE_PREFIX_MAP AND CMAKE_C_COMPILER_ID MATCHES "Clang|GNU")
    target_compile_options("${target}" PRIVATE
            "-ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")
  endif()

  if(AXSN_ENABLE_ASAN OR AXSN_ENABLE_UBSAN)
    set(_sanitizers)
    if(AXSN_ENABLE_ASAN)
      list(APPEND _sanitizers address)
    endif()
    if(AXSN_ENABLE_UBSAN)
      list(APPEND _sanitizers undefined)
    endif()
    list(JOIN _sanitizers "," _sanitizer_list)
    target_compile_options("${target}" PRIVATE
            "-fsanitize=${_sanitizer_list}" -fno-omit-frame-pointer)
  endif()

  if(AXSN_ENABLE_CLANG_TIDY AND AXSN_CLANG_TIDY_EXECUTABLE)
    if(CMAKE_C_COMPILER_ID MATCHES "Clang")
      set_property(TARGET "${target}" PROPERTY C_CLANG_TIDY "${AXSN_CLANG_TIDY_EXECUTABLE}")
    endif()
    if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
      set_property(TARGET "${target}" PROPERTY CXX_CLANG_TIDY "${AXSN_CLANG_TIDY_EXECUTABLE}")
    endif()
  endif()
endfunction()

function(_axsn_apply_link_policy target)
  get_target_property(_type "${target}" TYPE)
  if(NOT _type MATCHES "^(EXECUTABLE|SHARED_LIBRARY|MODULE_LIBRARY)$")
    return()
  endif()

  if(AXSN_ENABLE_SECTION_GC AND ANDROID)
    target_link_options("${target}" PRIVATE "LINKER:--gc-sections")
  endif()

  # r28+ defaults to 16 KB max page size. Older/unknown NDKs get explicit
  # linker page sizes on 64-bit ABIs. APK packaging/alignment must also comply.
  if(ANDROID AND AXSN_ENABLE_16K_PAGE_SIZE
       AND (ANDROID_ABI STREQUAL "arm64-v8a" OR ANDROID_ABI STREQUAL "x86_64"))
    set(_ndk_version "${CMAKE_ANDROID_NDK_VERSION}")
    if(NOT _ndk_version AND DEFINED ANDROID_NDK_REVISION)
      set(_ndk_version "${ANDROID_NDK_REVISION}")
    endif()
    if(NOT _ndk_version OR _ndk_version VERSION_LESS "28.0")
      target_link_options("${target}" PRIVATE
                "LINKER:-z,max-page-size=16384"
                "LINKER:-z,common-page-size=16384")
    endif()
  endif()

  if(AXSN_ENABLE_BUILD_ID AND CMAKE_EXECUTABLE_FORMAT STREQUAL "ELF")
    target_link_options("${target}" PRIVATE "LINKER:--build-id=sha1")
  endif()

  if(ANDROID AND (_type STREQUAL "SHARED_LIBRARY" OR _type STREQUAL "MODULE_LIBRARY"))
    target_link_options("${target}" PRIVATE "LINKER:--exclude-libs,ALL")
  endif()

  if(AXSN_GENERATE_LINK_MAP)
    target_link_options("${target}" PRIVATE
            "LINKER:-Map,${CMAKE_CURRENT_BINARY_DIR}/${target}-$<CONFIG>.map")
  endif()

  if(AXSN_ENABLE_ASAN OR AXSN_ENABLE_UBSAN)
    set(_sanitizers)
    if(AXSN_ENABLE_ASAN)
      list(APPEND _sanitizers address)
    endif()
    if(AXSN_ENABLE_UBSAN)
      list(APPEND _sanitizers undefined)
    endif()
    list(JOIN _sanitizers "," _sanitizer_list)
    target_link_options("${target}" PRIVATE "-fsanitize=${_sanitizer_list}")
  endif()
endfunction()

function(axsn_configure_target target)
  if(NOT TARGET "${target}")
    message(FATAL_ERROR "axsn_configure_target: unknown target '${target}'")
  endif()
  get_target_property(_imported "${target}" IMPORTED)
  if(_imported)
    message(FATAL_ERROR "axsn_configure_target: imported target '${target}' cannot be configured")
  endif()

  _axsn_apply_compile_policy("${target}")
  axsn_apply_warnings("${target}")
  _axsn_apply_link_policy("${target}")

  get_target_property(_type "${target}" TYPE)
  if(AXSN_ENABLE_ELF_NOTE AND CMAKE_EXECUTABLE_FORMAT STREQUAL "ELF"
       AND _type MATCHES "^(EXECUTABLE|SHARED_LIBRARY|MODULE_LIBRARY)$")
    axsn_add_elf_identity("${target}")
  endif()

  if(AXSN_GENERATE_SIZE_TARGETS AND _type MATCHES "^(EXECUTABLE|SHARED_LIBRARY|MODULE_LIBRARY)$")
    if(NOT AXSN_LLVM_SIZE_EXECUTABLE)
      find_program(AXSN_LLVM_SIZE_EXECUTABLE NAMES llvm-size)
    endif()
    if(AXSN_LLVM_SIZE_EXECUTABLE AND NOT TARGET "${target}_size")
      add_custom_target("${target}_size"
                COMMAND "${AXSN_LLVM_SIZE_EXECUTABLE}" -A "$<TARGET_FILE:${target}>"
                DEPENDS "${target}"
                COMMENT "AXSN section report: ${target}"
                VERBATIM)
    endif()
  endif()
endfunction()
