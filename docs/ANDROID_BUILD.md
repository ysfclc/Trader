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

GitHub Actions runs the same Gradle tasks on pushes to `main`, `build/**` and `feature/**`, and on pull requests targeting `main` or `feature/**`. It uploads `trader-installable-apk` only when both `TRADER_KEYSTORE_BASE64` and `TRADER_KEYSTORE_PASSWORD` repository Actions secrets are configured. Without them, it uploads `trader-ci-debug-apk-not-for-installation`; that ephemeral CI signature must never be sideloaded as an update target.


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

- Branch: `feature/discovery-run-persistence`; commit `9adb46cce95baa733e7c6f346904b313f924230d`.
- CI run [44998620615](https://github.com/ysfclc/Trader/actions/runs/44998620615) passed `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`: **115 tests, 0 failures, 0 errors, 0 skipped**.
- CI also passed `apksigner verify` and exact `aapt` assertions for package `com.yusufculcu.trader.debug`, versionCode `2`, versionName `0.2.0-debug`.
- Artifact: `trader-ci-debug-apk-not-for-installation`, artifact ID `14745225976`, APK size 69,554,096 bytes; SHA-256 `dd3b28aa150c360584abc67a273129bab627b665c7c22ecda3a84124abb463d5`.
- Persistent Trader signing secrets were absent in this run. The artifact uses the ephemeral CI debug key and is not a stable-signed update target; provide the APK to the user's own signing step, and do not install over an existing copy expecting data-preserving update compatibility.
- This commit makes the durable discovery-run coordinator reachable from the actual Discover UI, with one objective-driven DuckDuckGo search, enabled feeds, recent run/source history, timeout isolation, cancellation and URL deduplication. It is still a manual, bounded source scan; it does not create company dossiers or qualified matches.
- CI does not prove live provider markup/access, physical-device UI acceptance, database migration/backup restore on Redmi or stable in-place update continuity.

