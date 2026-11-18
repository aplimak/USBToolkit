add_library(axsn_common INTERFACE)

target_include_directories(axsn_common INTERFACE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_link_libraries(axsn_common INTERFACE
    log
)

if (AXSN_BUILD_GTEST)
    target_compile_definitions(axsn_common INTERFACE AXSN_HAVE_GTEST)
    set(BUILD_GMOCK OFF CACHE BOOL "" FORCE)
    set(INSTALL_GTEST OFF CACHE BOOL "" FORCE)
    set(BUILD_SHARED_LIBS ON)
    add_subdirectory(third_party/googletest)
    unset(BUILD_SHARED_LIBS)
endif()
