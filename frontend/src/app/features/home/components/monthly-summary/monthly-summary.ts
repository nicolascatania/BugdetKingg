import { ChangeDetectionStrategy, Component, effect, signal } from '@angular/core';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { MonthlyTransactionReportDTO } from '../../../transactions/interfaces/MonthlyTransactionReportDTO.interface';
import { CommonModule } from '@angular/common';
import { AccountService } from '../../../accounts/services/AccountService';
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

  constructor(private transactionService: TransactionService,
    private ns: NotificationService
  ) {
    effect(() => {
      this.transactionService.refresh$();

      this.transactionService.getCurrentMonthlyReport().subscribe({
        next: report => this.monthlyReport.set(report),
        error: err => this.ns.error(err)
      });
    });
  }

  get balance(): number {
    const r = this.monthlyReport();
    return r ? r.income - r.outcome : 0;
  }
}
