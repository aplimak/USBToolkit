include_guard(GLOBAL)

include(CheckCCompilerFlag)
include(CheckCXXCompilerFlag)

function(axsn_apply_warnings target)
    if(NOT AXSN_ENABLE_WARNINGS)
        return()
    endif()
    if(NOT TARGET "${target}")
        message(FATAL_ERROR "axsn_apply_warnings: unknown target '${target}'")
    endif()

    get_target_property(_type "${target}" TYPE)
    if(_type STREQUAL "INTERFACE_LIBRARY")
        message(FATAL_ERROR "axsn_apply_warnings: '${target}' is an INTERFACE target")
    endif()

    set(_common
        -Wall -Wextra -Wpedantic -Wshadow
        -Wformat=2 -Wformat-security -Wundef
        -Wconversion -Wsign-conversion
        -Wnull-dereference -Wdouble-promotion
        -Wswitch-enum -Wimplicit-fallthrough
    )
    set(_c
        -Wstrict-prototypes -Wmissing-prototypes -Wmissing-declarations
        -Wold-style-definition -Wredundant-decls
        -Wpointer-arith -Wcast-qual -Wcast-align -Wvla
        -Wmissing-field-initializers -Werror=implicit-function-declaration
    )
    set(_cxx
        -Wnon-virtual-dtor -Wdelete-non-virtual-dtor
        -Woverloaded-virtual -Wsuggest-override
        -Wctor-dtor-privacy -Wold-style-cast
        -Wzero-as-null-pointer-constant
    )

    target_compile_options("${target}" PRIVATE ${_common})
    foreach(_flag IN LISTS _c)
        target_compile_options("${target}" PRIVATE
            "$<$<COMPILE_LANGUAGE:C>:${_flag}>")
    endforeach()
    foreach(_flag IN LISTS _cxx)
        target_compile_options("${target}" PRIVATE
            "$<$<COMPILE_LANGUAGE:CXX>:${_flag}>")
    endforeach()

    # Clang 20+ supports warning-suppression mappings. Probe instead of
    # assuming every Android NDK Clang version supports the option.
    if(CMAKE_C_COMPILER_ID STREQUAL "Clang" AND AXSN_SUPPRESSION_FILE
       AND EXISTS "${AXSN_SUPPRESSION_FILE}")
        check_c_compiler_flag(
            "--warning-suppression-mappings=${AXSN_SUPPRESSION_FILE}"
            AXSN_C_HAS_WARNING_SUPPRESSION_MAPPINGS)
        if(AXSN_C_HAS_WARNING_SUPPRESSION_MAPPINGS)
            target_compile_options("${target}" PRIVATE
                "$<$<COMPILE_LANGUAGE:C>:--warning-suppression-mappings=${AXSN_SUPPRESSION_FILE}>")
        endif()
    endif()
    if(CMAKE_CXX_COMPILER_ID STREQUAL "Clang" AND AXSN_SUPPRESSION_FILE
       AND EXISTS "${AXSN_SUPPRESSION_FILE}")
        check_cxx_compiler_flag(
            "--warning-suppression-mappings=${AXSN_SUPPRESSION_FILE}"
            AXSN_CXX_HAS_WARNING_SUPPRESSION_MAPPINGS)
        if(AXSN_CXX_HAS_WARNING_SUPPRESSION_MAPPINGS)
            target_compile_options("${target}" PRIVATE
                "$<$<COMPILE_LANGUAGE:CXX>:--warning-suppression-mappings=${AXSN_SUPPRESSION_FILE}>")
        endif()
    endif()

    if(AXSN_WARNINGS_AS_ERRORS)
        target_compile_options("${target}" PRIVATE -Werror)
    endif()
endfunction()
