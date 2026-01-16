import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, signal } from '@angular/core';
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

@Component({
  selector: 'app-transaction-list',
  imports: [DatePipe, CommonModule, ReactiveFormsModule, EditTransaction],
  templateUrl: './transaction-list.html',
  styleUrl: './transaction-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionList {


  TRANSACTION_TYPE = TransactionType;

  transactionTypes = Object.values(TransactionType);

  transactions = signal<TransactionDTO[]>([]);

  form: FormGroup;

  accounts: OptionDTO[] = [];
  categories: OptionDTO[] = [];

  isTransactionModalOpen = false;

  constructor(private transactionService: TransactionService,
    private fb: FormBuilder,
    private accountsService: AccountService,
    private categoryService: CategoryService,
    private cdr: ChangeDetectorRef,
    private ns: NotificationService
  ) {
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
        this.accounts = accounts;
        this.categories = categories;
        this.cdr.markForCheck();
        this.onSearch();
      },
      error: err => {
        this.ns.error(err);
      }
    });
  }



  onSearch() {
    this.transactionService.search(this.form.value).subscribe({
      next: (data) => {
        this.transactions.set(data);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.ns.error(err);
      }
    });
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

    this.cdr.markForCheck();
    this.onSearch();
  }

  onTransactionModalClosed($event: boolean) {
    this.isTransactionModalOpen = false;
    this.onSearch();
  }


  openNewTransactionModal(): void {
    this.isTransactionModalOpen = true;
  }

  get userHasAccounts(): boolean {
    return this.accountsService.userHasAccounts();
  }
}
