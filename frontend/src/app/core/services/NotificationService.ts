import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

type NotificationType = 'success' | 'error' | 'warning' | 'info';

@Injectable({ providedIn: 'root' })
export class NotificationService {

  private readonly baseConfig: MatSnackBarConfig = {
    duration: 3500,
    horizontalPosition: 'right',
    verticalPosition: 'top',
  };

  constructor(private snackBar: MatSnackBar) {}

  success(message: string): void {
    this.open(message, 'success');
  }

  error(message: string): void {
    this.open(message, 'error', 5000);
  }

  warning(message: string): void {
    this.open(message, 'warning');
  }

  info(message: string): void {
    this.open(message, 'info');
  }

  private open(
    message: string,
    type: NotificationType,
    duration?: number
  ): void {
    this.snackBar.open(message, 'Close', {
      ...this.baseConfig,
      duration: duration ?? this.baseConfig.duration,
      panelClass: this.getPanelClass(type),
    });
  }

  private getPanelClass(type: NotificationType): string[] {
    switch (type) {
      case 'success':
        return ['snackbar-success'];
      case 'error':
        return ['snackbar-error'];
      case 'warning':
        return ['snackbar-warning'];
      case 'info':
        return ['snackbar-info'];
      default:
        return [];
    }
  }
}
