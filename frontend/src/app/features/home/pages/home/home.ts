import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { Heading } from "../../components/heading/heading";
import { Accounts } from '../../components/accounts/accounts';
import { MonthlySummary } from "../../components/monthly-summary/monthly-summary";
import { LastMoves } from "../../components/last-moves/last-moves";
import { AccountService } from '../../../accounts/services/AccountService';
import { AccountDTO } from '../../../accounts/interfaces/AccountDTO.interfaces';
import { EditAccountModal } from '../../../accounts/components/edit-account-modal/edit-account-modal';
import { TransactionDTO } from '../../../transactions/interfaces/TransactionDTO.interface';
import { EditTransaction } from '../../../transactions/components/edit-transaction/edit-transaction';

@Component({
  selector: 'home',
  imports: [Heading, Accounts, MonthlySummary, LastMoves, EditAccountModal, EditTransaction],
  templateUrl: './home.html',
  styleUrl: './home.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home {

  private accountService = inject(AccountService);
  accounts = this.accountService.accounts;

  isAccountModalOpen = false;
  isTransactionModalOpen = false;

  selectedAccount = null;
  selectedTransaction = null;

  constructor() {
    this.accountService.loadAccounts();
  }


  /**
   * Opens the modal to create a new account.
   */
  openNewAccountModal(): void {
    this.selectedAccount = null;
    this.isAccountModalOpen = true;
  }

  /**
   * Handles modal close event.
   * Refreshes the accounts list if an operation was successful.
   */
  onModalClosed(success: boolean): void {
    this.isAccountModalOpen = false;
  }

  openNewTransactionModal(): void {
    this.selectedTransaction = null;
    this.isTransactionModalOpen = true;
  }

  onTransactionModalClosed(success: boolean): void {
    this.isTransactionModalOpen = false;
  }

}
