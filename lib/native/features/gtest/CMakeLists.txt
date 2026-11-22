add_library(axsn_gtest OBJECT
        src/gtest.cpp
)
add_library(axsn::gtest ALIAS axsn_gtest)
set_target_properties(axsn_gtest PROPERTIES
        EXCLUDE_FROM_ALL TRUE
        POSITION_INDEPENDENT_CODE ON
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
)
target_include_directories(axsn_gtest PUBLIC include)
target_link_libraries(axsn_gtest PUBLIC
        axsn
        GTest::gtest
)
axsn_configure_target(axsn_gtest)
