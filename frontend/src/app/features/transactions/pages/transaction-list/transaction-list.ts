import { CommonModule, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { TransactionDTO } from '../../interfaces/TransactionDTO.interface';
import { TransactionService } from '../../services/transaction-service';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AccountService } from '../../../accounts/services/AccountService';
import { CategoryService } from '../../../categories/service/category-service';
import { forkJoin } from 'rxjs';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { TransactionType } from '../../../../shared/models/TransactionType.enum';
import { EditTransaction } from '../../components/edit-transaction/edit-transaction';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { NotificationService } from '../../../../core/services/NotificationService';
import { PaginationComponent } from '../../../../shared/components/PaginationComponent/PaginationComponent';
import {
  MonthQuickPicker,
  MonthQuickRange,
} from '../../../../shared/components/month-quick-picker/month-quick-picker';
import { TransactionFilter } from '../../interfaces/TransactionFilter.interface';
import {
  createPaginationState,
  PaginationState,
} from '../../../../core/utils/pagination.util';

import { RevealDirective } from '../../../../shared/directives/reveal.directive';
import { ImportTransactions } from '../../components/import-transactions/import-transactions';
import { TransactionImportExportService } from '../../services/transaction-import-export-service';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    EditTransaction,
    UiModalComponent,
    PaginationComponent,
    MonthQuickPicker,
    RevealDirective,
    ImportTransactions,
  ],
  templateUrl: './transaction-list.html',
  styleUrl: './transaction-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionList {
  private transactionService = inject(TransactionService);
  private fb = inject(FormBuilder);
  private accountsService = inject(AccountService);
  private categoryService = inject(CategoryService);
  private ns = inject(NotificationService);
  private importExportService = inject(TransactionImportExportService);

  loading = signal(false);
  isImportModalOpen = signal(false);

  /** Disables the export button and shows progress while the CSV request is in flight. */
  readonly exporting = signal(false);

  TRANSACTION_TYPE = TransactionType;
  transactionTypes = Object.values(TransactionType);

  paginationState: PaginationState = createPaginationState(20);
  form: FormGroup;

  accounts = signal<OptionDTO[]>([]);
  categories = signal<OptionDTO[]>([]);
  transactions = signal<TransactionDTO[]>([]);

  isTransactionModalOpen = signal(false);
  editingTransaction = signal<TransactionDTO | null>(null);
  transactionToDelete = signal<TransactionDTO | null>(null);

  /** Disables the delete confirm button and shows progress while the request is in flight. */
  readonly deleting = signal(false);

  /**
   * Whether the filter panel is expanded. Collapsed by default on small
   * screens so the transaction list itself is the first thing users see;
   * the panel is always visible from `lg` up (handled in the template).
   */
  filtersOpen = signal(false);

  // Bumped to re-run the search effect.
  private searchTrigger = signal(0);

  constructor() {
    this.form = this.fb.group({
      dateFrom: [''],
      dateTo: [''],
      minAmount: [''],
      maxAmount: [''],
      account: [''],
      category: [''],
      type: [''],
      description: [''],
      counterparty: [''],
    });

    // Re-runs the search every time the trigger is bumped.
    effect(() => {
      this.searchTrigger();
      this.performSearch();
    });
  }

  ngOnInit() {
    this.loadFilterData();
  }

  loadFilterData() {
    forkJoin({
      accounts: this.accountsService.getOptions(),
      categories: this.categoryService.getOptions(),
    }).subscribe({
      next: ({ accounts, categories }) => {
        this.accounts.set(accounts);
        this.categories.set(categories);
        this.onSearch();
      },
      error: (err) => {
        this.ns.error(err);
      },
    });
  }

  private performSearch(): void {
    this.loading.set(true);
    const filter: TransactionFilter = {
      page: this.paginationState.currentPage(),
      size: this.paginationState.pageSize(),
      ...this.form.value,
    };

    this.transactionService.search(filter).subscribe({
      next: (data) => {
        this.transactions.set(data.content);
        this.paginationState.updateFromResponse(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.ns.error(err);
        this.transactions.set([]);
        this.loading.set(false);
      },
    });
  }

  onSearch() {
    this.searchTrigger.update((v) => v + 1);
  }

  toggleFilters(): void {
    this.filtersOpen.update((open) => !open);
  }

  /** Number of filters currently applied, surfaced as a badge on the toggle. */
  readonly activeFilterCount = computed(() => {
    this.searchTrigger();
    return Object.values(this.form.value ?? {}).filter(
      (value) => value !== '' && value !== null && value !== undefined,
    ).length;
  });

  onMonthRangeSelected(range: MonthQuickRange) {
    this.form.patchValue({ dateFrom: range.from, dateTo: range.to });
  }

  onClear() {
    this.form.reset({
      dateFrom: '',
      dateTo: '',
      minAmount: '',
      maxAmount: '',
      account: '',
      category: '',
      type: '',
      description: '',
      counterparty: '',
    });
    this.onSearch();
  }

  onTransactionModalClosed($event: boolean) {
    this.isTransactionModalOpen.set(false);
    this.editingTransaction.set(null);
    this.onSearch();
  }

  openNewTransactionModal(): void {
    this.editingTransaction.set(null);
    this.isTransactionModalOpen.set(true);
  }

  openEditModal(transaction: TransactionDTO): void {
    this.editingTransaction.set(transaction);
    this.isTransactionModalOpen.set(true);
  }

  confirmDelete(transaction: TransactionDTO): void {
    this.transactionToDelete.set(transaction);
  }

  cancelDelete(): void {
    this.transactionToDelete.set(null);
  }

  deleteTransaction(): void {
    const transaction = this.transactionToDelete();
    if (!transaction || this.deleting()) return;

    this.deleting.set(true);

    this.transactionService.delete(transaction.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.transactionToDelete.set(null);
        this.ns.success('Transaction deleted.');
        this.onSearch();
      },
      error: (err) => {
        this.deleting.set(false);
        this.ns.error(err?.error?.message ?? 'Error deleting transaction');
      },
    });
  }

  get userHasAccounts(): boolean {
    return this.accountsService.userHasAccounts();
  }

  onPageChange(newPage: number) {
    this.paginationState.goToPage(newPage);
    this.onSearch();
  }

  openImportModal(): void {
    this.isImportModalOpen.set(true);
  }

  onImportModalClosed(imported: boolean): void {
    this.isImportModalOpen.set(false);
    if (imported) this.onSearch();
  }

  exportCsv(): void {
    if (this.exporting()) return;

    this.exporting.set(true);

    this.importExportService.exportTransactions(this.form.value).subscribe({
      next: (blob) => {
        this.exporting.set(false);
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = 'transactions.csv';
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.exporting.set(false);
        this.ns.error(err?.error?.message ?? 'Error exporting transactions');
      },
    });
  }
}
