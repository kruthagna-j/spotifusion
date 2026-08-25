# Spotifusion

A free, Spotify-style music app: search pulls tracks from YouTube Music via
[ytmusicapi](https://github.com/sigma67/ytmusicapi) — a free, key-less,
unofficial client, so there's no Google API quota or billing dependency for
search at all — while playback runs through YouTube's official embedded
player (no raw audio extraction — that keeps it inside YouTube's Terms of
Service). Auth is Google Sign-In via Firebase, and your library (liked
songs, playlists, recently played) is saved to Firestore. You can also add
your own audio files from your device — those play offline, entirely in the
browser.

**Everything here runs on free tiers — no credit card required anywhere**
(Firebase Spark plan + Vercel Hobby plan + a free-tier host for the small
Python search backend). See `api/README.md` for the backend specifically.

## 1. Create your Firebase project

1. Go to https://console.firebase.google.com → **Add project**.
2. **Build → Authentication → Sign-in method → Google → Enable.**
3. **Build → Firestore Database → Create database** (production mode is fine —
   `firestore.rules` locks it down to each signed-in user).
4. **Project settings → General → Your apps → Web (`</>`)** → register
   an app → copy the `firebaseConfig` values into a `.env` file at the project
   root (copy `.env.example` → `.env` and fill it in).
5. **Authentication → Settings → Authorized domains** → add every domain you'll
   actually open the app from (e.g. `your-app.vercel.app`, plus `localhost`
   which is included by default). Missing this causes
   `auth/unauthorized-domain` when someone tries to sign in.

## 2. Run the free search backend

Search now goes through a small self-hosted backend in `api/` (FastAPI +
[ytmusicapi](https://github.com/sigma67/ytmusicapi) + Redis) instead of the
official YouTube Data API — no Google API key needed for search at all.
Full setup/deploy instructions are in **`api/README.md`**; the short version:

```bash
cd api
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

Then set `VITE_MUSIC_API_URL` in your frontend `.env` to wherever this runs
(`http://localhost:8000` for local dev, or your deployed backend URL in
production — Render/Railway/Fly/a VPS all have free tiers; see
`api/README.md` for deploying it).

This backend is a standalone Python service, not a Vercel function — it
needs a persistent Redis connection to scale horizontally, which serverless
functions aren't a good fit for.

## 3. Deploy the frontend (Vercel)

1. Push this repo to GitHub, then import it in https://vercel.com/new.
2. In the Vercel project → **Settings → Environment Variables**, add:
   - `VITE_MUSIC_API_URL` = wherever you deployed the `api/` backend (step 2)
   - All the `VITE_FIREBASE_*` values from your `.env`
3. Deploy.
4. Add the resulting `your-app.vercel.app` domain to Firebase's Authorized
   domains list (step 1.5 above) or sign-in will fail with
   `auth/unauthorized-domain`.

## 4. Run it locally

```bash
npm install
npm run dev
```

## Local files (offline, on-device library)

The "Local Files" tab (sidebar on desktop, bottom of "Your Library" on
mobile) lets anyone add their own audio files from their device. They're
stored as blobs in IndexedDB — never uploaded anywhere — and play back
through a real `<audio>` element alongside the YouTube-backed player, with
full support for play/pause/seek/skip/shuffle/repeat, liking, and adding to
playlists. They only exist on the device/browser they were added on.

## Installing as a mobile/desktop app (PWA)

This is an installable Progressive Web App: on Android/desktop Chrome, look
for the install icon in the address bar (or Menu → "Install app"); on iOS
Safari, use Share → "Add to Home Screen". Once installed it opens full-screen
with its own icon, exactly like a native music player app, and the app shell
is cached for fast/offline-ish launches (local files play fully offline
regardless, since they never touch the network; YouTube-backed streaming
still needs a connection, same as the real Spotify app).

**Bluetooth / lock-screen controls:** handled via the Media Session API —
once a track is playing, Bluetooth headset/earbud hardware buttons and
lock-screen or notification-shade media controls (play/pause/skip) work
automatically. No separate Bluetooth pairing code is needed in the app
itself; that's handled by the OS once headphones are paired.

## Wrapping it as a real Android APK (optional, still free)

Because this is now a proper installable PWA (valid manifest + service
worker), the cleanest path to a Play Store-style APK is a **Trusted Web
Activity** via [Bubblewrap](https://github.com/GoogleChromeLabs/bubblewrap) —
it runs your deployed PWA in real Chrome under an app wrapper, which (unlike
a plain WebView) doesn't hit Google's WebView Sign-In restrictions. Point it
at your deployed `https://your-app.vercel.app` URL; register the generated
app's SHA-1 fingerprint in Firebase Console → your Android app, so Google
Sign-In trusts it.

## Search and streaming require sign-in

`/api/search` and `/api/song` on the backend require a valid Firebase ID
token — enforced server-side (see `api/auth.py`), not just hidden behind a
frontend sign-in screen. Anonymous requests get a `401`. Set
`FIREBASE_PROJECT_ID` on the backend (same value as `VITE_FIREBASE_PROJECT_ID`
on the frontend) or every request will be rejected. Local files remain
usable without an account, since they never touch this backend at all.

## Local files: folder access, persistent storage, output device selection

Beyond basic file import, the Local Files section supports:
- **Add a Folder** (Chromium-based browsers): grants access to a whole
  folder at once via the File System Access API, rather than picking files
  one at a time — falls back to a plain file picker where unsupported
  (e.g. Safari).
- **Persistent storage permission**: a prompt to ask the browser not to
  evict local files under storage pressure (`navigator.storage.persist()`).
- **Connect to a device** (Chromium-based browsers, in the player bar): lets
  you pick which already-paired output device (e.g. Bluetooth headphones/
  speaker) local file playback routes to. This doesn't pair Bluetooth
  itself — that's always an OS-level action — it's the same "choose an
  output" pattern Spotify's own Connect feature uses. Only affects local
  files; YouTube's audio renders inside a cross-origin iframe this API
  can't reach, same limitation as the equalizer.

## Current feature set

Spotifusion currently includes:

- Home dashboard with quick access, recent listening and discovery
- Search with debounce, cancellation, caching, recent searches and retry states
- Library with playlists, liked songs, recently played, local music and discovery
- Playlist creation, editing, reordering, removal and shuffle/play actions
- Album and artist collection views derived from catalog/search metadata
- Shared playback state for YouTube-backed and local-device tracks
- Play/pause, previous/next, seek, volume, mute, shuffle and repeat
- Queue with add, play-next, remove, reorder and clear controls
- Expanded Now Playing with lyrics, queue, sleep timer, sharing and track details
- Timestamped lyrics highlighting when the backend provides synced lyrics
- Local music through IndexedDB, including folder import where supported
- Local-file equalizer and supported output-device selection
- Firebase Google authentication and Firestore-backed library data
- Comprehensive settings for account, privacy, notifications, sleep timer, EQ and local storage
- Responsive desktop/tablet/mobile navigation and PWA support
- Vercel SPA fallback configuration and FastAPI CORS configuration for production + LAN development

### Production requirements

Before deployment, set the `VITE_FIREBASE_*` values and `VITE_MUSIC_API_URL` in the hosting provider's environment variables.
The FastAPI service must have `FIREBASE_PROJECT_ID` configured and must allow the deployed frontend origin through `ALLOWED_ORIGINS`. Firebase Authentication must list every domain you actually use under Authorized domains.

The project deliberately does not download or rip YouTube audio. Online playback uses the official YouTube IFrame Player, while offline playback is for audio files supplied by the user.
