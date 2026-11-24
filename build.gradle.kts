import kotlin.collections.plusAssign
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Packaging

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

fun Packaging.applyNativeTestPackaging() {
    jniLibs {
        pickFirsts += "**"
        testOnly += listOf(
            "**/libgtest.so",
            "**/libgtest_main.so",
            "**/lib*tests.so",
        )
    }
}

subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            packaging {
                applyNativeTestPackaging()
            }
        }
    }
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            packaging {
                applyNativeTestPackaging()
            }
        }
    }
}

allprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}
