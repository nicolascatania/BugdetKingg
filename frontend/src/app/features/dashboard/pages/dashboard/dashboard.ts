import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, signal } from '@angular/core';
import { Chart, PieController, ArcElement, Tooltip, Legend } from 'chart.js';
import { FormGroup, FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DashboardFilter } from '../../interfaces/dashboardFilter.interface';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { DashBoardDTO } from '../../interfaces/DashBoardDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';
import { HttpErrorResponse } from '@angular/common/http';
import { LastMoves } from "../../../home/components/last-moves/last-moves";
import { ExpensesIncomeEachMonth } from "../../components/expenses-income-each-month/expenses-income-each-month";



Chart.register(PieController, ArcElement, Tooltip, Legend);
@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, ReactiveFormsModule, LastMoves, ExpensesIncomeEachMonth],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard {


  private expenseChart?: Chart;

  fromDate!: string;
  toDate!: string;
  filterForm!: FormGroup;

  // State management with signals
  dashboardData = signal<DashBoardDTO | null>(null);
  filterTrigger = signal(0);  // Trigger para cambios de filtro

  constructor(private fb: FormBuilder, private ns: NotificationService,
    private transactionService: TransactionService
  ) {
    // ✅ Effect reactivo que se ejecuta cuando hay cambios
    effect(() => {
      this.filterTrigger();  // Leer trigger para dependency tracking
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
    });

    // Initial load
    this.filterTrigger.update(v => v + 1);
  }

  // ✅ Método para cargar datos del dashboard
  private loadDashboardData() {
    this.transactionService.dashboard(this.filterForm.value).subscribe({
      next: (data) => {
        this.dashboardData.set(data);  // Signal se actualiza automáticamente

        if (data.expensesByCategory) {
          this.renderExpenseChart(data.expensesByCategory);
        }
        // ✅ No necesita markForCheck() - Signal lo maneja
      },
      error: (err: HttpErrorResponse) => {
        this.ns.error(err.error.message);
      }
    });
  }

  onAccountsSelected(selected: any[]) {
    this.filterForm.get('accountIds')?.setValue(selected.map(s => s.id));
    this.filterTrigger.update(v => v + 1);  // ✅ Trigger a recarga
  }

  onCategoriesSelected(selected: any[]) {
    this.filterForm.get('categoryIds')?.setValue(selected.map(s => s.id));
    this.filterTrigger.update(v => v + 1);  // ✅ Trigger a recarga
  }

  onTypesSelected(selected: any[]) {
    this.filterForm.get('transactionTypes')?.setValue(selected.map(s => s.id));
    this.filterTrigger.update(v => v + 1);  // ✅ Trigger a recarga
  }

  // Submit - Aplica filtros y recarga
  applyFilters() {
    const filters: DashboardFilter = this.filterForm.value;
    console.log('Filters applied:', filters);
    this.filterTrigger.update(v => v + 1);  // ✅ Trigger a recarga
  }

  private renderExpenseChart(data: Record<string, number>) {
    const labels = Object.keys(data);
    const values = Object.values(data);

    if (this.expenseChart) {
      this.expenseChart.data.labels = labels;
      this.expenseChart.data.datasets[0].data = values;
      this.expenseChart.update();
      return;
    }

    this.expenseChart = new Chart('expensePie', {
      type: 'pie',
      data: {
        labels,
        datasets: [
          {
            data: values,
            backgroundColor: [
              '#22c55e',
              '#ef4444',
              '#3b82f6',
              '#f59e0b',
              '#8b5cf6',
              '#ec4899'
            ],
            borderWidth: 1
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom'
          },
          tooltip: {
            callbacks: {
              label: (context) => {
                const label = context.label ?? '';
                const value = context.raw as number;
                const total = values.reduce((a, b) => a + b, 0);
                const percentage = ((value / total) * 100).toFixed(1);

                return `${label}: $${value} (${percentage}%)`;
              }
            }
          }
        }
      }
    });
  }

}
