import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { AccountDTO } from '../../../accounts/interfaces/AccountDTO.interfaces';
import { CommonModule } from '@angular/common';
import { AccountService } from '../../../accounts/services/AccountService';

@Component({
  selector: 'accounts',
  imports: [CommonModule],
  templateUrl: './accounts.html',
  styleUrl: './accounts.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Accounts {
  private accountService = inject(AccountService);
  accounts = this.accountService.accounts;

  constructor() { }
}