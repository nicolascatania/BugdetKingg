import { TranslocoService } from '@jsverse/transloco';
import { TranslocoDirective } from '@jsverse/transloco';
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  signal,
  OnInit,
} from '@angular/core';
import { CategoryService } from '../../service/category-service';
import { CategoryDTO } from '../../interfaces/CategoryDTO.interface';
import { EditCategory } from '../../components/edit-category/edit-category';
import { NotificationService } from '../../../../core/services/NotificationService';
import { HttpErrorResponse } from '@angular/common/http';
import { PaginationComponent } from '../../../../shared/components/PaginationComponent/PaginationComponent';
import {
  createPaginationState,
  PaginationState,
} from '../../../../core/utils/pagination.util';
import { RevealDirective } from '../../../../shared/directives/reveal.directive';
import { ImportCategories } from '../../components/import-categories/import-categories';
import { CategoryImportExportService } from '../../services/category-import-export-service';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [CommonModule, EditCategory, PaginationComponent, RevealDirective, ImportCategories, TranslocoDirective],
  templateUrl: './category-list.html',
  styleUrl: './category-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryList implements OnInit {
  private readonly transloco = inject(TranslocoService);

  private categoryService = inject(CategoryService);
  private notificationService = inject(NotificationService);
  private importExportService = inject(CategoryImportExportService);

  isModalCategoryOpen = signal(false);
  isImportModalOpen = signal(false);
  selectedCategory = signal<CategoryDTO | null>(null);
  categories = signal<CategoryDTO[]>([]);
  loading = signal(true);
  paginationState: PaginationState = createPaginationState(20);

  /** Disables the export button and shows progress while the CSV request is in flight. */
  readonly exporting = signal(false);

  /** Id of the category currently being deleted, if any — drives that row's spinner. */
  readonly deletingId = signal<string | null>(null);

  private loadTrigger = signal(0);

  constructor() {
    effect(() => {
      this.loadTrigger();
      this.loadCategories();
    });
  }

  ngOnInit() {
    this.reloadCategories();
  }

  private loadCategories(): void {
    this.loading.set(true);
    const filter = this.paginationState.getFilter();

    this.categoryService.search(filter).subscribe({
      next: (response) => {
        this.categories.set(response.content);
        this.paginationState.updateFromResponse(response);
        this.loading.set(false);
      },
      error: () => {
        this.notificationService.error(this.transloco.translate('categories.loadError'));
        this.categories.set([]);
        this.loading.set(false);
      },
    });
  }

  private reloadCategories(): void {
    this.loadTrigger.update((v) => v + 1);
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
    if (this.deletingId()) return;

    this.deletingId.set(category.id);

    this.categoryService.delete(category.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.reloadCategories();
      },
      error: (err: HttpErrorResponse) => {
        this.deletingId.set(null);
        this.notificationService.error(
          err?.error?.message ?? this.transloco.translate('categories.deleteError'),
        );
      },
    });
  }

  onCategorySaved() {
    this.isModalCategoryOpen.set(false);
    this.reloadCategories();
  }

  openImportModal(): void {
    this.isImportModalOpen.set(true);
  }

  onImportModalClosed(imported: boolean): void {
    this.isImportModalOpen.set(false);
    if (imported) this.reloadCategories();
  }

  exportCsv(): void {
    if (this.exporting()) return;

    this.exporting.set(true);

    this.importExportService.exportCategories().subscribe({
      next: (blob) => {
        this.exporting.set(false);
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = 'categories.csv';
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.exporting.set(false);
        this.notificationService.error(err?.error?.message ?? this.transloco.translate('categories.exportError'));
      },
    });
  }
}
