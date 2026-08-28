# Spotifusion Android

Android WebView wrapper for the deployed Spotifusion player.

## Build locally

Open this `android` folder in Android Studio. Use a JDK 21 runtime (Android Studio's bundled JBR is fine) and build `app`.

For a directly installable APK, use **Build > Build APK(s)** for the debug variant, or run:

```text
gradle assembleDebug
```

The debug APK is signed automatically and is installable on a normal Android device.

The release configuration is also signed with the debug key for project/demo distribution. Do not publish that release artifact to Google Play; configure a private release keystore first.

## Media access

The Android wrapper exposes `window.SpotifusionAndroid.requestMediaAccess()` and `hasMediaAccess()` to the web app so the mobile Settings media-access toggle can request Android's audio permission.
