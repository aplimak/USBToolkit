import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.reactivecircus.appversioning)
}

android {
    namespace = "ir.aeliux.usbtoolkit"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ir.aeliux.usbtoolkit"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val localProperties = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localProperties.load(FileInputStream(localPropsFile))
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("KEYSTORE_PATH"))
            storePassword = localProperties.getProperty("KEYSTORE_PASSWORD")
            keyAlias = localProperties.getProperty("KEY_ALIAS")
            keyPassword = localProperties.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            externalNativeBuild {
                cmake {
                    arguments.add("-DCMAKE_BUILD_TYPE=Release")
                }
            }
        }
        debug {
            applicationIdSuffix = ".dbg"
            versionNameSuffix = "-debug"
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        jniLibs {
            pickFirsts += listOf("**/libgtest.so", "**/libgtest_main.so")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

appVersioning {
    tagFilter.set("[0-9]*.[0-9]*.[0-9]")
    versionCode.set {
        // Placeholder for 99 untagged commits with no conflict
        (tag.toSemVer().toVersionCode(digitsPerComponent = 2) * 100) + tag.commitsSinceLatestTag
    }
    versionName.set {
        // add build variant to the versionName for debug builds
        val suffix = if (variant.isDebugBuild) "-${variant.variantName}" else ""
        "${tag.name}${if (tag.commitsSinceLatestTag > 0) "-" + tag.commitHash else ""}$suffix"
    }
}

dependencies {
    implementation(project(":lib:ui"))
    runtimeOnly(project(":app:rootservice"))
    implementation(project(":lib:core"))
    implementation(project(":app:shared"))
    implementation(project(":lib:magic"))
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
