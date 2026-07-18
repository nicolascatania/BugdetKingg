import { ChangeDetectionStrategy, Component } from '@angular/core';
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

@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  loginForm: FormGroup;
  constructor(
    private authService: AuthService,
    private fb: FormBuilder,
    private router: Router,
    private notificationService: NotificationService,
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.email, Validators.required]],
      password: ['', [Validators.required]],
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.authService.login(this.loginForm.value).subscribe({
      next: () => this.router.navigate(['/home']),
      error: (err) => {
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
