# JNI is an OBJECT library. Linking axsn::jni to a
# shared library embeds the compiled implementation into that DSO, giving
# every consuming DSO its own VM slot and JNI_OnLoad/Unload.
add_library(axsn_jni OBJECT
        src/jni.c
        src/hooks.cpp
)
add_library(axsn::jni ALIAS axsn_jni)
set_target_properties(axsn_jni PROPERTIES
        EXCLUDE_FROM_ALL TRUE
        POSITION_INDEPENDENT_CODE ON
        C_VISIBILITY_PRESET hidden
        CXX_VISIBILITY_PRESET hidden
)
target_include_directories(axsn_jni PUBLIC include)
# pulls compiled object files
target_sources(axsn_jni PUBLIC
        $<TARGET_OBJECTS:axsn>
)
target_link_libraries(axsn_jni PUBLIC axsn)
axsn_configure_target(axsn_jni)
