import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { BudgetDTO } from '../../interfaces/BudgetDTO.interface';
import { BudgetService } from '../../service/budget-service';
import { CategoryService } from '../../../categories/service/category-service';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';
import { MONTHS } from '../../../../shared/models/months.const';

@Component({
  selector: 'app-edit-budget',
  standalone: true,
  imports: [UiModalComponent, ReactiveFormsModule, CommonModule],
  templateUrl: './edit-budget.html',
  styleUrl: './edit-budget.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditBudget {
  private fb = inject(FormBuilder);
  private budgetService = inject(BudgetService);
  private categoryService = inject(CategoryService);
  private ns = inject(NotificationService);

  budget = input<BudgetDTO | null>(null);
  defaultYear = input<number>(new Date().getFullYear());
  defaultMonth = input<number>(new Date().getMonth() + 1);
  submitEvent = output<boolean>();

  categories = signal<OptionDTO[]>([]);
  months = MONTHS;

  form: FormGroup;

  readonly isEdit = computed(() => !!this.budget());

  constructor() {
    this.form = this.fb.group({
      id: [''],
      category: ['', Validators.required],
      year: [this.defaultYear(), [Validators.required, Validators.min(2000)]],
      month: [this.defaultMonth(), [Validators.required, Validators.min(1), Validators.max(12)]],
      limitAmount: [null, [Validators.required, Validators.min(0.01)]],
    });

    this.categoryService.getOptions().subscribe({
      next: (options) => this.categories.set(options),
      error: (err) => this.ns.error(err),
    });

    effect(() => {
      const b = this.budget();
      if (b) {
        this.form.patchValue({
          id: b.id,
          category: b.category,
          year: b.year,
          month: b.month,
          limitAmount: b.limitAmount,
        });
      } else {
        this.form.reset({
          id: '',
          category: '',
          year: this.defaultYear(),
          month: this.defaultMonth(),
          limitAmount: null,
        });
      }
    });
  }

  submit(): void {
    if (this.form.invalid) return;

    const payload: BudgetDTO = this.form.getRawValue();
    const request$ = this.budget() ? this.budgetService.update(payload) : this.budgetService.create(payload);

    request$.subscribe({
      next: () => {
        this.ns.success('Budget saved successfully');
        this.submitEvent.emit(true);
      },
      error: (err) => {
        this.ns.error(err?.error?.message ?? 'Error saving budget');
        this.submitEvent.emit(false);
      },
    });
  }

  close(): void {
    this.submitEvent.emit(false);
  }
}
