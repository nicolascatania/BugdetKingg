import { ChangeDetectionStrategy, Component, computed, inject, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { TransactionImportExportService } from '../../services/transaction-import-export-service';
import { TransactionService } from '../../services/transaction-service';
import { AccountService } from '../../../accounts/services/AccountService';
import { ImportPreviewDTO } from '../../interfaces/ImportPreviewDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';

type Step = 'select' | 'preview' | 'done';

@Component({
  selector: 'app-import-transactions',
  standalone: true,
  imports: [UiModalComponent, CommonModule],
  templateUrl: './import-transactions.html',
  styleUrl: './import-transactions.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportTransactions {
  private importExportService = inject(TransactionImportExportService);
  private transactionService = inject(TransactionService);
  private accountService = inject(AccountService);
  private ns = inject(NotificationService);

  closed = output<boolean>();

  step = signal<Step>('select');
  loading = signal(false);
  selectedFile = signal<File | null>(null);
  preview = signal<ImportPreviewDTO | null>(null);

  /**
   * Maps a resolved account id (as returned per-row by the preview) back to its display
   * name, purely for rendering the preview table - the import no longer needs the user to
   * pick an account up front, each row already carries its own.
   */
  private accountNames = computed(() => {
    const map = new Map<string, string>();
    for (const account of this.accountService.accounts()) {
      map.set(account.id, account.name);
    }
    return map;
  });

  accountName(id: string | null): string {
    if (!id) return '—';
    return this.accountNames().get(id) ?? '—';
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile.set(file);
  }

  runPreview(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.loading.set(true);
    this.importExportService.previewImport(file).subscribe({
      next: (result) => {
        this.preview.set(result);
        this.step.set('preview');
        this.loading.set(false);
      },
      error: (err) => {
        this.ns.error(err?.error?.message ?? 'Error reading CSV file');
        this.loading.set(false);
      },
    });
  }

  commit(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.loading.set(true);
    this.importExportService.commitImport(file).subscribe({
      next: (result) => {
        this.preview.set(result);
        this.step.set('done');
        this.loading.set(false);
        this.ns.success(`${result.validRows} transactions imported`);
        this.transactionService.notifyImported();
      },
      error: (err) => {
        this.ns.error(err?.error?.message ?? 'Error importing transactions');
        this.loading.set(false);
      },
    });
  }

  backToSelect(): void {
    this.step.set('select');
    this.preview.set(null);
  }

  close(): void {
    this.closed.emit(this.step() === 'done');
  }
}
