# Spotifusion

A free, Spotify-style music app: search pulls tracks from trusted/official
YouTube channels, playback runs through YouTube's official embedded player
(no raw audio extraction — that keeps it inside YouTube's Terms of Service),
auth is Google Sign-In via Firebase, and your library (liked songs,
playlists, recently played) is saved to Firestore. You can also add your own
audio files from your device — those play offline, entirely in the browser.

**Everything here runs on free tiers — no credit card required anywhere**
(Firebase Spark plan + Vercel Hobby plan + Google's free YouTube Data API
quota). No Firebase Cloud Functions / Blaze billing needed.

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

## 2. Get a free YouTube Data API key

1. In the same Google Cloud project (Firebase projects are GCP projects),
   go to https://console.cloud.google.com/apis/library/youtube.googleapis.com
   and enable **YouTube Data API v3**.
2. **APIs & Services → Credentials → Create credentials → API key.**
   Restrict it to the YouTube Data API v3. This is free (10,000 quota
   units/day, which is plenty for personal/small-scale use).

## 3. Deploy — the free way (Vercel)

The search backend lives at `/api/search.js` and `/api/video/[id].js` as
**Vercel Serverless Functions** — these deploy automatically with the rest of
the site, no separate deploy step, and Vercel's Hobby tier includes them for
free with no billing card on file.

1. Push this repo to GitHub, then import it in https://vercel.com/new.
2. In the Vercel project → **Settings → Environment Variables**, add:
   - `YOUTUBE_API_KEY` = the key from step 2 above (used server-side by
     `/api/search.js`, never shipped to the browser)
   - All the `VITE_FIREBASE_*` values from your `.env`
3. Deploy. That's it — search and playback work immediately, no
   `VITE_SEARCH_API_BASE` needed (it defaults to same-origin `/api`).
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

## Bring-your-own YouTube API key (per user)

Anyone signed in can open their profile menu (top-right) and paste in their
**own** free YouTube Data API key. When set, searches run directly from their
browser against their own quota instead of the shared one — useful if the
shared key ever hits its daily limit. Stored in their own Firestore user
document, never shared with other users (see `firestore.rules`).

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

## Optional: Firebase Cloud Function backend instead

If you'd rather run the search backend on Firebase instead of Vercel (e.g.
you're not deploying to Vercel at all), the equivalent Cloud Function still
exists in `functions/src/index.js`. Note this **requires the Blaze
(pay-as-you-go) plan** to deploy at all, even though usage itself stays free
under normal quotas — that's a Firebase requirement for any Cloud Function,
not a cost of this app. Deploy with `firebase deploy --only functions`, then
set `VITE_SEARCH_API_BASE` to the printed function URL to use it instead of
the default `/api` path.

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
