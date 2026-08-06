import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/**
 * Pagination control shared by every paged list.
 *
 * Renders a compact numeric pager with ellipses so the width stays stable no
 * matter how many pages exist, and collapses to prev/next plus a "page x of y"
 * summary on narrow screens.
 */
@Component({
  selector: 'app-pagination',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav
      class="flex flex-col items-center justify-between gap-3 border-t border-line px-1 py-4 sm:flex-row"
      aria-label="Pagination"
    >
      <p class="text-xs text-muted">
        Showing
        <span class="font-semibold text-body">{{ rangeStart() }}</span>
        –
        <span class="font-semibold text-body">{{ rangeEnd() }}</span>
        of
        <span class="font-semibold text-body">{{ totalElements() }}</span>
        results
      </p>

      <div class="flex items-center gap-1.5">
        <button
          type="button"
          class="pager-btn"
          [disabled]="page() === 0"
          (click)="changePage.emit(page() - 1)"
          aria-label="Previous page"
        >
          <i class="fa-solid fa-chevron-left text-[10px]"></i>
        </button>

        <!-- Numeric pages: hidden on phones, where the summary below is enough -->
        <div class="hidden items-center gap-1.5 sm:flex">
          @for (item of pages(); track $index) {
            @if (item === null) {
              <span class="px-1 text-xs text-subtle">…</span>
            } @else {
              <button
                type="button"
                class="pager-btn"
                [class.is-current]="item === page()"
                [attr.aria-current]="item === page() ? 'page' : null"
                (click)="changePage.emit(item)"
              >
                {{ item + 1 }}
              </button>
            }
          }
        </div>

        <span class="text-xs font-semibold text-muted sm:hidden">
          {{ page() + 1 }} / {{ totalPages() || 1 }}
        </span>

        <button
          type="button"
          class="pager-btn"
          [disabled]="page() >= totalPages() - 1"
          (click)="changePage.emit(page() + 1)"
          aria-label="Next page"
        >
          <i class="fa-solid fa-chevron-right text-[10px]"></i>
        </button>
      </div>
    </nav>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .pager-btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 36px;
        height: 36px;
        padding: 0 0.5rem;
        border-radius: 0.625rem;
        border: 1px solid rgb(var(--color-line));
        background-color: rgb(var(--color-surface-2));
        color: rgb(var(--color-text-muted));
        font-size: 0.75rem;
        font-weight: 600;
        cursor: pointer;
        transition:
          background-color 180ms ease,
          color 180ms ease,
          border-color 180ms ease,
          transform 180ms cubic-bezier(0.34, 1.56, 0.64, 1);
      }

      .pager-btn:hover:not(:disabled) {
        border-color: rgb(var(--color-brand) / 0.45);
        color: rgb(var(--color-brand));
        transform: translateY(-1px);
      }

      .pager-btn:active:not(:disabled) {
        transform: scale(0.95);
      }

      .pager-btn:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .pager-btn.is-current {
        background-color: rgb(var(--color-brand));
        border-color: rgb(var(--color-brand));
        color: rgb(var(--color-brand-contrast));
      }

      @media (prefers-reduced-motion: reduce) {
        .pager-btn {
          transition: none;
        }
      }
    `,
  ],
})
export class PaginationComponent {
  readonly page = input.required<number>();
  readonly size = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly changePage = output<number>();

  /** 1-based index of the first row on the current page. */
  readonly rangeStart = computed(() =>
    this.totalElements() === 0 ? 0 : this.page() * this.size() + 1,
  );

  /** 1-based index of the last row on the current page. */
  readonly rangeEnd = computed(() =>
    Math.min((this.page() + 1) * this.size(), this.totalElements()),
  );

  /**
   * Page numbers to render. `null` marks an ellipsis. Always keeps the first
   * and last page plus a window around the current one, so the control never
   * changes width as the user pages through a long list.
   */
  readonly pages = computed<(number | null)[]>(() => {
    const total = this.totalPages();
    const current = this.page();

    if (total <= 7) {
      return Array.from({ length: total }, (_, index) => index);
    }

    const windowStart = Math.max(1, Math.min(current - 1, total - 4));
    const windowEnd = Math.min(total - 2, Math.max(current + 1, 3));

    const result: (number | null)[] = [0];

    if (windowStart > 1) {
      result.push(null);
    }
    for (let index = windowStart; index <= windowEnd; index++) {
      result.push(index);
    }
    if (windowEnd < total - 2) {
      result.push(null);
    }
    result.push(total - 1);

    return result;
  });
}
