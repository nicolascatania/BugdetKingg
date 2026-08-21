import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { TransactionImportExportService } from '../../services/transaction-import-export-service';
import { AccountService } from '../../../accounts/services/AccountService';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { ImportPreviewDTO } from '../../interfaces/ImportPreviewDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';

type Step = 'select' | 'preview' | 'done';

@Component({
  selector: 'app-import-transactions',
  standalone: true,
  imports: [UiModalComponent, CommonModule, FormsModule],
  templateUrl: './import-transactions.html',
  styleUrl: './import-transactions.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportTransactions {
  private importExportService = inject(TransactionImportExportService);
  private accountService = inject(AccountService);
  private ns = inject(NotificationService);

  closed = output<boolean>();

  step = signal<Step>('select');
  loading = signal(false);
  selectedFile = signal<File | null>(null);
  selectedAccount = signal<string>('');
  preview = signal<ImportPreviewDTO | null>(null);

  accounts = signal<OptionDTO[]>([]);

  constructor() {
    this.accountService.getOptions().subscribe({
      next: (options) => {
        this.accounts.set(options);
        if (options.length) this.selectedAccount.set(options[0].id);
      },
      error: (err) => this.ns.error(err),
    });
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
    const accountId = this.selectedAccount();
    if (!file || !accountId) return;

    this.loading.set(true);
    this.importExportService.commitImport(file, accountId).subscribe({
      next: (result) => {
        this.preview.set(result);
        this.step.set('done');
        this.loading.set(false);
        this.ns.success(`${result.validRows} transactions imported`);
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
