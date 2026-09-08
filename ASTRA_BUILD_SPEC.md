# SpotiFusion — Astra Production Build Specification

## Goal
Turn this repository into a production-ready music platform with two first-class clients:

1. Responsive web player
2. Native Android app

Do not treat the existing implementation as final. Preserve useful working functionality, but refactor or replace code where necessary. Do not merely improve screenshots or mock interactions.

## Non-negotiable quality bar
- Every visible button must perform a real action.
- No fake loading states that never resolve.
- No dead navigation routes.
- No placeholder API responses in production paths.
- No API keys or secrets in browser code, Android source, APK resources, Git history, or committed `.env` files.
- All network failures must have useful user-facing error/retry states.
- All playback actions must remain usable while navigating between screens.
- Build both clients and fix compilation/runtime errors before declaring completion.
- Test the production web build and Android debug/release build.

## Web player
Implement and verify:
- Home/discover page
- Search with debounced requests and pagination/infinite loading
- Track, album and artist browsing
- Play/pause, previous/next, seek, volume
- Queue management
- Shuffle and repeat modes
- Favorites/likes
- Playlists: create, rename, delete, add/remove tracks
- Recently played/history
- Lyrics with synchronized highlighting when timestamps are available
- Equalizer UI and real audio processing where browser APIs support it
- Sleep timer
- Local audio files without uploading them to the server
- Offline/PWA caching for supported assets and downloaded tracks where legally/technically supported
- Responsive layout for desktop and mobile
- Dark and light themes
- Accessible keyboard controls and semantic buttons
- Persistent player state across navigation/reloads

## Android app
Implement and verify:
- Native Jetpack Compose UI
- Home, Search, Fusion, Library and Equalizer sections
- Full now-playing screen
- Background Media3 playback
- Notification/media-session controls
- Bluetooth/headset media controls
- Play/pause, seek, next/previous, queue
- Shuffle/repeat
- Favorites and playlists
- History
- Local music scanning with runtime permissions
- Offline downloads stored privately on-device
- Download progress and completion/error notifications
- Lyrics and synchronized scrolling
- Equalizer controls
- Sleep timer
- Shake-to-skip setting
- Persistent settings
- Dark/light theme
- Proper empty/error/loading states

## Backend/API
- Keep provider credentials server-side.
- Use environment variables for all secrets.
- Add a clean API layer for search/catalog/stream resolution/lyrics where needed.
- Validate and normalize provider responses.
- Add request timeouts, retries with backoff, caching and rate-limit handling.
- Configure CORS only for trusted production origins.
- Do not expose provider API keys to web or Android clients.

## Astra integration
If the supplied Astra service is the intended provider, integrate it only through the backend. The Android and browser clients must call our backend rather than embedding an Astra key. Use an environment variable such as `ASTRA_API_KEY` and document the required endpoint configuration without committing the secret.

## Security
- Audit the repository for leaked credentials before finalizing.
- Remove/revoke any previously exposed API keys.
- Add/update `.gitignore` for `.env`, local secrets and build artifacts.
- Add dependency/security checks to CI.
- Never print secrets in logs.

## Testing
Create/maintain automated checks for:
- Web build
- Android compilation
- Player state transitions
- Search failure/retry
- Offline download/remove
- Playlist CRUD
- Theme persistence
- Settings persistence
- Deep navigation without losing the current player

Use the existing GitHub Actions workflow where possible. If it fails, diagnose the actual error and fix the source rather than weakening the workflow.

## Definition of done
Astra must:
1. Inspect the complete repository.
2. Produce a short implementation plan.
3. Implement the missing functionality.
4. Build the web client.
5. Build the Android client.
6. Run tests/static checks.
7. Fix all discovered compile/runtime errors.
8. Verify production configuration and secret handling.
9. Commit the finished work to the repository.
10. Report exactly what was tested and any remaining external dependency that prevents verification.
