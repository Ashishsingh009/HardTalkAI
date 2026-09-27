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
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    implementation(libs.revenuecat.purchases)
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
        val revenueCatKey = revenueCatGoogleApiKey()
        buildConfigField(
            "String",
            "REVENUECAT_GOOGLE_API_KEY",
            "\"${revenueCatKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"",
        )
        // Default false so debug installs show the HardTalk Pro paywall for Shipaton.
        // Opt in to walk the full catalog without Play: -Phardtalk.ungatedCatalog=true
        val ungatedCatalog = providers.gradleProperty("hardtalk.ungatedCatalog")
            .orNull
            ?.equals("true", ignoreCase = true) == true
        buildConfigField("boolean", "UNGATED_CATALOG", ungatedCatalog.toString())
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

private fun revenueCatGoogleApiKey(): String {
    val fromProperty = providers.gradleProperty("hardtalk.revenuecatGoogleApiKey")
        .orNull
        ?.trim()
        .orEmpty()
    if (fromProperty.isNotEmpty()) return fromProperty
    val localFile = rootProject.file("local.properties")
    if (!localFile.isFile) return ""
    val properties = Properties()
    localFile.inputStream().use { stream -> properties.load(stream) }
    return properties.getProperty("hardtalk.revenuecatGoogleApiKey")?.trim().orEmpty()
}