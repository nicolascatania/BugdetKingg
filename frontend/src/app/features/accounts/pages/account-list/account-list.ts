import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { EditAccountModal } from '../../components/edit-account-modal/edit-account-modal';
import { AccountDTO } from '../../interfaces/AccountDTO.interfaces';
import { AccountService } from '../../services/AccountService';
import { NotificationService } from '../../../../core/services/NotificationService';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-account-list',
  imports: [EditAccountModal, CommonModule],
  templateUrl: './account-list.html',
  styleUrl: './account-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountList {
  private accountService = inject(AccountService);
  private ns = inject(NotificationService);

  isModalOpen = signal(false);
  selectedAccount = signal<AccountDTO | null>(null);

  accounts = computed(() => this.accountService.accounts());
  loading = computed(() => this.accounts().length === 0);

  openAccountModal(account: AccountDTO | null) {
    this.selectedAccount.set(account);
    this.isModalOpen.set(true);
  }

  deleteAccount(account: AccountDTO) {
    this.accountService.delete(account.id).subscribe({
      next: () => {
        this.ns.success('Account deleted successfully');
      },
      error: (err) => {
        this.ns.error(err.error.message);
      },
    });
  }

  onAccountModalClose($event: boolean) {
    this.isModalOpen.set(false);
  }
}
