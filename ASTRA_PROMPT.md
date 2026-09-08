# Paste this into Astra

Work directly on the connected `kruthagna-j/spotifusion` repository.

Read `ASTRA_BUILD_SPEC.md` first and treat it as the acceptance specification.

Do not just redesign the UI. Build a genuinely functioning production web player and native Android app. Inspect the whole repository before changing architecture. Preserve useful working features, but replace broken implementations when necessary.

Your workflow must be:

1. Inspect repository structure and existing web/Android/backend architecture.
2. Identify compile errors, runtime errors, dead features, fake/mock functionality, API failures and security problems.
3. Make a concrete implementation plan.
4. Implement the web player end-to-end.
5. Implement the Android app end-to-end.
6. Keep one coherent backend/API contract between clients.
7. Put all provider secrets server-side. Never embed an Astra API key in web JavaScript or Android source.
8. Use `ASTRA_API_KEY` and `ASTRA_API_BASE_URL` only on the server if Astra is required.
9. Build the web application.
10. Build the Android application.
11. Run tests/static checks and exercise the important player flows.
12. Fix every error you discover.
13. Do not mark work complete merely because the code looks correct.
14. Commit the completed implementation to GitHub.
15. Give a final report listing builds/tests actually run and any remaining external dependency.

Important: do not expose or repeat any secret that may already have been pasted into a prompt. If an API key has been exposed, tell the owner to rotate it and use a fresh environment secret.

Definition of done: a user can actually search for music, start playback, control playback, manage a queue, use playlists/favorites/history, view lyrics, use settings/equalizer/sleep timer, use local music, and use offline downloads where supported, on both web and Android. All major buttons must work.
