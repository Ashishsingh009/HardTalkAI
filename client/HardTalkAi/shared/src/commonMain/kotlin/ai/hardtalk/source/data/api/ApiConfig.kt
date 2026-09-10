package ai.hardtalk.source.data.api

/**
 * Default FastAPI origin used by the Android emulator.
 *
 * `10.0.2.2` is the emulator's alias for the host loopback, so this reaches
 * `uvicorn` on the development machine at port 3001.
 *
 * Override from the Android host with `App(apiBaseUrl = BuildConfig.API_BASE_URL)`
 * or `./gradlew :androidApp:installDebug -Phardtalk.apiBaseUrl=http://192.168.1.10:3001`
 * when talking to a physical device on the LAN.
 */
const val ANDROID_EMULATOR_API_BASE_URL = "http://10.0.2.2:3001"

/**
 * Default FastAPI origin for the iOS simulator (host loopback).
 */
const val IOS_SIMULATOR_API_BASE_URL = "http://127.0.0.1:3001"

/**
 * Platform default used when the host does not pass an explicit base URL.
 * Android debug → [ANDROID_EMULATOR_API_BASE_URL]; iOS → [IOS_SIMULATOR_API_BASE_URL].
 */
expect fun defaultApiBaseUrl(): String

fun normalizeApiBaseUrl(baseUrl: String): String = baseUrl.trim().trimEnd('/')
