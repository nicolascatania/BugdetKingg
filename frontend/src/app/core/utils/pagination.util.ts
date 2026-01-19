import { signal, Signal, computed } from '@angular/core';
import { BaseFilter, PageResponse } from '../interfaces/GenericFilter.interfaces';

/**
 * Pagination state manager utility
 * Centralizes pagination logic to avoid repetition across components
 * 
 * Usage:
 * ```typescript
 * this.paginationState = createPaginationState();
 * 
 * // In your component:
 * const filter: BaseFilter = this.paginationState.getFilter();
 * service.search(filter).subscribe(response => {
 *   this.paginationState.updateFromResponse(response);
 * });
 * 
 * // Change page
 * this.paginationState.goToPage(1);
 * ```
 */
export interface PaginationState {
  currentPage: Signal<number>;
  pageSize: Signal<number>;
  totalPages: Signal<number>;
  totalElements: Signal<number>;
  hasNextPage: ReturnType<typeof computed>;
  hasPreviousPage: ReturnType<typeof computed>;
  
  getFilter(): BaseFilter;
  updateFromResponse<T>(response: PageResponse<T>): void;
  goToPage(page: number): void;
  nextPage(): void;
  previousPage(): void;
  setPageSize(size: number): void;
  reset(): void;
}

/**
 * Creates a pagination state object with all necessary signals and computed properties
 * @param initialPageSize Default page size (default: 20)
 */
export function createPaginationState(initialPageSize: number = 20): PaginationState {
  const currentPage = signal(0);
  const pageSize = signal(initialPageSize);
  const totalPages = signal(0);
  const totalElements = signal(0);

  const hasNextPage = computed(() => currentPage() < totalPages() - 1);
  const hasPreviousPage = computed(() => currentPage() > 0);

  return {
    currentPage,
    pageSize,
    totalPages,
    totalElements,
    hasNextPage,
    hasPreviousPage,

    getFilter(): BaseFilter {
      return {
        page: currentPage(),
        size: pageSize()
      };
    },

    updateFromResponse<T>(response: PageResponse<T>): void {
      totalElements.set(response.page.totalElements);
      totalPages.set(response.page.totalPages);
    },

    goToPage(page: number): void {
      currentPage.set(Math.max(0, Math.min(page, totalPages() - 1)));
    },

    nextPage(): void {
      if (hasNextPage()) {
        currentPage.update(p => p + 1);
      }
    },

    previousPage(): void {
      if (hasPreviousPage()) {
        currentPage.update(p => p - 1);
      }
    },

    setPageSize(size: number): void {
      pageSize.set(size);
      currentPage.set(0); // Reset to first page when changing page size
    },

    reset(): void {
      currentPage.set(0);
      pageSize.set(initialPageSize);
      totalPages.set(0);
      totalElements.set(0);
    }
  };
}
