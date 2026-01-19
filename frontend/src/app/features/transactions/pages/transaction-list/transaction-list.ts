import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
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
import { TransactionFilter } from '../../interfaces/TransactionFilter.interface';
import { createPaginationState, PaginationState } from '../../../../core/utils/pagination.util';

@Component({
  selector: 'app-transaction-list',
  imports: [CommonModule, ReactiveFormsModule, EditTransaction, PaginationComponent],
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

  TRANSACTION_TYPE = TransactionType;
  transactionTypes = Object.values(TransactionType);

  paginationState: PaginationState = createPaginationState(20);
  form: FormGroup;

  accounts = signal<OptionDTO[]>([]);
  categories = signal<OptionDTO[]>([]);
  transactions = signal<TransactionDTO[]>([]);

  isTransactionModalOpen = signal(false);

  // Trigger para recargar cuando cambia la búsqueda
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
      counterparty: ['']
    });

    // Efecto: cuando searchTrigger cambia, busca transacciones
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
      categories: this.categoryService.getOptions()
    }).subscribe({
      next: ({ accounts, categories }) => {
        this.accounts.set(accounts);
        this.categories.set(categories);
        this.onSearch();
      },
      error: err => {
        this.ns.error(err);
      }
    });
  }

  private performSearch(): void {
    const filter: TransactionFilter = {
      page: this.paginationState.currentPage(),
      size: this.paginationState.pageSize(),
      ...this.form.value
    };

    this.transactionService.search(filter).subscribe({
      next: (data) => {
        this.transactions.set(data.content);
        this.paginationState.updateFromResponse(data);
      },
      error: (err) => {
        this.ns.error(err);
        this.transactions.set([]);
      }
    });
  }

  onSearch() {
    this.searchTrigger.update(v => v + 1);
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
      counterparty: ''
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
