import { TranslocoDirective } from '@jsverse/transloco';
import {
  ChangeDetectionStrategy,
  Component,
  inject,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { NotificationService } from '../../../../core/services/NotificationService';
import { catchError, delay, of } from 'rxjs';

@Component({
  selector: 'monthly-summary',
  standalone: true,
  imports: [CommonModule, TranslocoDirective],
  templateUrl: './monthly-summary.html',
  styleUrl: './monthly-summary.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthlySummary {
  private transactionService = inject(TransactionService);
  private ns = inject(NotificationService);

  /** Current and previous month in one call; the tiles read the current half. */
  comparison = toSignal(
    this.transactionService.getMonthComparison().pipe(
      catchError((err) => {
        this.ns.error(err);
        return of(null);
      }),
    ),
    { initialValue: null },
  );

  balance = computed(() => {
    const c = this.comparison();
    return c ? c.currentIncome - c.currentExpense : 0;
  });

  previousBalance = computed(() => {
    const c = this.comparison();
    return c ? c.previousIncome - c.previousExpense : 0;
  });

  /** Signed change vs last month, in absolute money, per figure. */
  incomeDelta = computed(() => this.delta(this.comparison()?.currentIncome, this.comparison()?.previousIncome));
  expenseDelta = computed(() => this.delta(this.comparison()?.currentExpense, this.comparison()?.previousExpense));
  balanceDelta = computed(() => this.balance() - this.previousBalance());

  /** Percentage change vs last month; `null` when last month was zero (no ratio to show). */
  incomePct = computed(() => this.pct(this.comparison()?.currentIncome, this.comparison()?.previousIncome));
  expensePct = computed(() => this.pct(this.comparison()?.currentExpense, this.comparison()?.previousExpense));

  private delta(current?: number, previous?: number): number {
    return (current ?? 0) - (previous ?? 0);
  }

  private pct(current?: number, previous?: number): number | null {
    if (!previous) return null;
    return (((current ?? 0) - previous) / previous) * 100;
  }
}
