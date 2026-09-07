# Spotifusion – Combined Web + Android Build

This package contains the Web Player, FastAPI music backend, and native Android client.

## Web Player

```bash
npm ci
npm run build
```

Required production environment variables are documented in `.env.example`.

## Android

Open the `android/` folder in Android Studio with JDK 21, or run:

```bash
cd android
./gradlew assembleDebug
```

The installable debug APK is:

`android/app/build/outputs/apk/debug/app-debug.apk`

## Cloud verification

`.github/workflows/verify-spotifusion.yml` builds both the Vite web app and Android debug APK on GitHub Actions.

## Runtime requirements

- Firebase Web credentials must be configured for the web player.
- The Android client uses Media3 for local playback and requires audio permission.
- The online music API is `https://spotifusion.onrender.com` by default.
- YouTube/online streaming remains dependent on the backend and the availability of the upstream music service.
