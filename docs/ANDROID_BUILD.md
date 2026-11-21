# Android build and signing

## Pinned toolchain

- JDK 17
- Gradle Wrapper 8.13
- Android Gradle Plugin 8.13.0
- Kotlin Android/Compose 2.4.21
- KSP 2.3.12
- Compile SDK 36 / Build Tools 36.0.0
- Minimum Android API 26; target API 35

The Gradle wrapper is committed. Do not replace it with an unpinned system Gradle.

## Local build

Open the repository in Android Studio or run:

```bash
./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug
```

The debug APK is written to:

`app/build/outputs/apk/debug/app-debug.apk`

The debug build has the stable application ID `com.yusufculcu.trader.debug` and version code 2 (`0.2.0-debug`). The package ID is intentionally unchanged across updates. When the dedicated Trader development keystore is configured in GitHub Actions, every installable debug APK is signed with that same certificate; Android can update the app in place and retain its Room database and app-private preferences. The debug key is not a production release identity.

## CI

GitHub Actions runs the same Gradle tasks on pushes to `main` and `build/**`, and on pull requests targeting `main`. It uploads `trader-installable-apk` only when both `TRADER_KEYSTORE_BASE64` and `TRADER_KEYSTORE_PASSWORD` repository Actions secrets are configured. Without them, it uploads `trader-ci-debug-apk-not-for-installation`; that ephemeral CI signature must never be sideloaded as an update target.


Robolectric tests use Maven Central's canonical `repo.maven.apache.org` host for Android SDK artifacts rather than the default `repo1.maven.org` checksum endpoint, which returned intermittent 404 responses in CI.

## Persistent development signing setup

A dedicated development keystore is generated for Trader and is deliberately not committed to Git. Configure these two repository Actions secrets under **Settings → Secrets and variables → Actions**:

- `TRADER_KEYSTORE_BASE64`: base64-encoded contents of the supplied `trader-upload.jks` file (the setup bundle includes the exact base64 text).
- `TRADER_KEYSTORE_PASSWORD`: the password in the setup bundle. Alias is fixed as `trader-upload`; key password equals store password.

The certificate SHA-256 fingerprint pinned in `.github/workflows/android.yml` and `CERTIFICATE_FINGERPRINT.txt` in the private signing bundle`. After setting both secrets, rerun the Android workflow. The workflow will produce the installable artifact only when the stable key is present. Keep the keystore and password in a secure password manager and retain an offline backup. Never reuse signing identities from VaultOS, Memo or another project.

**Important migration boundary:** an APK previously signed with a different ephemeral CI debug key cannot be updated in place with this new key. Do not uninstall it casually. First export a verified encrypted Trader workspace backup (snapshot v2 includes public discovery feed settings); only then plan a one-time signature transition. Once installed from the stable-signed artifact, future versionCode increments update in place and retain app data. Release signing remains a separate future identity and must not silently replace this development key.


## Launcher identity and data continuity

The Android manifest declares the Trader adaptive launcher icon (`ic_launcher` and `ic_launcher_round`). The app ID remains `com.yusufculcu.trader.debug`; the Room database and app-private preferences remain in place on an in-place package update. `allowBackup=false` disables Android cloud auto-backup; it does not erase data during an update. Uninstalling or clearing app storage removes local data. Use Trader's explicit encrypted export/restore for migration or device replacement.

## Latest verified Discover build

- Feature PR: #18, branch feature/public-search-adapter.
- CI validation: run 385, validation branch build/discovery-source-slice; its tree matches the PR head tree. 97 tests, 0 failures, 0 errors, 0 skipped; lint, assembleDebug, apksigner and exact package/version checks passed.
- Artifact: trader-debug-apk, 69,441,948 bytes; package com.yusufculcu.trader.debug, version 0.1.0-debug; SHA-256 b28d0b089962fb3481425ceae14dfb4d4449527a15f7d5bd3fd4408b2664f210.
- A CI-generated debug key is not a production signing identity; update compatibility with a previously installed debug APK is not established. Do not uninstall an existing installation without protecting local data.


## Current candidate (CI verified; stable installable signing setup pending)

- Version: `0.2.0-debug`, versionCode 2; package remains `com.yusufculcu.trader.debug`.
- Adds adaptive launcher icon, optional stable development signing via the two Actions secrets above, and encrypted backup snapshot v2 for public source settings.
- CI run 405 passed 98 tests, lint, build, package/version and signature checks. Stable secrets were absent, so the produced artifact is CI-only and must not be installed. Previous CI run 385 artifact used an ephemeral runner key and cannot be assumed upgrade-compatible.
