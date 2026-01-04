import { AfterViewInit, ChangeDetectionStrategy, Component } from '@angular/core';
import { Chart, PieController, ArcElement, Tooltip, Legend } from 'chart.js';

declare var TomSelect: any;


Chart.register(PieController, ArcElement, Tooltip, Legend);
@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard implements AfterViewInit {


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

    new TomSelect('#category-select', {
      plugins: ['remove_button'], // Botón para quitar cada tag
      persist: false,
      create: false,
      placeholder: 'Select categories...',
    });

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
}
