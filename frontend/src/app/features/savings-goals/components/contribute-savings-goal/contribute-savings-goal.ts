import { TranslocoService } from '@jsverse/transloco';
import { TranslocoDirective } from '@jsverse/transloco';
import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { SavingsGoalDTO } from '../../interfaces/SavingsGoalDTO.interface';
import { SavingsGoalService } from '../../service/savings-goal-service';
import { AccountService } from '../../../accounts/services/AccountService';
import { CategoryService } from '../../../categories/service/category-service';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { SavingsGoalCloseOutcome } from '../../interfaces/SavingsGoalContributionDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';

/** What the modal does with the money. */
export type ContributionMode = 'deposit' | 'withdraw' | 'close';

/**
 * Moves money between an account and a savings goal.
 *
 * One component for the three flows because they share the same shape (pick an
 * account, confirm an amount); `mode` only changes copy, validation and which
 * endpoint is called. Closing always moves the whole balance, so the amount field
 * is hidden and the account is optional when there is nothing left to return.
 * When closing with money the user says what happened with it: spent on the goal
 * (an expense is recorded, optionally categorised) or returned to an account.
 */
@Component({
  selector: 'app-contribute-savings-goal',
  standalone: true,
  imports: [UiModalComponent, ReactiveFormsModule, CommonModule, TranslocoDirective],
  templateUrl: './contribute-savings-goal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContributeSavingsGoal implements OnInit {
  private readonly transloco = inject(TranslocoService);

  private fb = inject(FormBuilder);
  private savingsGoalService = inject(SavingsGoalService);
  private accountService = inject(AccountService);
  private categoryService = inject(CategoryService);
  private ns = inject(NotificationService);

  goal = input.required<SavingsGoalDTO>();
  mode = input.required<ContributionMode>();
  submitEvent = output<boolean>();

  /** Accounts with balances, so the user sees what each one can afford. */
  readonly accounts = this.accountService.accounts;

  readonly saving = signal(false);

  /** Category options for the expense recorded when the goal was spent. */
  readonly categories = signal<OptionDTO[]>([]);

  readonly isDeposit = computed(() => this.mode() === 'deposit');
  readonly isWithdraw = computed(() => this.mode() === 'withdraw');
  readonly isClose = computed(() => this.mode() === 'close');

  /** Closing an empty goal has no money to return, so no account is needed. */
  readonly needsAccount = computed(() => !this.isClose() || this.goal().currentAmount > 0);

  /** Translation keys; the template resolves them so the copy follows the active language. */
  readonly titleKey = computed(() => {
    switch (this.mode()) {
      case 'deposit':
        return 'savings.contribute.depositTitle';
      case 'withdraw':
        return 'savings.contribute.withdrawTitle';
      default:
        return 'savings.contribute.closeTitle';
    }
  });

  readonly accountLabelKey = computed(() => {
    if (this.isDeposit()) return 'savings.contribute.fromAccount';
    if (this.isSpend()) return 'savings.contribute.spendAccount';
    return 'savings.contribute.toAccount';
  });

  readonly submitLabelKey = computed(() => {
    switch (this.mode()) {
      case 'deposit':
        return 'savings.addMoney';
      case 'withdraw':
        return 'savings.withdraw';
      default:
        return 'savings.contribute.closeTitle';
    }
  });

  form: FormGroup = this.fb.group({
    accountId: ['', Validators.required],
    amount: [null, [Validators.required, Validators.min(0.01)]],
    date: [this.getLocalDateTimeString(), Validators.required],
    note: ['', Validators.maxLength(255)],
    outcome: ['RETURN' as SavingsGoalCloseOutcome],
    categoryId: [''],
  });

  /** Mirrors the outcome control so the close form can swap its copy and fields. */
  private readonly outcome = toSignal(this.form.get('outcome')!.valueChanges, {
    initialValue: this.form.get('outcome')!.value as SavingsGoalCloseOutcome,
  });

  readonly isSpend = computed(() => this.isClose() && this.needsAccount() && this.outcome() === 'SPEND');

  /** Mirrors the account control as a signal so derived figures react to it. */
  private readonly selectedAccountId = toSignal(this.form.get('accountId')!.valueChanges, {
    initialValue: '',
  });

  readonly selectedAccount = computed(() =>
    this.accounts().find((a) => a.id === this.selectedAccountId()) ?? null,
  );

  /** Upper bound for the amount: what the account or the goal can actually give. */
  readonly maxAmount = computed(() => {
    if (this.isWithdraw()) return this.goal().currentAmount;
    return this.selectedAccount()?.balance ?? null;
  });

  ngOnInit(): void {
    // Preselect the goal's default source account, else the first one the user has.
    const defaultAccount = this.goal().linkedAccountId ?? this.accounts()[0]?.id ?? '';
    this.form.patchValue({ accountId: defaultAccount });

    if (this.isClose()) {
      this.form.get('amount')?.clearValidators();
      this.form.get('amount')?.updateValueAndValidity();
      if (!this.needsAccount()) {
        this.form.get('accountId')?.clearValidators();
        this.form.get('accountId')?.updateValueAndValidity();
      } else {
        // A reached goal was most likely spent on its purpose; anything else is usually money coming back.
        this.form.get('outcome')?.setValue(this.goal().state === 'ACHIEVED' ? 'SPEND' : 'RETURN');
        this.loadCategories();
      }
    }
  }

  private loadCategories(): void {
    this.categoryService.getOptions().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (err) => this.ns.error(err),
    });
  }

  /** Fills the amount with the maximum the flow allows. */
  useMax(): void {
    const max = this.maxAmount();
    if (max === null) return;
    this.form.get('amount')?.setValue(max);
    this.form.get('amount')?.markAsDirty();
  }

  submit(): void {
    if (this.form.invalid || this.saving()) return;

    const raw = this.form.getRawValue();
    const id = this.goal().id;

    let request$: Observable<SavingsGoalDTO>;
    if (this.isClose()) {
      request$ = this.savingsGoalService.close(id, {
        accountId: raw.accountId || null,
        outcome: raw.outcome,
        categoryId: this.isSpend() ? raw.categoryId || null : null,
      });
    } else {
      const body = {
        accountId: raw.accountId,
        amount: Number(raw.amount),
        date: raw.date || null,
        note: raw.note?.trim() || null,
      };
      request$ = this.isDeposit()
        ? this.savingsGoalService.deposit(id, body)
        : this.savingsGoalService.withdraw(id, body);
    }

    this.saving.set(true);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.ns.success(this.transloco.translate(this.isClose() ? 'savings.closed' : 'savings.updated'));
        this.submitEvent.emit(true);
      },
      error: (err) => {
        this.saving.set(false);
        this.ns.error(err?.error?.message ?? this.transloco.translate('savings.contribute.moveError'));
      },
    });
  }

  close(): void {
    this.submitEvent.emit(false);
  }

  private getLocalDateTimeString(): string {
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000;
    return new Date(now.getTime() - offset).toISOString().slice(0, 16);
  }
}
