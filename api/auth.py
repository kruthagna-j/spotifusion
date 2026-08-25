"""
Verifies Firebase ID tokens so /api/search and /api/song require a signed-in
Spotifusion account — this is enforced server-side (not just hidden behind a
frontend sign-in wall, which anyone could bypass by calling the API URL
directly).

Deliberately does NOT use the firebase-admin SDK, which needs a service
account credential file/secret to manage. Firebase ID tokens are standard
signed JWTs, so we verify them directly against Google's public signing
keys using just the (non-secret) Firebase project ID — one less credential
to provision/rotate on a free-tier host.
"""
import os
import logging

import jwt
from jwt import PyJWKClient
from fastapi import Request, HTTPException

logger = logging.getLogger("spotifusion.auth")

FIREBASE_PROJECT_ID = os.getenv("FIREBASE_PROJECT_ID")
JWKS_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"

_jwk_client = PyJWKClient(JWKS_URL) if FIREBASE_PROJECT_ID else None


def require_firebase_user(request: Request) -> dict:
    """FastAPI dependency: raises 401 unless a valid Firebase ID token is
    present, otherwise returns the decoded token payload (uid, email, ...)."""
    if not FIREBASE_PROJECT_ID:
        # Fails closed, not open: if the server isn't configured with a
        # project id, nobody gets through, rather than everybody.
        logger.error("FIREBASE_PROJECT_ID is not set — refusing all requests.")
        raise HTTPException(status_code=500, detail="Server authentication is not configured.")

    authz = request.headers.get("authorization", "")
    if not authz.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Sign in to search and stream music.")

    token = authz[len("Bearer "):].strip()
    try:
        signing_key = _jwk_client.get_signing_key_from_jwt(token)
        payload = jwt.decode(
            token,
            signing_key.key,
            algorithms=["RS256"],
            audience=FIREBASE_PROJECT_ID,
            issuer=f"https://securetoken.google.com/{FIREBASE_PROJECT_ID}",
        )
    except Exception as exc:
        logger.info("Rejected request with invalid/expired token: %s", exc)
        raise HTTPException(status_code=401, detail="Your session has expired. Please sign in again.") from exc

    return payload
