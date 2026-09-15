import { TranslocoDirective } from '@jsverse/transloco';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccountService } from '../../../accounts/services/AccountService';

@Component({
  selector: 'accounts',
  standalone: true,
  imports: [CommonModule, TranslocoDirective],
  templateUrl: './accounts.html',
  styleUrl: './accounts.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Accounts {
  private accountService = inject(AccountService);
  accounts = this.accountService.accounts;

  /**
   * Request state owned by the service. The previous local effect checked
   * `accounts().length >= 0`, which is true for an empty array too, so it
   * cleared the flag on its first run and the skeleton never actually showed.
   */
  loading = this.accountService.loading;
}