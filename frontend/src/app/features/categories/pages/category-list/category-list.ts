import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { CategoryService } from '../../service/category-service';
import { CategoryDTO } from '../../interfaces/CategoryDTO.interface';
import { EditCategory } from '../../components/edit-category/edit-category';
import { NotificationService } from '../../../../core/services/NotificationService';

@Component({
  selector: 'app-category-list',
  imports: [CommonModule, EditCategory],
  templateUrl: './category-list.html',
  styleUrl: './category-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryList {



  categories = signal<CategoryDTO[]>([]);

  isModalCategoryOpen = signal(false);

  selectedCategory = signal<CategoryDTO | null>(null);

  constructor(private categoryService: CategoryService, private notificationService: NotificationService) { }


  ngOnInit() {
    this.loadCategories();
  }

  public loadCategories(): void {
    this.categoryService.search().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (err) => {
        this.notificationService.error(err);
        this.categories.set([]);
      }
    });
  }


  openCategoryModal(category: CategoryDTO | null): void {
    this.isModalCategoryOpen.set(true);
    this.selectedCategory.set(category);
  }

  deleteCategory(category: CategoryDTO) {
    this.categoryService.delete(category.id).subscribe({
      next: () => this.loadCategories(),
      error: (err) => {
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

