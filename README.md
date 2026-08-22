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

`vercel dev` (instead of `npm run dev`) will also serve the `/api` functions
locally if you want to test search without deploying first — install with
`npm i -g vercel`, then `vercel dev`.

## Bring-your-own YouTube API key (legacy/optional)

This is left over from before the switch to ytmusicapi and no longer affects
search (ytmusicapi needs no key at all). Anyone signed in can still open
their profile menu and set a personal YouTube Data API key if you want it
available for something else later — it's just unused by `Search.jsx` today.

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

## Legacy: official YouTube Data API path (kept, unused by default)

The original key-based search implementation is still in the repo,
untouched, in case you want it as a fallback:

- `functions/src/index.js` — Firebase Cloud Function version (needs Blaze billing to deploy)
- `api/search.js` / `api/video/[id].js` — Vercel serverless function version (free, no billing needed)
- `src/lib/youtube.js` — the matching frontend client

None of these are called by `Search.jsx` anymore — it now uses
`src/lib/musicApi.js` against the ytmusicapi backend in `api/` (Python)
described above. To switch back, point `Search.jsx` at `searchTracks` from
`lib/youtube.js` instead of `searchMusic` from `lib/musicApi.js`, and set
`VITE_SEARCH_API_BASE` + a YouTube Data API key as before.

## What's built vs. what's next

**Working now:** Google sign-in with Firestore-backed profile/library data,
YouTube search (trusted-channel filtered, with optional bring-your-own API
key), playback via the YouTube IFrame API *and* local on-device files (both
through the same play/pause/seek/skip/shuffle/repeat/volume controls), liked
songs, playlists (create/add/remove/delete), recently played, full account
management (view details, log out, delete account), installable PWA with
Media Session/Bluetooth hardware-button support, responsive desktop + mobile
layouts (sidebar+player bar on desktop, bottom tabs + mini-player +
full-screen now-playing sheet on mobile).

**Deliberately not built:** downloading/ripping YouTube audio for offline
storage — that's a YouTube Terms of Service and copyright issue regardless of
cost, so it's out of scope here. Local file upload is the offline-playback
alternative, for audio you already own.

**Not built yet — tell me which to tackle next:** matching the exact visual
details of your Figma files pixel-for-pixel (I approximated Spotify's real
dark theme since I can't read the Figma canvas directly — send screen
exports and I'll match them precisely), queue view/drag-reordering, artist/
album pages, search history, and a proper landing/onboarding screen.
