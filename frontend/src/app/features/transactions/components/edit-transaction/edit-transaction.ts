import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  EventEmitter,
  input,
  Input,
  Output,
  OnInit,
  ChangeDetectorRef,
  signal,
} from '@angular/core';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TransactionDTO } from '../../interfaces/TransactionDTO.interface';
import { TransactionService } from '../../services/transaction-service';
import { TransactionType } from '../../../../shared/models/TransactionType.enum';
import { AccountDTO } from '../../../accounts/interfaces/AccountDTO.interfaces';
import { CategoryService } from '../../../categories/service/category-service';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';
import { CommonModule } from '@angular/common';

type AccountLike = AccountDTO | OptionDTO;

function isAccountDTO(acc: AccountLike): acc is AccountDTO {
  return 'name' in acc;
}

@Component({
  selector: 'app-edit-transaction',
  standalone: true,
  imports: [UiModalComponent, ReactiveFormsModule, CommonModule],
  templateUrl: './edit-transaction.html',
  styleUrl: './edit-transaction.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditTransaction implements OnInit {
  @Input() transaction: TransactionDTO | null = null;
  @Output() closed = new EventEmitter<boolean>();

  accounts = input<AccountLike[]>();
  categories: OptionDTO[] = [];
  transactionTypes = Object.values(TransactionType);
  form: FormGroup;

  readonly quickAmounts: number[] = [1000, 2000, 5000, 10000, 15000];

  originAccountSignal = signal<string>('');

  filteredDestinationAccounts = computed(() => {
    const selectedAccountId = this.originAccountSignal();
    return this.accounts()?.filter((acc) => acc.id !== selectedAccountId) ?? [];
  });

  constructor(
    private fb: FormBuilder,
    private transactionService: TransactionService,
    private categoryService: CategoryService,
    private ns: NotificationService,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group({
      account: ['', Validators.required],
      description: ['', [Validators.required, Validators.maxLength(255)]],
      amount: [null, [Validators.required, Validators.min(0.01)]],
      category: ['', Validators.required],
      type: ['EXPENSE', Validators.required],
      counterparty: ['', Validators.required],
      destinationAccount: [''],
      date: [this.getLocalDateTimeString(), Validators.required],
    });

    effect(() => {
      const accounts = this.accounts();
      const accountCtrl = this.form.get('account');

      if (accounts?.length && !accountCtrl?.value) {
        const initialId = accounts[0].id;
        accountCtrl!.setValue(initialId);
        this.originAccountSignal.set(initialId);
        this.cdr.markForCheck();
      }
    });
  }

  ngOnInit(): void {
    this.loadCategories();

    if (this.transaction) {
      this.form.patchValue({
        description: this.transaction.description,
        amount: this.transaction.amount,
        category: this.transaction.category,
        type: this.transaction.type,
        counterparty: this.transaction.counterparty,
        destinationAccount: this.transaction.destinationAccount ?? '',
        account: this.transaction.account,
        date: this.transaction.date,
      });
      this.originAccountSignal.set(this.transaction.account);
    }

    this.handleTypeChanges();
    this.handleAccountChanges();
  }

  setType(type: 'EXPENSE' | 'INCOME' | 'TRANSFER'): void {
    this.form.get('type')?.setValue(type);
  }

  setQuickAmount(value: number): void {
    const amountCtrl = this.form.get('amount');
    const current = amountCtrl?.value ?? 0;
    amountCtrl?.setValue(current + value);
    amountCtrl?.markAsDirty();
    this.cdr.markForCheck();
  }

  submit(): void {
    if (this.form.invalid) {
      this.ns.error('Please fill in all required fields correctly.');
      return;
    }

    const payload: TransactionDTO = {
      ...this.transaction,
      ...this.form.getRawValue(),
    };

    if (
      payload.type === TransactionType.TRANSFER &&
      !payload.destinationAccount
    ) {
      this.ns.info('Please select a destination account for the transfer.');
      return;
    }

    const request$ = this.transaction
      ? this.transactionService.update(payload)
      : this.transactionService.create(payload);

    request$.subscribe({
      next: () => this.close(true),
      error: () => this.ns.error('Something went wrong. Please try again.'),
    });
  }

  private loadCategories(): void {
    this.categoryService.getOptions().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.cdr.markForCheck();
      },
      error: (err) => this.ns.error(err),
    });
  }

  close(success = false): void {
    this.closed.emit(success);
  }

  private handleTypeChanges(): void {
    this.form.get('type')!.valueChanges.subscribe((type) => {
      const categoryCtrl = this.form.get('category');
      const destinationCtrl = this.form.get('destinationAccount');

      if (type === TransactionType.TRANSFER) {
        categoryCtrl?.setValue(null);
        categoryCtrl?.clearValidators();
        categoryCtrl?.disable({ emitEvent: false });

        destinationCtrl?.setValidators(Validators.required);

        const destinations = this.filteredDestinationAccounts();
        if (destinations.length) {
          destinationCtrl?.setValue(destinations[0].id);
        }
      } else {
        categoryCtrl?.enable({ emitEvent: false });
        categoryCtrl?.setValidators(Validators.required);
        if (!this.transaction) categoryCtrl?.reset('');

        destinationCtrl?.clearValidators();
        destinationCtrl?.reset('');
      }

      categoryCtrl?.updateValueAndValidity();
      destinationCtrl?.updateValueAndValidity();
      this.cdr.markForCheck();
    });
  }

  private handleAccountChanges(): void {
    this.form.get('account')!.valueChanges.subscribe((accountId) => {
      this.originAccountSignal.set(accountId);

      if (this.form.get('type')?.value === TransactionType.TRANSFER) {
        const destinations = this.filteredDestinationAccounts();
        const currentDest = this.form.get('destinationAccount')?.value;

        if (currentDest === accountId || !currentDest) {
          this.form
            .get('destinationAccount')
            ?.setValue(destinations[0]?.id ?? null);
        }
      }
      this.cdr.markForCheck();
    });
  }

  getAccountLabel(account: AccountLike): string {
    return isAccountDTO(account) ? account.name : account.value;
  }

  private getLocalDateTimeString(): string {
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000;
    return new Date(now.getTime() - offset).toISOString().slice(0, 16);
  }
}
