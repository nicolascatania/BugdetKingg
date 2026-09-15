import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import {
  LoginRequest,
  RegisterRequest,
} from '../../features/login/interfaces/login.interface';
import { AuthResponse } from '../interfaces/AuthResponse.interface';
import { UpdateProfileRequest, UserProfile } from '../interfaces/UserProfile.interface';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../../environments/environment';

export enum Role {
  ADMIN = 'ROLE_ADMIN',
  USER = 'ROLE_USER',
}

/**
 * Minimal shape of the `google.accounts.id` API used on sign-out. Google
 * Identity Services attaches itself to `window.google`; no `@types` package
 * ships it.
 */
declare const google: {
  accounts: { id: { disableAutoSelect(): void } };
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private tokenKey = 'jwt_token';
  public loggedIn$ = new BehaviorSubject<boolean>(this.isLoggedIn());

  private readonly currentUserSignal = signal<UserProfile | null>(null);
  /** The signed-in user's own profile (name, picture) — null until {@link ensureCurrentUserLoaded} resolves. */
  readonly currentUser = this.currentUserSignal.asReadonly();
  private currentUserRequested = false;

  constructor(private http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap((res) => {
        localStorage.setItem(this.tokenKey, res.token);
        this.loggedIn$.next(true);
      }),
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, request)
      .pipe(
        tap((res) => {
          localStorage.setItem(this.tokenKey, res.token);
          this.loggedIn$.next(true);
        }),
      );
  }

  /**
   * Exchanges a Google ID token (obtained client-side via Google Identity
   * Services) for this app's own JWT. The Google token itself is never stored.
   */
  loginWithGoogle(idToken: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/google`, { idToken })
      .pipe(
        tap((res) => {
          localStorage.setItem(this.tokenKey, res.token);
          this.loggedIn$.next(true);
        }),
      );
  }

  /**
   * Ends the session with a full page load rather than a router navigation.
   *
   * Session data does not live in this service alone: other root-provided
   * services keep the signed-in user's data in signals (account lists, for one),
   * and a singleton created under user A still holds A's data when user B signs
   * in. Recreating the injector is the only thing that reliably clears all of
   * it. Navigating to `document.baseURI` honours the app's `<base href>`, and
   * the empty route redirects to /login.
   */
  logout() {
    localStorage.removeItem(this.tokenKey);
    // Clear in-memory state too: the reload is not instant and the view can
    // still repaint with the previous user's profile before it lands.
    this.loggedIn$.next(false);
    this.currentUserSignal.set(null);
    this.currentUserRequested = false;

    // Without this Google keeps silently offering the account that just signed
    // out the next time the sign-in button is rendered.
    if (typeof google !== 'undefined') {
      google.accounts.id.disableAutoSelect();
    }

    window.location.href = document.baseURI;
  }

  /**
   * Fetches the signed-in user's profile once and caches it in {@link currentUser}.
   * Safe to call from every component that needs it (e.g. on mount) — a repeat
   * call before the first response resolves, or after it already has, is a no-op.
   */
  ensureCurrentUserLoaded(): void {
    if (this.currentUserRequested || !this.isLoggedIn()) {
      return;
    }
    this.currentUserRequested = true;

    this.http.get<UserProfile>(`${environment.apiUrl}/me`).subscribe({
      next: (profile) => this.currentUserSignal.set(profile),
      error: () => {
        this.currentUserRequested = false;
      },
    });
  }

  /**
   * Partially updates the signed-in user's profile and refreshes
   * {@link currentUser} with what the backend stored, so every reader (sidebar,
   * region-aware widgets) sees the change without a refetch.
   */
  updateProfile(patch: UpdateProfileRequest): Observable<UserProfile> {
    return this.http
      .patch<UserProfile>(`${environment.apiUrl}/me`, patch)
      .pipe(tap((profile) => this.currentUserSignal.set(profile)));
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    const token = this.getToken();
    if (!token) return false;

    try {
      const decoded: any = jwtDecode(token);
      const roles = decoded.roles || [];
      return roles.includes(Role.ADMIN);
    } catch (e) {
      return false;
    }
  }
}
