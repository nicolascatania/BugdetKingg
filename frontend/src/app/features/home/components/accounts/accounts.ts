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
  
  // Signal local para el skeleton
  loading = signal(true);

  constructor() {
    // Cuando las cuentas cambian, si ya hay datos, apagamos el skeleton
    effect(() => {
      if (this.accounts().length >= 0) {
        this.loading.set(false);
      }
    });
  }
}