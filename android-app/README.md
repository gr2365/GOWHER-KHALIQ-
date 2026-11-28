# Gowher Khaliq Space for Android

This Android wrapper opens the live site at `https://www.gowherkhaliqspace.tech/`. Existing site features stay online and are displayed in the app. Camera and location access are requested only when the site's camera sky guide asks for them, and only for the site's HTTPS origin. Other links open in the device browser.

## Download an APK

Every push that changes `android-app/` starts the **Build Android APK** GitHub Actions workflow. Open the repository's **Actions** tab, choose the successful run, and download the `gowher-khaliq-space-debug-apk` artifact. The APK is a debug build for testing and direct installation; publishing to Google Play requires a release build signed with a private upload key.

## Build locally

Install JDK 17 and Android SDK Platform 35, then run from this directory:

```sh
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
