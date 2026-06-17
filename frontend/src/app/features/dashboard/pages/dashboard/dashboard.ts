import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  signal,
  OnInit,
} from '@angular/core';
import { Chart, PieController, ArcElement, Tooltip, Legend } from 'chart.js';
import { FormGroup, FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { AccountService } from '../../../accounts/services/AccountService';
import {
  CategoryExpense,
  DashBoardDTO,
} from '../../interfaces/DashBoardDTO.interface';
import { MonthlyIncomeExpenseDTO } from '../../../transactions/interfaces/MonthlyIncomeExpenseDTO.interface';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';
import { HttpErrorResponse } from '@angular/common/http';
import { LastMoves } from '../../../home/components/last-moves/last-moves';
import { ExpensesIncomeEachMonth } from '../../components/expenses-income-each-month/expenses-income-each-month';

Chart.register(PieController, ArcElement, Tooltip, Legend);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LastMoves,
    ExpensesIncomeEachMonth,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard implements OnInit {
  private expenseChart?: Chart;

  fromDate!: string;
  toDate!: string;
  filterForm!: FormGroup;

  dashboardData = signal<DashBoardDTO | null>(null);
  annualChartData = signal<MonthlyIncomeExpenseDTO[]>([]);
  accountsSignal = signal<OptionDTO[]>([]);
  filterTrigger = signal(0);
  loading = signal(true);

  constructor(
    private fb: FormBuilder,
    private ns: NotificationService,
    private transactionService: TransactionService,
    private accountService: AccountService,
  ) {
    effect(() => {
      this.filterTrigger();
      this.loadDashboardData();
    });
  }

  ngOnInit() {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    this.fromDate = firstDay.toISOString().split('T')[0];
    this.toDate = lastDay.toISOString().split('T')[0];

    this.filterForm = this.fb.group({
      dateFrom: [this.fromDate],
      dateTo: [this.toDate],
      annualAccountFilterId: [''],
    });

    this.loadAccounts();
    this.filterTrigger.update((v) => v + 1);
    this.loadAnnualData('');
  }

  private loadAccounts() {
    this.accountService.getOptions().subscribe({
      next: (accounts) => {
        this.accountsSignal.set(accounts);
      },
      error: (err: HttpErrorResponse) => {
        this.ns.error(err.error.message);
      },
    });
  }

  private loadDashboardData() {
    this.loading.set(true);

    //destroys the canvas to avoid conflicts on the re render when data arrives.
    if (this.expenseChart) {
      this.expenseChart.destroy();
      this.expenseChart = undefined;
    }

    this.transactionService.dashboard(this.filterForm.value).subscribe({
      next: (data) => {
        this.dashboardData.set(data);
        if (data.expensesByCategory) {
          this.loading.set(false);
          this.renderExpenseChart(data.expensesByCategory);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.ns.error(err.error.message);
        this.loading.set(false);
      },
    });
  }

  private loadAnnualData(accountId: string) {
    this.transactionService
      .getIncomeExpenseByMonth(accountId || undefined)
      .subscribe({
        next: (data) => {
          this.annualChartData.set(data);
        },
        error: (err: HttpErrorResponse) => {
          this.ns.error(err.error.message);
        },
      });
  }

  onAccountFilterChanged(accountId: string) {
    this.filterForm.get('annualAccountFilterId')?.setValue(accountId);
    this.loadAnnualData(accountId);
  }

  onAccountsSelected(selected: any[]) {
    this.filterForm.get('accountIds')?.setValue(selected.map((s) => s.id));
    this.filterTrigger.update((v) => v + 1);
  }

  onCategoriesSelected(selected: any[]) {
    this.filterForm.get('categoryIds')?.setValue(selected.map((s) => s.id));
    this.filterTrigger.update((v) => v + 1);
  }

  onTypesSelected(selected: any[]) {
    this.filterForm
      .get('transactionTypes')
      ?.setValue(selected.map((s) => s.id));
    this.filterTrigger.update((v) => v + 1);
  }

  applyFilters() {
    this.filterTrigger.update((v) => v + 1);
  }

  private renderExpenseChart(data: CategoryExpense[]) {
    const labels = data.map((item) => item.name);
    const values = data.map((item) => item.amount);
    const total = values.reduce((a, b) => a + b, 0);

    const labelsWithPercentage = labels.map((label, index) => {
      const val = values[index];
      const pct = total > 0 ? ((val / total) * 100).toFixed(1) : '0.0';
      return `${label} (${pct}%)`;
    });

    setTimeout(() => {
      const canvasElement = document.getElementById(
        'expensePie',
      ) as HTMLCanvasElement;
      if (!canvasElement) return;

      if (this.expenseChart) {
        this.expenseChart.destroy();
      }
      this.expenseChart = new Chart(canvasElement, {
        type: 'pie',
        data: {
          labels: labelsWithPercentage,
          datasets: [
            {
              data: values,
              backgroundColor: [
                '#22c55e',
                '#ef4444',
                '#3b82f6',
                '#f59e0b',
                '#8b5cf6',
                '#ec4899',
              ],
              borderColor: '#0b0f19',
              borderWidth: 2,
              hoverOffset: 4,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: false,
            },
            tooltip: {
              backgroundColor: '#0f172a',
              titleColor: '#f1f5f9',
              bodyColor: '#94a3b8',
              borderColor: '#334155',
              borderWidth: 1,
              padding: 10,
              callbacks: {
                label: (context) => {
                  const value = context.raw as number;
                  return ` $${value.toLocaleString('es-AR')}`;
                },
              },
            },
          },
        },
      });
    }, 0);
  }
}
