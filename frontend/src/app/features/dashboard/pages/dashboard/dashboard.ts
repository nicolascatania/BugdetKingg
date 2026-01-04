import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, Component, HostListener } from '@angular/core';
import { Chart, PieController, ArcElement, Tooltip, Legend } from 'chart.js';



Chart.register(PieController, ArcElement, Tooltip, Legend);
@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard implements AfterViewInit {
  categories = ['Food', 'Transport', 'Services', 'Investment'];
  selectedCategories: string[] = [];
  dropdownOpen = false;



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


  toggleDropdown() {
    this.dropdownOpen = !this.dropdownOpen;
  }

  selectCategory(cat: string) {
    if (!this.selectedCategories.includes(cat)) {
      this.selectedCategories.push(cat);
    } else {
      this.removeCategory(cat);
    }
  }

  removeCategory(cat: string) {
    this.selectedCategories = this.selectedCategories.filter(c => c !== cat);
  }

  @HostListener('document:click', ['$event'])
  handleClickOutside(event: MouseEvent) {
    const target = event.target as HTMLElement;
    // Si el click no fue dentro de tu dropdown, cerralo
    if (!target.closest('.category-dropdown')) {
      this.dropdownOpen = false;
    }
  }
}
