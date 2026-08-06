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

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    EditTransaction,
    PaginationComponent,
    MonthQuickPicker,
    RevealDirective,
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

  loading = signal(false);

  TRANSACTION_TYPE = TransactionType;
  transactionTypes = Object.values(TransactionType);

  paginationState: PaginationState = createPaginationState(20);
  form: FormGroup;

  accounts = signal<OptionDTO[]>([]);
  categories = signal<OptionDTO[]>([]);
  transactions = signal<TransactionDTO[]>([]);

  isTransactionModalOpen = signal(false);

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
    this.onSearch();
  }

  openNewTransactionModal(): void {
    this.isTransactionModalOpen.set(true);
  }

  get userHasAccounts(): boolean {
    return this.accountsService.userHasAccounts();
  }

  onPageChange(newPage: number) {
    this.paginationState.goToPage(newPage);
    this.onSearch();
  }
}
