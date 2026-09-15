import { inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { TranslocoDirective } from '@jsverse/transloco';
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
import { isSavingsType, MANUAL_TRANSACTION_TYPES, TransactionType } from '../../../../shared/models/TransactionType.enum';
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
  imports: [UiModalComponent, ReactiveFormsModule, CommonModule, TranslocoDirective],
  templateUrl: './edit-transaction.html',
  styleUrl: './edit-transaction.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditTransaction implements OnInit {
  private readonly transloco = inject(TranslocoService);

  @Input() transaction: TransactionDTO | null = null;
  @Output() closed = new EventEmitter<boolean>();

  accounts = input<AccountLike[]>();
  categories: OptionDTO[] = [];
  transactionTypes = MANUAL_TRANSACTION_TYPES;
  form: FormGroup;

  readonly quickAmounts: number[] = [1000, 2000, 5000, 10000, 15000];

  originAccountSignal = signal<string>('');

  /** Disables the form and shows progress while the request is in flight. */
  readonly saving = signal(false);

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
        date: this.formatDateForInput(this.transaction.date),
      });
      this.originAccountSignal.set(this.transaction.account);

      // Account, type and destination account cannot be changed once a transaction
      // exists — users who logged the wrong one delete it and create a new one.
      this.form.get('account')?.disable();
      this.form.get('type')?.disable();
      this.form.get('destinationAccount')?.disable();

      // Savings movements carry no category (like transfers); the type control is
      // already frozen, so the rule has to be applied here rather than on change.
      if (isSavingsType(this.transaction.type)) {
        this.applyNoCategoryRule();
      }
    }

    this.handleTypeChanges();
    this.handleAccountChanges();
  }

  setType(type: 'EXPENSE' | 'INCOME' | 'TRANSFER'): void {
    if (this.transaction) return;
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
    if (this.saving()) return;

    if (this.form.invalid) {
      this.ns.error(this.transloco.translate('transactions.form.fillRequired'));
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
      this.ns.info(this.transloco.translate('transactions.form.selectDestination'));
      return;
    }

    const request$ = this.transaction
      ? this.transactionService.update(payload)
      : this.transactionService.create(payload);

    this.saving.set(true);

    request$.subscribe({
      next: () => this.close(true),
      error: () => {
        this.saving.set(false);
        this.ns.error(this.transloco.translate('common.genericError'));
      },
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

      if (isSavingsType(type)) {
        this.applyNoCategoryRule();
        destinationCtrl?.clearValidators();
        destinationCtrl?.reset('');
      } else if (type === TransactionType.TRANSFER) {
        this.applyNoCategoryRule();

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

  /** Transfers and savings movements are uncategorised: clear, relax and freeze the control. */
  private applyNoCategoryRule(): void {
    const categoryCtrl = this.form.get('category');
    categoryCtrl?.setValue(null);
    categoryCtrl?.clearValidators();
    categoryCtrl?.disable({ emitEvent: false });
    categoryCtrl?.updateValueAndValidity({ emitEvent: false });
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

  /** Converts the backend's display format ("dd/MM/yyyy HH:mm") into the
   *  "yyyy-MM-ddTHH:mm" shape a `datetime-local` input requires. */
  private formatDateForInput(displayDate: string): string {
    const [datePart, timePart] = displayDate.split(' ');
    const [day, month, year] = datePart.split('/');
    return `${year}-${month}-${day}T${timePart}`;
  }
}
