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
import { RevealDirective } from '../../../../shared/directives/reveal.directive';
import { ImportAccounts } from '../../components/import-accounts/import-accounts';
import { AccountImportExportService } from '../../services/account-import-export-service';

@Component({
  selector: 'app-account-list',
  standalone: true,
  imports: [EditAccountModal, CommonModule, RevealDirective, ImportAccounts],
  templateUrl: './account-list.html',
  styleUrl: './account-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountList {
  private accountService = inject(AccountService);
  private ns = inject(NotificationService);
  private importExportService = inject(AccountImportExportService);

  isModalOpen = signal(false);
  isImportModalOpen = signal(false);
  selectedAccount = signal<AccountDTO | null>(null);

  accounts = computed(() => this.accountService.accounts());
  loading = computed(() => this.accounts().length === 0);

  /** Disables the export button and shows progress while the CSV request is in flight. */
  readonly exporting = signal(false);

  /** Id of the account currently being deleted, if any — drives that row's spinner. */
  readonly deletingId = signal<string | null>(null);

  openAccountModal(account: AccountDTO | null) {
    this.selectedAccount.set(account);
    this.isModalOpen.set(true);
  }

  deleteAccount(account: AccountDTO) {
    if (this.deletingId()) return;

    this.deletingId.set(account.id);

    this.accountService.delete(account.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.ns.success('Account deleted successfully');
      },
      error: (err) => {
        this.deletingId.set(null);
        this.ns.error(err.error.message);
      },
    });
  }

  onAccountModalClose($event: boolean) {
    this.isModalOpen.set(false);
  }

  openImportModal(): void {
    this.isImportModalOpen.set(true);
  }

  onImportModalClosed(imported: boolean): void {
    this.isImportModalOpen.set(false);
    if (imported) this.accountService.refreshAccounts();
  }

  exportCsv(): void {
    if (this.exporting()) return;

    this.exporting.set(true);

    this.importExportService.exportAccounts().subscribe({
      next: (blob) => {
        this.exporting.set(false);
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = 'accounts.csv';
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.exporting.set(false);
        this.ns.error(err?.error?.message ?? 'Error exporting accounts');
      },
    });
  }
}
