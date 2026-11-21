# CI validation marker

This commit exists only to trigger the repository's Android CI for the app identity, adaptive launcher icon, version 0.2.0 and optional stable signing changes on `feature/public-search-adapter`.

Source implementation tree is inherited unchanged from `e30053b6e6730773d7eb44c520a21a9364954601`. This branch is not a release branch and must not be installed from unless the workflow emits the stable-signed `trader-installable-apk` artifact.


Retry after fixing empty TRADER_KEYSTORE_PATH handling in Gradle; stable signing secrets are currently absent, so only a CI-only artifact is expected.
