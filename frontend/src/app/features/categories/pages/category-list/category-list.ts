import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { CategoryService } from '../../service/category-service';
import { CategoryDTO } from '../../interfaces/CategoryDTO.interface';
import { EditCategory } from '../../components/edit-category/edit-category';
import { NotificationService } from '../../../../core/services/NotificationService';
import { HttpErrorResponse } from '@angular/common/http';
import { PaginationComponent } from '../../../../shared/components/PaginationComponent/PaginationComponent';
import { createPaginationState, PaginationState } from '../../../../core/utils/pagination.util';

@Component({
  selector: 'app-category-list',
  imports: [CommonModule, EditCategory, PaginationComponent],
  templateUrl: './category-list.html',
  styleUrl: './category-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryList {

  private categoryService = inject(CategoryService);
  private notificationService = inject(NotificationService);

  isModalCategoryOpen = signal(false);
  selectedCategory = signal<CategoryDTO | null>(null);
  categories = signal<CategoryDTO[]>([]);
  paginationState: PaginationState = createPaginationState(20);

  // Trigger para recargar cuando sea necesario
  private loadTrigger = signal(0);

  constructor() {
    // Efecto: cuando loadTrigger cambia, carga categorías
    effect(() => {
      this.loadTrigger();
      this.loadCategories();
    });
  }

  ngOnInit() {
    this.loadTrigger.update(v => v + 1);
  }

  private loadCategories(): void {
    const filter = this.paginationState.getFilter();

    this.categoryService.search(filter).subscribe({
      next: (response) => {
        this.categories.set(response.content);
        this.paginationState.updateFromResponse(response);
      },
      error: (err) => {
        this.notificationService.error('Error loading categories');
        this.categories.set([]);
      }
    });
  }

  private reloadCategories(): void {
    this.loadTrigger.update(v => v + 1);
  }

  onPageChange(newPage: number) {
    this.paginationState.goToPage(newPage);
    this.reloadCategories();
  }

  openCategoryModal(category: CategoryDTO | null): void {
    this.isModalCategoryOpen.set(true);
    this.selectedCategory.set(category);
  }

  deleteCategory(category: CategoryDTO) {
    this.categoryService.delete(category.id).subscribe({
      next: () => this.reloadCategories(),
      error: (err: HttpErrorResponse) => {
        const message = err?.error?.message ?? 'Error deleting category';
        this.notificationService.error(message);
      }
    });
  }

  onCategorySaved() {
    this.isModalCategoryOpen.set(false);
    this.reloadCategories();
  }
}

