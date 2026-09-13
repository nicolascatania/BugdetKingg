import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { SavingsGoalDTO } from '../../interfaces/SavingsGoalDTO.interface';
import { SavingsGoalService } from '../../service/savings-goal-service';
import { AccountService } from '../../../accounts/services/AccountService';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';
import { FINANCIAL_ICONS } from '../../../icons/interfaces/iconsenum.interace';

@Component({
  selector: 'app-edit-savings-goal',
  standalone: true,
  imports: [UiModalComponent, ReactiveFormsModule, CommonModule],
  templateUrl: './edit-savings-goal.html',
  styleUrl: './edit-savings-goal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditSavingsGoal {
  private fb = inject(FormBuilder);
  private savingsGoalService = inject(SavingsGoalService);
  private accountService = inject(AccountService);
  private ns = inject(NotificationService);

  goal = input<SavingsGoalDTO | null>(null);
  submitEvent = output<boolean>();

  accounts = signal<OptionDTO[]>([]);
  iconOptions = FINANCIAL_ICONS;

  readonly isEdit = computed(() => !!this.goal());

  /** Disables the form and shows progress while the request is in flight. */
  readonly saving = signal(false);

  form: FormGroup;

  constructor() {
    this.form = this.fb.group({
      id: [''],
      name: ['', [Validators.required, Validators.maxLength(100)]],
      icon: ['fa-piggy-bank', Validators.required],
      targetAmount: [null, [Validators.required, Validators.min(0.01)]],
      targetDate: ['', Validators.required],
      linkedAccountId: [''],
    });

    this.accountService.getOptions().subscribe({
      next: (options) => this.accounts.set(options),
      error: (err) => this.ns.error(err),
    });

    effect(() => {
      const g = this.goal();
      if (g) {
        this.form.patchValue({
          id: g.id,
          name: g.name,
          icon: g.icon,
          targetAmount: g.targetAmount,
          targetDate: g.targetDate,
          linkedAccountId: g.linkedAccountId ?? '',
        });
      } else {
        this.form.reset({ icon: 'fa-piggy-bank' });
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.saving()) return;

    const raw = this.form.getRawValue();
    const payload: SavingsGoalDTO = {
      ...raw,
      linkedAccountId: raw.linkedAccountId || null,
      // Derived fields the backend recomputes and ignores on write — Jackson still
      // requires them present because SavingsGoalDTO is a record with primitive components.
      status: this.goal()?.status ?? 'ACTIVE',
      state: this.goal()?.state ?? 'ACTIVE',
      achieved: this.goal()?.achieved ?? false,
      currentAmount: this.goal()?.currentAmount ?? 0,
      progressPercentage: this.goal()?.progressPercentage ?? 0,
      remainingAmount: this.goal()?.remainingAmount ?? 0,
      monthlyRequired: this.goal()?.monthlyRequired ?? 0,
      daysRemaining: this.goal()?.daysRemaining ?? 0,
    };

    const request$ = this.goal()
      ? this.savingsGoalService.update(payload)
      : this.savingsGoalService.create(payload);

    this.saving.set(true);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.ns.success('Savings goal saved successfully');
        this.submitEvent.emit(true);
      },
      error: (err) => {
        this.saving.set(false);
        this.ns.error(err?.error?.message ?? 'Error saving savings goal');
        this.submitEvent.emit(false);
      },
    });
  }

  close(): void {
    this.submitEvent.emit(false);
  }
}
