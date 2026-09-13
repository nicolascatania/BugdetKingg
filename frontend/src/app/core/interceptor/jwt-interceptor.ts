import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth';
import { environment } from '../../../environments/environment';

/**
 * Attaches the bearer token to requests aimed at our own backend only.
 * Any other host (a third-party API, a CDN) must never receive the session token.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  if (token && req.url.startsWith(environment.apiUrl)) {
    const cloned = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
    return next(cloned);
  }

  return next(req);
};
