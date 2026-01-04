import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, Component, HostListener } from '@angular/core';
import { Chart, PieController, ArcElement, Tooltip, Legend } from 'chart.js';
import { MultiSelectComponent } from '../../../../shared/components/multiselect/multiselect';
import { FormGroup, FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DashboardFilter } from '../../interfaces/dashboardFilter.interface';
import { forkJoin } from 'rxjs';
import { AccountService } from '../../../accounts/services/AccountService';
import { OptionDTO } from '../../../../shared/models/OptionDTO.interface';
import { CategoryService } from '../../../categories/service/category-service';



Chart.register(PieController, ArcElement, Tooltip, Legend);
@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, MultiSelectComponent, ReactiveFormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard implements AfterViewInit {

  accounts: OptionDTO[] = [];
  categories: OptionDTO[] = [];
  
  types = [
    { id: 2, name: 'Income' },
    { id: 3, name: 'Expense' }
  ];





  fromDate!: string;
  toDate!: string;

  filterForm!: FormGroup;

  constructor(private fb: FormBuilder, private accountService: AccountService,
    private categoryService: CategoryService
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
      accountIds: [[]],        // multiselect accounts
      categoryIds: [[]],       // multiselect categories
      transactionTypes: [[]]   // multiselect types
    });


    forkJoin({
      accountsOptions: this.accountService.getOptions(),
      categoriesOptions: this.categoryService.getOptions()
    }).subscribe(({ accountsOptions, categoriesOptions }) => {
      this.accounts = accountsOptions;
      this.categories = categoriesOptions;
    })
  }

  ngAfterViewInit() {

    new Chart('expensePie', {
      type: 'pie',
      data: {
        labels: ['Purchase', 'Investment', 'Transfer', 'Adjustment'],
        datasets: [{
          data: [45000, 30000, 15000, 10000],
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              boxWidth: 12,
              padding: 12
            }
          }
        }
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
}
