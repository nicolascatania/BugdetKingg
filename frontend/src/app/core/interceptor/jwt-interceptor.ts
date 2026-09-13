import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth';
import { environment } from '../../../environments/environment';

/**
 * Attaches the bearer token to requests aimed at our own backend only.
 * Any other host (a third-party API, a CDN) must never receive the session token.
 *
 * The public `/auth/**` endpoints are skipped: a leftover token belongs to the
 * previous session, and sign-in must not carry one user's credentials into
 * another user's request.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();
  const isAuthEndpoint = req.url.startsWith(`${environment.apiUrl}/auth/`);

  if (token && !isAuthEndpoint && req.url.startsWith(environment.apiUrl)) {
    const cloned = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
    return next(cloned);
  }

  return next(req);
};
