import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  NgZone,
  ViewChild,
  signal,
} from '@angular/core';
import { AuthService } from '../../../core/services/auth';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../core/services/NotificationService';
import { environment } from '../../../../environments/environment';

/**
 * Minimal shape of the `google.accounts.id` API used here. Google Identity
 * Services attaches itself to `window.google`; no `@types` package ships it.
 */
declare const google: {
  accounts: {
    id: {
      initialize(config: { client_id: string; callback: (response: { credential: string }) => void }): void;
      renderButton(parent: HTMLElement, options: Record<string, unknown>): void;
    };
  };
};

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login implements AfterViewInit {
  loginForm: FormGroup;

  /** Feature flag: email/password sign-in is disabled server-side for now (Google-only). */
  readonly enableLocalAuth = environment.enableLocalAuth;

  /** Disables the form and shows progress while the request is in flight. */
  readonly submitting = signal(false);

  /** Reveals the password so users can check what they typed. */
  readonly passwordVisible = signal(false);

  @ViewChild('googleButton') googleButton?: ElementRef<HTMLDivElement>;

  constructor(
    private authService: AuthService,
    private fb: FormBuilder,
    private router: Router,
    private notificationService: NotificationService,
    private ngZone: NgZone,
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.email, Validators.required]],
      password: ['', [Validators.required]],
    });
  }

  ngAfterViewInit(): void {
    this.renderGoogleButtonWhenReady();
  }

  /**
   * The Google Identity Services script tag in index.html loads `async defer`,
   * so it may not be ready yet when this component mounts. Polls briefly
   * rather than assuming a load order between the two.
   */
  private renderGoogleButtonWhenReady(attemptsLeft = 20): void {
    if (typeof google !== 'undefined' && this.googleButton) {
      this.initializeGoogleButton();
      return;
    }
    if (attemptsLeft <= 0) {
      this.notificationService.error('Could not load Google sign-in. Please refresh the page.');
      return;
    }
    setTimeout(() => this.renderGoogleButtonWhenReady(attemptsLeft - 1), 150);
  }

  private initializeGoogleButton(): void {
    if (!environment.googleClientId) {
      this.notificationService.error('Google sign-in is not configured.');
      return;
    }

    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      // Google invokes this outside Angular's zone; re-enter it so signals/navigation update the view.
      callback: (response) => this.ngZone.run(() => this.onGoogleCredential(response.credential)),
    });

    google.accounts.id.renderButton(this.googleButton!.nativeElement, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      shape: 'pill',
      width: 320,
    });
  }

  private onGoogleCredential(idToken: string): void {
    this.submitting.set(true);

    this.authService.loginWithGoogle(idToken).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.submitting.set(false);

        const errorMessage =
          typeof err.error === 'string'
            ? err.error
            : err.error?.message || 'Google sign-in failed, please try again';

        this.notificationService.error(errorMessage);
      },
    });
  }

  togglePasswordVisibility(): void {
    this.passwordVisible.update((visible) => !visible);
  }

  /** True once the field has been touched and is invalid — drives inline errors. */
  showError(controlName: string): boolean {
    const control = this.loginForm.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  /**
   * Email/password submit handler. Kept working end-to-end against the backend's
   * gated /auth/login (see AuthController.localAuthEnabled) so this path can be
   * re-enabled by flipping `environment.enableLocalAuth` and the backend flag
   * together — nothing to rebuild here.
   */
  onSubmit(): void {
    if (this.loginForm.invalid || this.submitting()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.submitting.set(false);

        const errorMessage =
          typeof err.error === 'string'
            ? err.error
            : err.error?.message ||
              'Login error, please contact an administrator';

        this.notificationService.error(errorMessage);
      },
    });
  }
}
