import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, inject, input, Output, signal } from '@angular/core';
import { AccountService } from '../../../accounts/services/AccountService';

@Component({
  selector: 'heading',
  imports: [CommonModule],
  templateUrl: './heading.html',
  styleUrl: './heading.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Heading {

  private accountService = inject(AccountService);

  /**
   * Fired when the user wants to create a new account.
   */
  @Output() newAccount = new EventEmitter<void>();
  @Output() newTransaction = new EventEmitter<void>();
  
  totalBalance = this.accountService.totalBalance;

  openNewAccountModal(): void {
    this.newAccount.emit();
  }

  openNewTransactionModal(): void {
    this.newTransaction.emit();
  }

}
