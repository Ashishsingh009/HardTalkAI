plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)

    // JVM target — consumed by the Ktor :server (and Android later).
    jvm()

    // Enable these targets per platform as the apps come online:
    //   js(IR) { browser(); nodejs() }        // share the engine with the React web app
    //   androidTarget()                        // requires the Android SDK
    //   iosX64(); iosArm64(); iosSimulatorArm64()  // requires macOS + Xcode
    //   listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    //       it.binaries.framework { baseName = "Shared" }
    //   }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
