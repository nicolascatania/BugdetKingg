import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  NgZone,
  OnDestroy,
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
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

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
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslocoDirective],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login implements AfterViewInit, OnDestroy {
  loginForm: FormGroup;

  /** Feature flag: email/password sign-in is disabled server-side for now (Google-only). */
  readonly enableLocalAuth = environment.enableLocalAuth;

  /** Disables the form and shows progress while the request is in flight. */
  readonly submitting = signal(false);

  /** Reveals the password so users can check what they typed. */
  readonly passwordVisible = signal(false);

  @ViewChild('googleButton') googleButton?: ElementRef<HTMLDivElement>;

  /** Last width handed to Google, so a resize that leaves it unchanged does not redraw. */
  private lastRenderedWidth = 0;

  private resizeObserver?: ResizeObserver;

  constructor(
    private authService: AuthService,
    private fb: FormBuilder,
    private router: Router,
    private notificationService: NotificationService,
    private ngZone: NgZone,
    private transloco: TranslocoService,
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
      this.notificationService.error(this.transloco.translate('auth.login.googleLoadFailed'));
      return;
    }
    setTimeout(() => this.renderGoogleButtonWhenReady(attemptsLeft - 1), 150);
  }

  private initializeGoogleButton(): void {
    if (!environment.googleClientId) {
      this.notificationService.error(this.transloco.translate('auth.login.googleNotConfigured'));
      return;
    }

    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      // Google invokes this outside Angular's zone; re-enter it so signals/navigation update the view.
      callback: (response) => this.ngZone.run(() => this.onGoogleCredential(response.credential)),
    });

    this.renderGoogleButton();
    this.observeAvailableWidth();
  }

  /**
   * Google only accepts a fixed pixel width, so the button has to be redrawn
   * whenever the card resizes — rotating a phone, or resizing the window —
   * otherwise it keeps a width the card can no longer fit and overflows it.
   * Only the DOM is touched here, so there is no need to re-enter Angular's zone.
   */
  private observeAvailableWidth(): void {
    this.resizeObserver = new ResizeObserver(() => {
      if (this.measureButtonWidth() !== this.lastRenderedWidth) {
        this.renderGoogleButton();
      }
    });
    this.resizeObserver.observe(this.googleButton!.nativeElement);
  }

  /**
   * (Re)draws Google's button at the current width.
   *
   * Always the light `outline` variant, in dark mode too. The personalised
   * button Google shows to a browser with an active session sits on a white
   * box that `filled_black` does not cover, which left a white frame around a
   * black button on the dark theme. A light button on a dark surface is the
   * variant Google itself ships for dark backgrounds, and it hides that box.
   */
  private renderGoogleButton(): void {
    const host = this.googleButton!.nativeElement;
    const width = this.measureButtonWidth();

    // renderButton appends rather than replaces; drop the previous one so a
    // resize does not stack two buttons.
    host.replaceChildren();

    google.accounts.id.renderButton(host, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      shape: 'pill',
      logo_alignment: 'center',
      width,
    });

    this.lastRenderedWidth = width;
  }

  /**
   * Width available inside the card. Google clamps the button to 400px and
   * refuses to go under 200px, so the measurement is clamped to that range;
   * `overflow-hidden` on the host contains the remainder on a screen too
   * narrow even for the minimum.
   */
  private measureButtonWidth(): number {
    const available = this.googleButton?.nativeElement.clientWidth ?? 0;
    return Math.round(Math.min(400, Math.max(200, available || 320)));
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
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
            : err.error?.message || this.transloco.translate('auth.login.googleFailed');

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
            : err.error?.message || this.transloco.translate('auth.login.loginFailed');

        this.notificationService.error(errorMessage);
      },
    });
  }
}
