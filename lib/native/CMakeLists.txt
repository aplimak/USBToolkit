add_library(axsn_common INTERFACE)

target_include_directories(axsn_common INTERFACE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_link_libraries(axsn_common INTERFACE
    android
    log
)
