import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  input,
  output,
} from '@angular/core';
import {
  Chart,
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
} from 'chart.js';
import { MonthlyIncomeExpenseDTO } from '../../../transactions/interfaces/MonthlyIncomeExpenseDTO.interface';

Chart.register(
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
);

@Component({
  selector: 'app-expenses-income-each-month',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './expenses-income-each-month.html',
  styleUrl: './expenses-income-each-month.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExpensesIncomeEachMonth {
  accounts = input<any[]>([]);
  chartData = input<MonthlyIncomeExpenseDTO[]>([]);
  accountChanged = output<string>();

  private barChart?: Chart;

  constructor() {
    effect(() => {
      const data = this.chartData();
      this.updateOrRenderChart(data);
    });
  }

  onAccountChange(value: string) {
    this.accountChanged.emit(value);
  }

  private updateOrRenderChart(data: MonthlyIncomeExpenseDTO[]) {
    if (!data || data.length === 0) return;

    const canvasElement = document.getElementById(
      'incomeExpenseChart',
    ) as HTMLCanvasElement;
    if (!canvasElement) return;

    const incomeData = Array(12).fill(0);
    const expenseData = Array(12).fill(0);

    data.forEach((item) => {
      const index = item.month - 1;
      if (index >= 0 && index < 12) {
        incomeData[index] = item.income;
        expenseData[index] = item.expense;
      }
    });

    if (this.barChart) {
      this.barChart.data.datasets[0].data = incomeData;
      this.barChart.data.datasets[1].data = expenseData;
      this.barChart.update();
      return;
    }

    this.barChart = new Chart(canvasElement, {
      type: 'bar',
      data: {
        labels: [
          'Jan',
          'Feb',
          'Mar',
          'Apr',
          'May',
          'Jun',
          'Jul',
          'Aug',
          'Sep',
          'Oct',
          'Nov',
          'Dec',
        ],
        datasets: [
          {
            label: 'Income',
            data: incomeData,
            backgroundColor: '#22c55e',
            borderRadius: 6,
            borderWidth: 0,
            barPercentage: 0.6,
            categoryPercentage: 0.7,
          },
          {
            label: 'Expense',
            data: expenseData,
            backgroundColor: '#ef4444',
            borderRadius: 6,
            borderWidth: 0,
            barPercentage: 0.6,
            categoryPercentage: 0.7,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: '#94a3b8',
              font: { size: 11 },
              padding: 12,
              usePointStyle: true,
              pointStyle: 'circle',
            },
          },
          tooltip: {
            backgroundColor: '#0f172a',
            titleColor: '#f1f5f9',
            bodyColor: '#94a3b8',
            borderColor: '#334155/40',
            borderWidth: 1,
            padding: 10,
          },
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#64748b', font: { size: 11, weight: 'normal' } },
          },
          y: {
            grid: { color: '#1e293b/40' },
            border: { dash: [4, 4] },
            ticks: {
              color: '#64748b',
              font: { size: 11, weight: 'normal' },
              callback: (value) => `$${value}`,
            },
          },
        },
      },
    });
  }
}
