plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ir.aeliux.usbtoolkit.rootservice"
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":app:shared"))
    implementation(project(":app:usb"))
    implementation(project(":lib:core"))
    implementation(project(":lib:magic"))
    implementation(libs.appcompat)
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
