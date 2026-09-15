import { ChangeDetectionStrategy, Component, computed, inject, input, LOCALE_ID, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoDirective } from '@jsverse/transloco';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { CategoryDTO } from '../../interfaces/CategoryDTO.interface';
import { CategorySpendingDTO } from '../../interfaces/CategorySpendingDTO.interface';
import { CategoryService } from '../../service/category-service';
import { NotificationService } from '../../../../core/services/NotificationService';
import { monthOptions } from '../../../../shared/models/months.const';

/**
 * Read-only look at what a category costs: the selected month's expenses and
 * the all-time total. Opens on the current month; arrows step through months
 * and each step reloads the figures for that period.
 */
@Component({
  selector: 'app-category-spending',
  standalone: true,
  imports: [CommonModule, UiModalComponent, TranslocoDirective],
  templateUrl: './category-spending.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategorySpending implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly ns = inject(NotificationService);
  private readonly months = monthOptions(inject(LOCALE_ID));

  category = input.required<CategoryDTO>();
  closed = output<void>();

  readonly loading = signal(true);
  readonly spending = signal<CategorySpendingDTO | null>(null);

  /** Period being shown; starts on the current month. */
  readonly year = signal(new Date().getFullYear());
  readonly month = signal(new Date().getMonth() + 1);

  /** Localised "March 2026" style label for the period header. */
  readonly periodLabel = computed(() => `${this.months[this.month() - 1].label} ${this.year()}`);

  /** Stepping past the current month makes no sense: there is nothing spent there yet. */
  readonly isCurrentMonth = computed(() => {
    const now = new Date();
    return this.year() === now.getFullYear() && this.month() === now.getMonth() + 1;
  });

  ngOnInit(): void {
    this.load();
  }

  previousMonth(): void {
    if (this.month() === 1) {
      this.month.set(12);
      this.year.update((y) => y - 1);
    } else {
      this.month.update((m) => m - 1);
    }
    this.load();
  }

  nextMonth(): void {
    if (this.isCurrentMonth()) return;
    if (this.month() === 12) {
      this.month.set(1);
      this.year.update((y) => y + 1);
    } else {
      this.month.update((m) => m + 1);
    }
    this.load();
  }

  close(): void {
    this.closed.emit();
  }

  private load(): void {
    this.loading.set(true);
    this.categoryService.getSpending(this.category().id, this.year(), this.month()).subscribe({
      next: (data) => {
        this.spending.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.ns.error(err);
        this.loading.set(false);
      },
    });
  }
}
