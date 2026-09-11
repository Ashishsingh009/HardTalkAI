import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

fun String.asBuildConfigString(): String =
    buildString {
        append('"')
        for (ch in this@asBuildConfigString) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else -> append(ch)
            }
        }
        append('"')
    }

fun Project.secretProperty(gradleKey: String, envKey: String): Provider<String> {
    val fromFile = providers.provider {
        val file = rootProject.file("local.properties")
        if (!file.exists()) return@provider ""
        val props = Properties()
        file.inputStream().use { props.load(it) }
        props.getProperty(gradleKey).orEmpty().trim()
    }
    return providers.gradleProperty(gradleKey)
        .orElse(providers.environmentVariable(envKey))
        .orElse(fromFile)
}

val revenueCatTestStoreKey = secretProperty(
    gradleKey = "revenuecat.androidApiKey",
    envKey = "REVENUECAT_ANDROID_API_KEY",
)
val revenueCatPlayKey = secretProperty(
    gradleKey = "revenuecat.playApiKey",
    envKey = "REVENUECAT_PLAY_API_KEY",
)

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.revenuecat.purchases)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "ai.hardtalk.source"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "ai.hardtalk.source"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        // Override at build time: ./gradlew :androidApp:installDebug -Phardtalk.apiBaseUrl=http://192.168.1.10:3001
        val apiBaseUrl = providers.gradleProperty("hardtalk.apiBaseUrl")
            .getOrElse("http://10.0.2.2:3001")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "REVENUECAT_API_KEY", "\"\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            buildConfigField(
                "String",
                "REVENUECAT_API_KEY",
                revenueCatTestStoreKey.get().asBuildConfigString(),
            )
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val playKey = revenueCatPlayKey.get()
            val releaseKey = if (playKey.startsWith("test_")) "" else playKey
            buildConfigField(
                "String",
                "REVENUECAT_API_KEY",
                releaseKey.asBuildConfigString(),
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
