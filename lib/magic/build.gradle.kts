plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
}

val magicAssetDir = layout.buildDirectory.dir("generated/magic_assets")

android {
    namespace = "ir.aeliux.magic"
    compileSdk {
        version = release(37)
    }

    ndkVersion = "30.0.16248370"

    defaultConfig {
        minSdk = 27
        buildConfigField("int", "DB_VERSION", "500480000") // 5.48

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=none",
                    "-DMAGIC_ASSET_OUTPUT_DIR=${magicAssetDir.get().asFile.absolutePath}"
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        prefab = true
        buildConfig = true
    }
}

abstract class PropertyBasedCopy : Copy() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    override fun getDestinationDir(): File = outputDirectory.get().asFile
    override fun setDestinationDir(destination: File) {
        outputDirectory.set(destination)
    }
}

androidComponents {
    onVariants { variant ->
        val cap = variant.name.replaceFirstChar { it.uppercase() }

        val stageMagicAssets = tasks.register<PropertyBasedCopy>("stage${cap}MagicAssets") {
            dependsOn("buildCMake${cap}")
            from(magicAssetDir)
            into(layout.buildDirectory.dir("generated/magic_assets_staged"))
        }

        variant.sources.assets?.addGeneratedSourceDirectory(
            stageMagicAssets,
            PropertyBasedCopy::outputDirectory
        )
    }
}

dependencies {
    implementation(project(":lib:ptrw"))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.kotlin.parcelize.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    add("kotlinCompilerPluginClasspath", "org.jetbrains.kotlin:kotlin-parcelize-compiler:2.2.10")
}
