import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  signal
} from '@angular/core';
import {
  Chart,
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend
} from 'chart.js';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { AccountService } from '../../../accounts/services/AccountService';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { MonthlyIncomeExpenseDTO } from '../../../transactions/interfaces/MonthlyIncomeExpenseDTO.interface';

Chart.register(
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend
);

@Component({
  selector: 'app-expenses-income-each-month',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './expenses-income-each-month.html',
  styleUrl: './expenses-income-each-month.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExpensesIncomeEachMonth implements OnInit {

  /** Chart instance */
  private chart?: Chart;

  /** Available accounts for filtering */
  accounts = signal<OptionDTO[]>([]);

  /** Currently selected account ID (undefined = all accounts) */
  selectedAccountId = signal<string | undefined>(undefined);

  constructor(
    private transactionService: TransactionService,
    private accountService: AccountService
  ) {}

  ngOnInit(): void {
    this.loadAccounts();
    this.loadChartData();
  }

  /**
   * Loads the available accounts for the select filter.
   */
  private loadAccounts(): void {
    this.accountService.getOptions().subscribe(accounts => {
      this.accounts.set(accounts);
    });
  }

  /**
   * Triggered when the account filter changes.
   */
  onAccountChange(accountId: string): void {
    this.selectedAccountId.set(accountId || undefined);
    this.loadChartData();
  }

  /**
   * Loads data from backend and renders or updates the chart.
   */
  private loadChartData(): void {
    this.transactionService
      .getIncomeExpenseByMonth(this.selectedAccountId())
      .subscribe(data => this.renderChart(data));
  }

  /**
   * Renders or updates the bar chart.
   */
  private renderChart(data: MonthlyIncomeExpenseDTO[]): void {

    const labels = [
      'Jan','Feb','Mar','Apr','May','Jun',
      'Jul','Aug','Sep','Oct','Nov','Dec'
    ];

    const incomeData = new Array(12).fill(0);
    const expenseData = new Array(12).fill(0);

    // Map backend data to fixed 12-month structure
    data.forEach(d => {
      incomeData[d.month - 1] = d.income;
      expenseData[d.month - 1] = d.expense;
    });

    if (this.chart) {
      this.chart.data.datasets[0].data = incomeData;
      this.chart.data.datasets[1].data = expenseData;
      this.chart.update();
      return;
    }

    this.chart = new Chart('incomeExpenseChart', {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Income',
            data: incomeData,
            backgroundColor: '#22c55e'
          },
          {
            label: 'Expense',
            data: expenseData,
            backgroundColor: '#ef4444'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom'
          }
        }
      }
    });
  }
}
