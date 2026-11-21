include_guard(GLOBAL)

function(_axsn_c_escape input output)
    set(_v "${input}")
    string(REPLACE "\\" "\\\\" _v "${_v}")
    string(REPLACE "\"" "\\\"" _v "${_v}")
    string(REPLACE "\n" "\\n" _v "${_v}")
    string(REPLACE "\r" "\\r" _v "${_v}")
    set(${output} "${_v}" PARENT_SCOPE)
endfunction()

function(axsn_add_elf_identity target)
    if(NOT TARGET "${target}")
        message(FATAL_ERROR "axsn_add_elf_identity: unknown target '${target}'")
    endif()
    get_target_property(_type "${target}" TYPE)
    if(NOT _type MATCHES "^(EXECUTABLE|SHARED_LIBRARY|MODULE_LIBRARY)$")
        return()
    endif()

    _axsn_c_escape("${PROJECT_NAME}" _project)
    _axsn_c_escape("${target}" _target)
    if(ANDROID_ABI)
        _axsn_c_escape("${ANDROID_ABI}" _abi)
    else()
        set(_abi "non-android")
    endif()

    set(_revision "unknown")
    find_package(Git QUIET)
    if(GIT_FOUND)
        execute_process(
            COMMAND "${GIT_EXECUTABLE}" -C "${CMAKE_SOURCE_DIR}" rev-parse --short=12 HEAD
            OUTPUT_VARIABLE _git_revision
            OUTPUT_STRIP_TRAILING_WHITESPACE
            ERROR_QUIET
            RESULT_VARIABLE _git_result)
        if(_git_result EQUAL 0 AND _git_revision)
            set(_revision "${_git_revision}")
        endif()
    endif()
    _axsn_c_escape("${_revision}" _revision_c)

    set(_generated_dir "${CMAKE_CURRENT_BINARY_DIR}/_axsn/elf")
    file(MAKE_DIRECTORY "${_generated_dir}")
    set(_source "${_generated_dir}/${target}_axsn_note.c")
    set(AXSN_NOTE_PROJECT "${_project}")
    set(AXSN_NOTE_TARGET "${_target}")
    set(AXSN_NOTE_ABI "${_abi}")
    set(AXSN_NOTE_REVISION "${_revision_c}")
    configure_file("${AXSN_ROOT}/templates/elf_identity.c.in" "${_source}" @ONLY)
    target_sources("${target}" PRIVATE "${_source}")
    target_link_options("${target}" PRIVATE "LINKER:--undefined=axsn_elf_note")
endfunction()
