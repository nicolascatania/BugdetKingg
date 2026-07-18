import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import {
  LoginRequest,
  RegisterRequest,
} from '../../features/login/interfaces/login.interface';
import { AuthResponse } from '../interfaces/AuthResponse.interface';
import { jwtDecode } from 'jwt-decode';

export enum Role {
  ADMIN = 'ROLE_ADMIN',
  USER = 'ROLE_USER',
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = 'http://localhost:8080/auth';
  private tokenKey = 'jwt_token';
  public loggedIn$ = new BehaviorSubject<boolean>(this.isLoggedIn());

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

  logout() {
    localStorage.removeItem(this.tokenKey);
    this.loggedIn$.next(false);
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
