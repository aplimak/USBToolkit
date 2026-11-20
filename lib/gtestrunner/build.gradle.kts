plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ir.aeliux.gtestrunner"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 27
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.junit)
}
