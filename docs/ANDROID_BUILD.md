# Android build and signing

## Pinned toolchain

- JDK 17
- Gradle Wrapper 8.13
- Android Gradle Plugin 8.13.0
- Kotlin 2.2.20
- KSP 2.2.20-2.0.4
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

The debug build has the application ID `com.yusufculcu.trader.debug`. It is signed with the Android Gradle Plugin's generated debug key and is suitable for development installation, not public release distribution.

## CI

GitHub Actions runs the same Gradle tasks on pushes to `main` and `build/**`, and on pull requests targeting `main`. The workflow uploads the debug APK as the `trader-debug-apk` artifact for 14 days.

## Release signing

No production signing key or password belongs in this repository. A release-signing workflow must be added only after the user configures GitHub Actions secrets for a dedicated Trader keystore and passwords. Never reuse signing identities from VaultOS, Memo or another project. Until that exists, do not describe a debug APK as a production release.
