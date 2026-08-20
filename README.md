# Spotifusion

A Spotify-style music app: search pulls tracks from trusted/official YouTube
channels, playback runs through YouTube's official embedded player (no raw
audio extraction — that keeps it inside YouTube's Terms of Service), auth is
Google Sign-In via Firebase, and your library (liked songs, playlists,
recently played) is saved to Firestore.

## 1. Create your Firebase project

1. Go to https://console.firebase.google.com → **Add project**.
2. **Build → Authentication → Sign-in method → Google → Enable.**
3. **Build → Firestore Database → Create database** (production mode is fine —
   `firestore.rules` locks it down to each signed-in user).
4. **Project settings → General → Your apps → Web (</>together>)** → register
   an app → copy the `firebaseConfig` values into a `.env` file at the project
   root (copy `.env.example` → `.env` and fill it in).
5. **Project settings → Your apps → add an Android app too** (needed later for
   the Android Studio wrapper) with your chosen package name, e.g.
   `com.yourname.spotifusion`, and download `google-services.json` for that
   step.

## 2. Get a YouTube Data API key

1. In the same Google Cloud project (Firebase projects are GCP projects),
   go to https://console.cloud.google.com/apis/library/youtube.googleapis.com
   and enable **YouTube Data API v3**.
2. **APIs & Services → Credentials → Create credentials → API key.**
   Restrict it to the YouTube Data API v3.
3. Set it on your Cloud Function config:
   ```
   cd functions
   firebase functions:config:set youtube.key="YOUR_API_KEY"
   ```
   (Functions v2 / newer CLI: use `firebase functions:secrets:set YOUTUBE_API_KEY`
   instead, and reference it as a secret in `functions/src/index.js`.)

## 3. Run it locally

```bash
npm install
firebase login
firebase init   # if you haven't already linked this folder to your project
# in one terminal:
cd functions && npm install && firebase emulators:start --only functions
# in another terminal, from the project root:
npm run dev
```

Point `VITE_SEARCH_API_BASE` in `.env` at the emulator URL that gets printed
(looks like `http://127.0.0.1:5001/YOUR_PROJECT_ID/us-central1/api`).

## 4. Deploy (go live)

```bash
npm run build
firebase deploy --only hosting,functions,firestore:rules
```

After deploying functions, update `VITE_SEARCH_API_BASE` in `.env` to the
deployed function URL (shown in the deploy output, or
Firebase Console → Functions), then rebuild and redeploy hosting.

**Google Sign-In on your real domain:** Firebase Console → Authentication →
Settings → Authorized domains → add your deployed domain (Firebase Hosting's
`*.web.app` domain is added automatically).

## 5. Wrapping it for Android Studio (APK)

The simplest path for what you described:

1. In Android Studio, create a new **Empty Views Activity** project.
2. Add a `WebView` to the main layout that loads your deployed HTTPS URL
   (`https://YOUR_PROJECT.web.app`).
3. In your Activity, enable JavaScript and DOM storage on the WebView
   (`settings.javaScriptEnabled = true`, `settings.domStorageEnabled = true`)
   — Firebase Auth and the YouTube player both need these.
4. Add `INTERNET` permission in `AndroidManifest.xml`.
5. Google Sign-In inside a WebView: Google blocks OAuth inside plain
   WebViews for security. The code already detects a WebView user agent and
   falls back to `signInWithRedirect`, which works — but for the smoothest
   experience, consider using [Chrome Custom Tabs](https://developer.android.com/develop/ui/views/layout/webapps/managing-webview)
   for the auth step specifically, or migrate later to a **Trusted Web
   Activity** (via [Bubblewrap](https://github.com/GoogleChromeLabs/bubblewrap)),
   which runs your PWA in real Chrome instead of an embedded WebView and
   avoids the Google Sign-In restriction entirely — recommended once you're
   ready to publish to the Play Store.
6. Register the Android app's SHA-1 fingerprint in the Firebase Console
   (Project settings → your Android app) so Google Sign-In trusts it.

## What's built vs. what's next

**Working now:** Google sign-in, YouTube search (trusted-channel filtered),
playback via the YouTube IFrame API (play/pause/seek/skip/shuffle/repeat/
volume), liked songs, playlists (create/add/remove/delete), recently played,
responsive desktop + mobile layouts (sidebar+player bar on desktop, bottom
tabs + mini-player + full-screen now-playing sheet on mobile).

**Not built yet — tell me which to tackle next:** matching the exact visual
details of your two Figma files pixel-for-pixel (I approximated Spotify's
real dark theme since I can't read the Figma canvas directly — send screen
exports and I'll match them precisely), queue view/drag-reordering, an
"Add to playlist" menu from the search results, artist/album pages, search
history, offline caching, and a proper landing/onboarding screen.
