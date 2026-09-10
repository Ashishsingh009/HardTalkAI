This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

`:shared` talks to the FastAPI backend (`GET /api/scenarios`, `POST /api/chat`). There is no
on-device coaching engine — scores and counterpart replies come from the server. The Android
loop is practice → score → retry (three existing scenarios, three scored turns per round),
not an open-ended chat.

### Running the apps

Start FastAPI from the repo root first:

```bash
.venv/bin/uvicorn app.main:app --app-dir server --host 0.0.0.0 --port 3001 --reload
```

Then use the run configurations in your IDE's toolbar, or:

- Android app: `./gradlew :androidApp:installDebug`
  - Emulator default API URL: `http://10.0.2.2:3001` (`10.0.2.2` = host loopback).
  - Physical device: `./gradlew :androidApp:installDebug -Phardtalk.apiBaseUrl=http://<lan-ip>:3001`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there
  (simulator default API URL: `http://127.0.0.1:3001`).

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android / shared tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
