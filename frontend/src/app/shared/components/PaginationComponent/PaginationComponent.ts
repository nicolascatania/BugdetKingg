// pagination.component.ts
import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  template: `
    <div class="flex items-center justify-between px-4 py-3 border-t border-border mt-4">
      <div class="text-sm text-slate-400">
        Showing <span class="font-medium text-white">{{ (page() * size()) + 1 }}</span> 
        to <span class="font-medium text-white">{{ Math.min((page() + 1) * size(), totalElements()) }}</span> 
        of <span class="font-medium text-white">{{ totalElements() }}</span> results
      </div>
      <div class="flex gap-2">
        <button 
          [disabled]="page() === 0"
          (click)="changePage.emit(page() - 1)"
          class="px-3 py-1 rounded border border-border hover:bg-secondary disabled:opacity-50 transition-colors">
          <i class="fa-solid fa-chevron-left text-xs"></i>
        </button>
        
        <button 
          [disabled]="page() >= totalPages() - 1"
          (click)="changePage.emit(page() + 1)"
          class="px-3 py-1 rounded border border-border hover:bg-secondary disabled:opacity-50 transition-colors">
          <i class="fa-solid fa-chevron-right text-xs"></i>
        </button>
      </div>
    </div>
  `
})
export class PaginationComponent {
  page = input.required<number>();
  size = input.required<number>();
  totalElements = input.required<number>();
  totalPages = input.required<number>();
  changePage = output<number>();
  
  protected readonly Math = Math;
}