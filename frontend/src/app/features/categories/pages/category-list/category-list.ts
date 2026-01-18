import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { CategoryService } from '../../service/category-service';
import { CategoryDTO } from '../../interfaces/CategoryDTO.interface';
import { EditCategory } from '../../components/edit-category/edit-category';
import { NotificationService } from '../../../../core/services/NotificationService';
import { HttpErrorResponse } from '@angular/common/http';
import { BaseFilter } from '../../../../core/interfaces/GenericFilter.interfaces';
import { PaginationComponent } from '../../../../shared/components/PaginationComponent/PaginationComponent';

@Component({
  selector: 'app-category-list',
  imports: [CommonModule, EditCategory, PaginationComponent],
  templateUrl: './category-list.html',
  styleUrl: './category-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryList {

  categories = signal<CategoryDTO[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  pageSize = signal(20);


  isModalCategoryOpen = signal(false);

  selectedCategory = signal<CategoryDTO | null>(null);

  constructor(private categoryService: CategoryService, private notificationService: NotificationService) { }


  ngOnInit() {
    this.loadCategories();
  }

  public loadCategories(): void {
    const filter: BaseFilter = {
      page: this.currentPage(),
      size: this.pageSize()
    };

    this.categoryService.search(filter).subscribe({
      next: (response) => {
        this.categories.set(response.content);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
      },
      error: (err) => {
        this.notificationService.error('Error loading categories');
        this.categories.set([]);
      }
    });

  }

  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadCategories();
  }

  openCategoryModal(category: CategoryDTO | null): void {
    this.isModalCategoryOpen.set(true);
    this.selectedCategory.set(category);
  }

  deleteCategory(category: CategoryDTO) {
    this.categoryService.delete(category.id).subscribe({
      next: () => this.loadCategories(),
      error: (err: HttpErrorResponse) => {
        const message =
          err?.error?.message ?? 'Error deleting category';

        this.notificationService.error(message);
      }
    });
  }

  onCategorySaved() {
    this.isModalCategoryOpen.set(false);
    this.loadCategories();
  }

}

