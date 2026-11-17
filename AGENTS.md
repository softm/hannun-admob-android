# Project rules

- Android defaults: compileSdk 36, targetSdk 36 when an app module exists, minSdk 24.
- This repository produces an AAR library, not an APK or AAB.
- Keep the public package `softm.hannun.ads` backward compatible within a major version.
- Do not add app-specific package names, WebView URLs, production AdMob IDs, signing keys, or credentials.
- Debug/test IDs belong to consuming apps; this library only executes IDs explicitly supplied by the app.
- A release tag must pass unit tests, release AAR assembly, and Maven-local publication.
