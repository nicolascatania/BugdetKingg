import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
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
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

/** At least one letter and one digit; length is enforced by the min/max validators. */
const PASSWORD_PATTERN = /^(?=.*\p{L})(?=.*\d).+$/u;

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslocoDirective],
  templateUrl: './register.html',
  styleUrl: './register.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Register {
  registerForm: FormGroup;

  /** Disables the form and shows progress while the request is in flight. */
  readonly submitting = signal(false);

  /** Reveals the password so users can check what they typed. */
  readonly passwordVisible = signal(false);

  constructor(
    private authService: AuthService,
    private fb: FormBuilder,
    private router: Router,
    private notificationService: NotificationService,
    private transloco: TranslocoService,
  ) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.email, Validators.required]],
      // Mirrors the backend policy (RegisterRequest): 8-72 chars, at least one letter and one digit.
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72), Validators.pattern(PASSWORD_PATTERN)]],
      name: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
    });
  }

  togglePasswordVisibility(): void {
    this.passwordVisible.update((visible) => !visible);
  }

  /** True once the field has been touched and is invalid — drives inline errors. */
  showError(controlName: string): boolean {
    const control = this.registerForm.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  onSubmit(): void {
    if (this.registerForm.invalid || this.submitting()) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.submitting.set(false);

        const errorMessage =
          typeof err.error === 'string'
            ? err.error
            : err.error?.message || this.transloco.translate('auth.register.failed');

        this.notificationService.error(errorMessage);
      },
    });
  }
}
