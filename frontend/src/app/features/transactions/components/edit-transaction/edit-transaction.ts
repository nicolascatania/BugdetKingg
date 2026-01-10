import { ChangeDetectionStrategy, Component, computed, effect, EventEmitter, input, Input, Output } from '@angular/core';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TransactionDTO } from '../../interfaces/TransactionDTO.interface';
import { TransactionService } from '../../services/transaction-service';
import { TransactionType } from '../../../../shared/models/TransactionType.enum';
import { TransactionCategory } from '../../../../shared/models/TransactionCategories.enum';
import { AccountService } from '../../../accounts/services/AccountService';
import { AccountDTO } from '../../../accounts/interfaces/AccountDTO.interfaces';
import { formatDate } from '../../../../shared/utils/datesUtils';
import { CategoryService } from '../../../categories/service/category-service';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';



type AccountLike = AccountDTO | OptionDTO;

function isAccountDTO(acc: AccountLike): acc is AccountDTO {
  return 'name' in acc;
}



@Component({
  selector: 'app-edit-transaction',
  imports: [UiModalComponent, ReactiveFormsModule],
  templateUrl: './edit-transaction.html',
  styleUrl: './edit-transaction.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditTransaction {

  @Input() transaction: TransactionDTO | null = null;
  @Output() closed = new EventEmitter<boolean>();

  accounts = input<AccountLike[]>();
  categories: OptionDTO[] = [];


  transactionTypes = Object.values(TransactionType);

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private transactionService: TransactionService,
    private accountService: AccountService,
    private categoryService: CategoryService
  ) {
    this.form = this.fb.group({
      account: ['', Validators.required],
      description: ['', [Validators.required, Validators.maxLength(255)]],
      amount: [0, [Validators.required]],
      category: [{ value: '', disabled: false }, Validators.required],
      type: ['', Validators.required],
      counterparty: ['', Validators.required],
      destinationAccount: [''],
    });

    effect(() => {
      const accounts = this.accounts();
      const accountCtrl = this.form.get('account');

      if (accounts?.length && !accountCtrl?.value) {
        accountCtrl?.setValue(accounts[0].id);
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
      });
    }

    this.handleTypeChanges();
    this.handleAccountChanges();
  }


  submit(): void {
    if (this.form.invalid) return;

    const payload: TransactionDTO = {
      id: this.transaction?.id ?? '',
      account: this.transaction?.account ?? '',
      date: this.transaction?.date ?? formatDate(new Date()),
      ...this.form.getRawValue(),
    };

    if (payload.type === TransactionType.TRANSFER && !payload.destinationAccount) {
      alert('Please select a destination account for the transfer.');
      return;
    }

    const request$ = this.transaction
      ? this.transactionService.update(payload)
      : this.transactionService.create(payload);

    request$.subscribe({
      next: () => this.close(true),
      error: () => alert('Something went wrong'),
    });
  }

  private loadCategories(): void {
    this.categoryService.getOptions().subscribe({
      next: categories => {
        this.categories = categories;
      },
      error: () => alert('Error loading categories'),
    });
  }


  /**
   * Closes the modal.
   */
  close(success = false): void {
    this.closed.emit(success);
  }

  private handleTypeChanges(): void {
    this.form.get('type')!.valueChanges.subscribe(type => {
      const categoryCtrl = this.form.get('category');
      const destinationCtrl = this.form.get('destinationAccount');

      if (type === TransactionType.TRANSFER) {
        categoryCtrl?.setValue(TransactionCategory.TRANSFER);
        categoryCtrl?.disable({ emitEvent: false });

        destinationCtrl?.setValidators(Validators.required);

        const destinations = this.filteredDestinationAccounts();
        if (destinations.length) {
          destinationCtrl?.setValue(destinations[0].id);
        }
      } else {
        categoryCtrl?.enable({ emitEvent: false });
        categoryCtrl?.reset();

        destinationCtrl?.clearValidators();
        destinationCtrl?.reset();
      }

      destinationCtrl?.updateValueAndValidity();
    });
  }


  filteredDestinationAccounts = computed(() => {
    const selectedAccountId = this.form.get('account')?.value;
    return this.accounts()?.filter(acc => acc.id !== selectedAccountId) ?? [];
  });

  private handleAccountChanges(): void {
    this.form.get('account')!.valueChanges.subscribe(() => {
      if (this.form.get('type')?.value === TransactionType.TRANSFER) {
        const destinations = this.filteredDestinationAccounts();
        this.form
          .get('destinationAccount')
          ?.setValue(destinations[0]?.id ?? null);
      }
    });
  }

  getAccountLabel(account: AccountLike): string {
    if (isAccountDTO(account)) {
      return account.name;
    }
    return account.value;
  }



}
