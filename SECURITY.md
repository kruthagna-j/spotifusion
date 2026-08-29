# Spotifusion Security Baseline

Spotifusion treats security as a release requirement. This document records the baseline controls implemented in the web application and the areas that still require testing against the project's VAPT checklist.

## Implemented baseline

- Firebase ID tokens are verified by the API before protected requests are handled.
- CORS is allow-list based; production origins must be explicitly configured.
- API requests are rate-limited and upstream concurrency is bounded.
- Authentication failures use generic responses and do not expose token contents.
- User-owned Firestore data is scoped by authenticated user ID.
- Frontend production builds do not publish source maps by default.
- Keyboard focus indicators are enabled for interactive controls.

## Browser security headers

The production deployment should send at least:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy` with only required browser capabilities
- A CSP reviewed against Firebase, YouTube playback, fonts, and required API origins

## VAPT status

This file is a baseline, not a penetration-test certification. The 31-item VAPT checklist must be validated against the deployed frontend and API before claiming a pass.
