plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "ir.aeliux.usbtoolkit.shared"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 27

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(project(":lib:core"))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.kotlin.parcelize.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    add("kotlinCompilerPluginClasspath", "org.jetbrains.kotlin:kotlin-parcelize-compiler:2.2.10")
}
