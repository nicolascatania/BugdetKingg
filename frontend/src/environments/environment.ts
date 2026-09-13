export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  // Google OAuth2 client id (Google Cloud Console -> Credentials -> OAuth client ID -> Web application).
  googleClientId: '',
  // Email/password sign-in is disabled backend-side (see AuthController.localAuthEnabled); this
  // just hides the manual form/link so the UI does not offer a path the server will reject.
  // Flip both together if the LOCAL fallback is ever turned back on.
  enableLocalAuth: false,
};
