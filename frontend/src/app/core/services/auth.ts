import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import {
  LoginRequest,
  RegisterRequest,
} from '../../features/login/interfaces/login.interface';
import { AuthResponse } from '../interfaces/AuthResponse.interface';
import { UserProfile } from '../interfaces/UserProfile.interface';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../../environments/environment';

export enum Role {
  ADMIN = 'ROLE_ADMIN',
  USER = 'ROLE_USER',
}

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

  logout() {
    localStorage.removeItem(this.tokenKey);
    this.loggedIn$.next(false);
    this.currentUserSignal.set(null);
    this.currentUserRequested = false;
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
