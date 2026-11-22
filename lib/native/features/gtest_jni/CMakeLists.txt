# Java-facing GTest entrypoints are a separate compiled object feature.
# They are deliberately not part of axsn::gtest, because the standalone
# runner must not contain JNI entrypoints or depend on a JavaVM.
add_library(axsn_gtest_jni OBJECT
        src/jni.cpp
)
add_library(axsn::gtest_jni ALIAS axsn_gtest_jni)
set_target_properties(axsn_gtest_jni PROPERTIES
        EXCLUDE_FROM_ALL TRUE
        POSITION_INDEPENDENT_CODE ON
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
)
target_include_directories(axsn_gtest_jni PUBLIC include)
# pulls compiled object files
target_sources(axsn_gtest_jni PUBLIC
        $<TARGET_OBJECTS:axsn_gtest>
        $<TARGET_OBJECTS:axsn_jni>
)
# pulls headers and etc
target_link_libraries(axsn_gtest_jni PUBLIC
        axsn::gtest
        axsn::jni
)
axsn_configure_target(axsn_gtest_jni)
