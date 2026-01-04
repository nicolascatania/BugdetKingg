import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'heading',
  imports: [],
  templateUrl: './heading.html',
  styleUrl: './heading.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Heading {

  /**
   * Fired when the user wants to create a new account.
   */
  @Output() newAccount = new EventEmitter<void>();
  @Output() newTransaction = new EventEmitter<void>();

  openNewAccountModal(): void {
    this.newAccount.emit();
  }

  openNewTransactionModal(): void {
    this.newTransaction.emit();
  }

}
