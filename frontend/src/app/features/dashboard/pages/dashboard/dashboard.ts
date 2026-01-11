import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener, signal } from '@angular/core';
import { Chart, PieController, ArcElement, Tooltip, Legend } from 'chart.js';
import { MultiSelectComponent } from '../../../../shared/components/multiselect/multiselect';
import { FormGroup, FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DashboardFilter } from '../../interfaces/dashboardFilter.interface';
import { forkJoin } from 'rxjs';
import { AccountService } from '../../../accounts/services/AccountService';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { CategoryService } from '../../../categories/service/category-service';
import { TransactionService } from '../../../transactions/services/transaction-service';
import { DashBoardDTO } from '../../interfaces/DashBoardDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';
import { HttpErrorResponse } from '@angular/common/http';
import { LastMoves } from "../../../home/components/last-moves/last-moves";



Chart.register(PieController, ArcElement, Tooltip, Legend);
@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, ReactiveFormsModule, LastMoves],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard  {


  private expenseChart?: Chart;


  fromDate!: string;
  toDate!: string;
  filterForm!: FormGroup;

  dashboardData = signal<DashBoardDTO | null>(null);

  constructor(private fb: FormBuilder, private ns: NotificationService,
    private transactionService: TransactionService,
    private cdr: ChangeDetectorRef
  ) { }

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



    this.transactionService.dashboard(this.filterForm.value).subscribe({
      next: (data) => {
        this.dashboardData.set(data);

        if (data.expensesByCategory) {
          this.renderExpenseChart(data.expensesByCategory);
        }

        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.ns.error(err.error.message);
      }
    });


  }

  onAccountsSelected(selected: any[]) {
    this.filterForm.get('accountIds')?.setValue(selected.map(s => s.id));
  }

  onCategoriesSelected(selected: any[]) {
    this.filterForm.get('categoryIds')?.setValue(selected.map(s => s.id));
  }

  onTypesSelected(selected: any[]) {
    this.filterForm.get('transactionTypes')?.setValue(selected.map(s => s.id));
  }

  // Submit
  applyFilters() {
    const filters: DashboardFilter = this.filterForm.value;
    console.log('Filters applied:', filters);
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
