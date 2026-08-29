# Spotifusion Release Gates

Before production release:

1. `npm run build` succeeds.
2. `npm run lint` succeeds with no new errors.
3. Main routes render without console exceptions.
4. Playback controls work on desktop and mobile.
5. Layout is verified at 360x800 and other supported breakpoints.
6. UTF-8 text is clean; no mojibake such as `Â`, `â`, `â€™`, or stray replacement characters.
7. Production CORS contains only intended origins.
8. Security headers are present on the deployed site.
9. Authentication and per-user access controls are tested.
10. The VAPT checklist is tested against the deployed frontend/API.
