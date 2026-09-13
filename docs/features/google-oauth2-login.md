# Google OAuth2 Login

Sign-in is Google-only. The old email/password flow stays fully implemented but
disabled behind a flag, so it can be turned back on later without a rewrite.

## What it does

- `POST /auth/google` — verifies a Google ID token and returns this app's own JWT
  (`AuthResponse`), the same shape `/auth/login` and `/auth/register` return.
- First sign-in for an email creates the `AppUser` automatically (no separate
  register step); a returning Google subject reuses its existing user.
- `POST /auth/login` and `POST /auth/register` still exist and still work end to
  end, but return `403 FORBIDDEN` unless `app.auth.local-enabled=true`. Default is
  `false` in every environment except tests.
- Frontend: the login page renders Google's own "Sign in with Google" button
  (Google Identity Services). The manual email/password form and the "Create an
  account" link are hidden behind `environment.enableLocalAuth` (`false` by
  default) — same on/off switch as the backend flag, kept in sync manually.

## How it works

- `GoogleTokenInfoClient` (an `@HttpExchange` interface, same pattern as
  `DollarApiClient`/`ArgentinaAPIClient`) calls Google's `tokeninfo` endpoint to
  verify the ID token's signature, issuer and expiry — no JWKS caching to
  maintain locally.
- `GoogleAuthService.verify` additionally checks the token's `aud` claim against
  `app.auth.google.client-id` (rejects tokens minted for a different OAuth
  client) and requires `email_verified`. Fails closed if the client id is unset.
- `AuthController.google`: `findByProviderId` (Google's `sub`) first; if not
  found, falls back to `findByEmail` to link a Google sign-in onto a pre-existing
  local account (covers users created before this change); otherwise creates a
  new `AppUser` with `authProvider = GOOGLE` and `passwordHash = null`.
- `AppUser` gained `authProvider` (`LOCAL` | `GOOGLE`) and `providerId` (Google's
  `sub`, unique, nullable). `passwordHash` is now nullable — a `GOOGLE` user has
  none.
- `app.auth.local-enabled` (env var `AUTH_LOCAL_ENABLED`) gates `/login` and
  `/register` server-side; `GlobalExceptionHandler` maps the resulting
  `LocalAuthDisabledException` to `403`. Test resources set it to `true` so the
  existing register/login-shaped tests keep exercising that path.

## Setup (per environment)

1. Google Cloud Console -> APIs & Services -> Credentials -> create an OAuth
   client ID (Web application). Add the frontend origin(s) to "Authorized
   JavaScript origins".
2. Backend: set `GOOGLE_CLIENT_ID` to that client id.
3. Frontend: set `googleClientId` in `src/environments/environment*.ts` to the
   same value.

## Files

`security/controller/AuthController`, `security/service/GoogleAuthService`,
`security/enumerator/AuthProvider`, `security/dto/GoogleAuthRequest`,
`client/GoogleTokenInfoClient`, `dto/GoogleTokenInfoDTO`,
`exception/InvalidGoogleTokenException`, `exception/LocalAuthDisabledException`,
`model/AppUser`, `repository/AppUserRepository`.

Frontend: `core/services/auth.ts`, `features/login/login/login.ts`,
`features/login/login/login.html`, `index.html` (Google Identity Services
script), `environments/environment*.ts`.

## Not done

- No account-linking UI — linking a legacy local account to Google happens
  silently by email match. If that email was never verified locally, this is a
  soft spot: revisit if local auth is ever re-enabled for real.
- No refresh-token / silent re-auth handling beyond what this app already does
  for its own JWT.
- `LOCAL` path has no test coverage for the "flag re-enabled" case beyond what
  already existed — re-verify manually if `app.auth.local-enabled` is ever
  flipped back to `true` in a real environment.
