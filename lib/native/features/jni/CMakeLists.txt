# JNI is an OBJECT library. Linking axsn::jni to a
# shared library embeds the compiled implementation into that DSO, giving
# every consuming DSO its own VM slot and JNI_OnLoad/Unload.
add_library(axsn_jni OBJECT
        src/jni.c
)
add_library(axsn::jni ALIAS axsn_jni)
set_target_properties(axsn_jni PROPERTIES
        EXCLUDE_FROM_ALL TRUE
        POSITION_INDEPENDENT_CODE ON
        C_VISIBILITY_PRESET hidden
)
target_include_directories(axsn_jni PUBLIC include)
target_link_libraries(axsn_jni PUBLIC axsn)
axsn_configure_target(axsn_jni)
