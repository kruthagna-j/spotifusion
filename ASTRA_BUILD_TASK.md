# Astra task — production Spotifusion rebuild

Start from the current `main` branch. Read `ASTRA_BUILD_SPEC.md` and `ASTRA_PROMPT.md` before making changes.

The target is a real production-ready web player plus native Android application. Do not stop at UI work. Build, test and fix both clients. Keep secrets server-side and use `ASTRA_API_KEY`/`ASTRA_API_BASE_URL` only in backend environments if Astra is used.

Before finishing, verify the actual web build and Android build, exercise the main playback flows, and commit the result.