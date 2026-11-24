add_library(axsn_jni_runtime SHARED
        src/jni.c
        src/runtime.cpp
        src/shared.cpp
)
set_target_properties(axsn_jni_runtime PROPERTIES
        EXCLUDE_FROM_ALL TRUE
        POSITION_INDEPENDENT_CODE ON
        C_VISIBILITY_PRESET hidden
        CXX_VISIBILITY_PRESET hidden
)
target_compile_definitions(axsn_jni_runtime PRIVATE AXSN_JNI_RUNTIME=1)
target_include_directories(axsn_jni_runtime PUBLIC include)
target_link_libraries(axsn_jni_runtime PRIVATE axsn)
axsn_configure_target(axsn_jni_runtime)

# JNI is an OBJECT library. Linking axsn::jni to a
# shared library embeds the compiled implementation into that DSO, giving
# every consuming DSO its own VM slot and JNI_OnLoad/Unload.
add_library(axsn_jni OBJECT
        src/entry.cpp
        src/shared.cpp
)
add_library(axsn::jni ALIAS axsn_jni)
set_target_properties(axsn_jni PROPERTIES
        EXCLUDE_FROM_ALL TRUE
        POSITION_INDEPENDENT_CODE ON
        C_VISIBILITY_PRESET hidden
        CXX_VISIBILITY_PRESET hidden
)
target_compile_definitions(axsn_jni PRIVATE AXSN_JNI_RUNTIME=0)
# pulls compiled object files
target_sources(axsn_jni PUBLIC
        $<TARGET_OBJECTS:axsn>
)
target_link_libraries(axsn_jni PUBLIC
        axsn
        axsn_jni_runtime)
axsn_configure_target(axsn_jni)
