import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
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
import { chartTheme, chartTooltipStyle } from '../../../../shared/utils/chartTheme';
import { ThemeService } from '../../../../core/services/theme.service';

Chart.register(
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
);

const MONTH_LABELS = [
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
];

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
  private readonly themeService = inject(ThemeService);

  constructor() {
    effect(() => {
      const data = this.chartData();
      this.updateOrRenderChart(data);
    });

    // Chart.js paints to a canvas and cannot inherit CSS variables, so the
    // whole chart is rebuilt when the theme changes.
    effect(() => {
      this.themeService.theme();
      if (this.barChart) {
        this.barChart.destroy();
        this.barChart = undefined;
        this.updateOrRenderChart(this.chartData());
      }
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

    const theme = chartTheme();

    this.barChart = new Chart(canvasElement, {
      type: 'bar',
      data: {
        labels: MONTH_LABELS,
        datasets: [
          {
            label: 'Income',
            data: incomeData,
            backgroundColor: theme.positive,
            borderRadius: 6,
            borderWidth: 0,
            barPercentage: 0.62,
            categoryPercentage: 0.7,
          },
          {
            label: 'Expense',
            data: expenseData,
            backgroundColor: theme.negative,
            borderRadius: 6,
            borderWidth: 0,
            barPercentage: 0.62,
            categoryPercentage: 0.7,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 650, easing: 'easeOutQuart' },
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: theme.muted,
              font: { size: 11 },
              padding: 12,
              usePointStyle: true,
              pointStyle: 'circle',
            },
          },
          tooltip: chartTooltipStyle(),
        },
        scales: {
          x: {
            grid: { display: false },
            border: { color: theme.grid },
            ticks: { color: theme.muted, font: { size: 11 } },
          },
          y: {
            grid: { color: theme.grid },
            border: { dash: [4, 4], color: theme.grid },
            ticks: {
              color: theme.muted,
              font: { size: 11 },
              // Compact axis labels keep the plot area readable on phones.
              callback: (value) => `$${Number(value).toLocaleString('es-AR')}`,
            },
          },
        },
      },
    });
  }
}
