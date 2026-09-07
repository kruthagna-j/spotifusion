# SpotiFusion Android

Minimal music player built from the current Google AI Studio Android project.

AI Studio app: https://ai.studio/apps/9882d535-c2a3-49b0-b19b-368e50c9f000

## Highlights

- Dark AMOLED and pure white light themes
- Persistent user theme selection
- Minimal Material 3 UI with real vector controls
- Media3 background playback and media session
- Play/pause, seek, next/previous, shuffle, repeat and volume
- Crossfade playback
- 5-band equalizer with persistent settings
- Synced lyrics with configurable auto-scroll
- High-FPS visualizer mode
- Local music library and playlists
- Likes and listening history
- Offline downloads and offline playback
- Download notifications with Android 13+ permission handling
- Real cache size calculation and cache clearing
- Quality-aware online stream resolution

## Run locally

1. Open this project in Android Studio.
2. Let Android Studio sync the Gradle project.
3. Configure Firebase/Gemini credentials as required by the project.
4. Run the `app` configuration on an emulator or physical Android device.

The current repository also contains the existing Spotifusion web player and API under `src/` and `api/` respectively.
