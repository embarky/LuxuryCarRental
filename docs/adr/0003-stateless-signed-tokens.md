# 3. Stateless signed tokens for API authentication

**Status:** accepted, with a known limitation

## Context

The REST API needs to know who calls it. The web layer is its only client today, but the API is
designed so that other clients could use it. Keeping sessions on the API server would tie clients to
one server instance.

## Decision

At login the API returns a token `base64url(role|id|expiry).signature`, signed with HMAC-SHA256 and a
secret from the environment (`API_TOKEN_SECRET`), valid for 8 hours. A JAX-RS filter checks every
request and **denies by default**:

- endpoints marked `@Public` are open,
- `@LoggedIn` endpoints accept any valid token, and each one checks that customers only touch their
  own data (`Caller.requireSelfOrAdmin`),
- everything else is for administrators.

A new endpoint without an annotation is therefore admin-only, never accidentally public.

## Consequences

- No session storage on the API; any instance with the same secret can check a token.
- Passwords are only sent at login; the web layer keeps the token in the user's HTTP session.
- **A token cannot be revoked** before it expires. Logging out forgets it in the browser only, and a
  blocked customer keeps access until expiry. Fixes, in order of effort: shorter lifetime with refresh
  tokens, a token version stored per user, or a standard solution (OpenID Connect with Keycloak).
- The format is home-made but small and tested; JWT would add interoperability, not security, here.
