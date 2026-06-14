import {
  ChangeDetectionStrategy,
  Component,
  effect,
  signal,
  computed,
} from '@angular/core'; // <-- Agregar computed
import { TransactionService } from '../../../transactions/services/transaction-service';
import { MonthlyTransactionReportDTO } from '../../../transactions/interfaces/MonthlyTransactionReportDTO.interface';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../../core/services/NotificationService';

@Component({
  selector: 'monthly-summary',
  imports: [CommonModule],
  templateUrl: './monthly-summary.html',
  styleUrl: './monthly-summary.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthlySummary {
  monthlyReport = signal<MonthlyTransactionReportDTO | null>(null);

  balance = computed(() => {
    const r = this.monthlyReport();
    return r ? r.income - r.outcome : 0;
  });

  constructor(
    private transactionService: TransactionService,
    private ns: NotificationService,
  ) {
    effect(() => {
      this.transactionService.refresh$();

      this.transactionService.getCurrentMonthlyReport().subscribe({
        next: (report) => this.monthlyReport.set(report),
        error: (err) => this.ns.error(err),
      });
    });
  }
}
