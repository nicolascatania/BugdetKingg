import { ChangeDetectionStrategy, Component, inject, Inject, signal } from '@angular/core';
import { EditAccountModal } from "../../components/edit-account-modal/edit-account-modal";
import { AccountDTO } from '../../interfaces/AccountDTO.interfaces';
import { AccountService } from '../../services/AccountService';
import { NotificationService } from '../../../../core/services/NotificationService';
import { CommonModule } from '@angular/common';
import { TransactionType } from '../../../../shared/models/TransactionType.enum';

@Component({
  selector: 'app-account-list',
  imports: [EditAccountModal, CommonModule],
  templateUrl: './account-list.html',
  styleUrl: './account-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountList {

  accountService = inject(AccountService);
  isModalOpen = signal(false);
  selectedAccount = signal<AccountDTO | null>(null);
  accounts = this.accountService.accounts;

  constructor(
    private ns: NotificationService
  ) { }


  openAccountModal(account: AccountDTO | null) {
    this.selectedAccount.set(account);
    this.isModalOpen.set(true);
  }


  deleteAccount(account: AccountDTO) {
    this.accountService.delete(account.id).subscribe({
      next: () => {
        this.ns.success('Account deleted successfully');
      },
      error: (err: string) => {
        this.ns.error(err);
      }
    });
  }

  onAccountModalClose($event: boolean) {
    this.isModalOpen.set(false);
  }
}
