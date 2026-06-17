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
  standalone: true, // Asegúrate de que sea standalone
  imports: [CommonModule],
  templateUrl: './monthly-summary.html',
  styleUrl: './monthly-summary.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthlySummary {
  private transactionService = inject(TransactionService);
  private ns = inject(NotificationService);

  monthlyReport = toSignal(
    this.transactionService.getCurrentMonthlyReport().pipe(
      catchError((err) => {
        this.ns.error(err);
        return of(null);
      }),
    ),
    { initialValue: null },
  );

  balance = computed(() => {
    const r = this.monthlyReport();
    return r ? r.income - r.outcome : 0;
  });
}
