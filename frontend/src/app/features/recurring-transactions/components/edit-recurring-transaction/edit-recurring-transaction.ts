import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { RecurringTransactionDTO } from '../../interfaces/RecurringTransactionDTO.interface';
import { RecurringTransactionService } from '../../service/recurring-transaction-service';
import { RecurrenceFrequency } from '../../interfaces/RecurrenceFrequency.enum';
import { TransactionType } from '../../../../shared/models/TransactionType.enum';
import { AccountService } from '../../../accounts/services/AccountService';
import { CategoryService } from '../../../categories/service/category-service';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';

@Component({
  selector: 'app-edit-recurring-transaction',
  standalone: true,
  imports: [UiModalComponent, ReactiveFormsModule, CommonModule],
  templateUrl: './edit-recurring-transaction.html',
  styleUrl: './edit-recurring-transaction.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditRecurringTransaction {
  private fb = inject(FormBuilder);
  private recurringService = inject(RecurringTransactionService);
  private accountService = inject(AccountService);
  private categoryService = inject(CategoryService);
  private ns = inject(NotificationService);
  private cdr = inject(ChangeDetectorRef);

  template = input<RecurringTransactionDTO | null>(null);
  submitEvent = output<boolean>();

  frequencies = Object.values(RecurrenceFrequency);
  accounts = signal<OptionDTO[]>([]);
  categories = signal<OptionDTO[]>([]);

  readonly isEdit = computed(() => !!this.template());

  filteredDestinationAccounts = computed(() =>
    this.accounts().filter((acc) => acc.id !== this.form.get('account')?.value),
  );

  form: FormGroup;

  constructor() {
    this.form = this.fb.group({
      id: [''],
      description: ['', [Validators.required, Validators.maxLength(255)]],
      amount: [null, [Validators.required, Validators.min(0.01)]],
      type: ['EXPENSE', Validators.required],
      counterparty: ['', Validators.required],
      account: ['', Validators.required],
      destinationAccount: [''],
      category: ['', Validators.required],
      frequency: [RecurrenceFrequency.MONTHLY, Validators.required],
      nextRunDate: [this.today(), Validators.required],
      endDate: [''],
      active: [true],
    });

    this.accountService.getOptions().subscribe({
      next: (options) => {
        this.accounts.set(options);
        if (!this.form.get('account')?.value && options.length) {
          this.form.get('account')?.setValue(options[0].id);
        }
      },
      error: (err) => this.ns.error(err),
    });

    this.categoryService.getOptions().subscribe({
      next: (options) => this.categories.set(options),
      error: (err) => this.ns.error(err),
    });

    effect(() => {
      const t = this.template();
      if (t) {
        this.form.patchValue({
          id: t.id,
          description: t.description,
          amount: t.amount,
          type: t.type,
          counterparty: t.counterparty,
          account: t.account,
          destinationAccount: t.destinationAccount ?? '',
          category: t.category ?? '',
          frequency: t.frequency,
          nextRunDate: t.nextRunDate,
          endDate: t.endDate ?? '',
          active: t.active,
        });
      } else {
        this.form.reset({
          id: '',
          description: '',
          amount: null,
          type: 'EXPENSE',
          counterparty: '',
          account: this.accounts()[0]?.id ?? '',
          destinationAccount: '',
          category: '',
          frequency: RecurrenceFrequency.MONTHLY,
          nextRunDate: this.today(),
          endDate: '',
          active: true,
        });
      }
      this.handleTypeValidators(this.form.get('type')?.value);
    });

    this.form.get('type')!.valueChanges.subscribe((type) => this.handleTypeValidators(type));
  }

  private handleTypeValidators(type: string): void {
    const categoryCtrl = this.form.get('category');
    const destinationCtrl = this.form.get('destinationAccount');

    if (type === TransactionType.TRANSFER) {
      categoryCtrl?.clearValidators();
      categoryCtrl?.setValue(null, { emitEvent: false });
      destinationCtrl?.setValidators(Validators.required);
    } else {
      categoryCtrl?.setValidators(Validators.required);
      destinationCtrl?.clearValidators();
      destinationCtrl?.setValue('', { emitEvent: false });
    }

    categoryCtrl?.updateValueAndValidity({ emitEvent: false });
    destinationCtrl?.updateValueAndValidity({ emitEvent: false });
    this.cdr.markForCheck();
  }

  setType(type: 'EXPENSE' | 'INCOME' | 'TRANSFER'): void {
    this.form.get('type')?.setValue(type);
  }

  submit(): void {
    if (this.form.invalid) {
      this.ns.error('Please fill in all required fields correctly.');
      return;
    }

    const payload: RecurringTransactionDTO = this.form.getRawValue();
    const request$ = this.template()
      ? this.recurringService.update(payload)
      : this.recurringService.create(payload);

    request$.subscribe({
      next: () => {
        this.ns.success('Recurring transaction saved successfully');
        this.submitEvent.emit(true);
      },
      error: (err) => {
        this.ns.error(err?.error?.message ?? 'Error saving recurring transaction');
        this.submitEvent.emit(false);
      },
    });
  }

  close(): void {
    this.submitEvent.emit(false);
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
