import { ChangeDetectionStrategy, Component, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccountService } from '../../../accounts/services/AccountService';

@Component({
  selector: 'accounts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './accounts.html',
  styleUrl: './accounts.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Accounts {
  private accountService = inject(AccountService);
  accounts = this.accountService.accounts;
  
  // Local signal driving the skeleton placeholders.
  loading = signal(true);

  constructor() {
    // As soon as the accounts signal emits, the skeleton can be dismissed.
    effect(() => {
      if (this.accounts().length >= 0) {
        this.loading.set(false);
      }
    });
  }
}