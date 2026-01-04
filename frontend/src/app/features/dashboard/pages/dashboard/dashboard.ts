import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, Component, HostListener } from '@angular/core';
import { Chart, PieController, ArcElement, Tooltip, Legend } from 'chart.js';
import { MultiSelectComponent } from '../../../../shared/components/multiselect/multiselect';



Chart.register(PieController, ArcElement, Tooltip, Legend);
@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, MultiSelectComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard implements AfterViewInit {

  accounts = [
    { id: 1, name: 'All' },
    { id: 2, name: 'Cash' },
    { id: 3, name: 'Bank' },
    { id: 4, name: 'Credit Card' }
  ];

  types = [
    { id: 1, name: 'All' },
    { id: 2, name: 'Income' },
    { id: 3, name: 'Expense' }
  ];



  categories = [
    { id: 1, name: 'Food' },
    { id: 2, name: 'Transport' },
    { id: 3, name: 'Services' },
    { id: 4, name: 'Investment' }
  ];

  fromDate!: string;
  toDate!: string;

  ngOnInit() {
    const now = new Date();

    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    this.fromDate = firstDay.toISOString().split('T')[0];
    this.toDate = lastDay.toISOString().split('T')[0];
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


  onCategoriesSelected(selected: any[]) {
    console.log('Seleccionados:', selected);
  }


  onAccountsSelected(selected: any[]) {
    console.log('Accounts selected:', selected);
  }

  onTypesSelected(selected: any[]) {
    console.log('Types selected:', selected);
  }
}
