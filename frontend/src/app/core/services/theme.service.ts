import { Injectable, signal, computed, effect } from '@angular/core';

/** The two themes the design system ships tokens for. */
export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'budgetking-theme';

/**
 * Owns the active colour theme.
 *
 * The theme is expressed as a single `dark` class on the <html> element; all
 * colours are CSS custom properties (see `styles.css`), so toggling the class
 * re-themes the whole app without any component having to know about it.
 *
 * Resolution order: explicit user choice (localStorage) > OS preference.
 * A small inline script in `index.html` applies the same rule before the first
 * paint to avoid a flash of the wrong theme.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _theme = signal<Theme>(this.resolveInitialTheme());

  /** Currently applied theme. */
  readonly theme = this._theme.asReadonly();

  /** Convenience flag for templates. */
  readonly isDark = computed(() => this._theme() === 'dark');

  constructor() {
    // Keep the DOM and the persisted preference in sync with the signal.
    effect(() => this.applyTheme(this._theme()));

    // Follow the OS while the user has not made an explicit choice.
    if (typeof window !== 'undefined' && window.matchMedia && !this.storedTheme()) {
      window
        .matchMedia('(prefers-color-scheme: dark)')
        .addEventListener('change', (event) => {
          if (!this.storedTheme()) {
            this._theme.set(event.matches ? 'dark' : 'light');
          }
        });
    }
  }

  /** Switches to the opposite theme and remembers the choice. */
  toggle(): void {
    this.setTheme(this._theme() === 'dark' ? 'light' : 'dark');
  }

  /** Applies and persists an explicit theme. */
  setTheme(theme: Theme): void {
    this._theme.set(theme);
    this.persist(theme);
  }

  private applyTheme(theme: Theme): void {
    if (typeof document === 'undefined') {
      return;
    }
    const root = document.documentElement;
    root.classList.toggle('dark', theme === 'dark');
    root.style.colorScheme = theme;
  }

  private resolveInitialTheme(): Theme {
    const stored = this.storedTheme();
    if (stored) {
      return stored;
    }
    const prefersDark =
      typeof window !== 'undefined' &&
      window.matchMedia?.('(prefers-color-scheme: dark)').matches;
    return prefersDark ? 'dark' : 'light';
  }

  private storedTheme(): Theme | null {
    try {
      const value = localStorage.getItem(STORAGE_KEY);
      return value === 'dark' || value === 'light' ? value : null;
    } catch {
      // Storage can be unavailable (private mode, blocked cookies).
      return null;
    }
  }

  private persist(theme: Theme): void {
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      // Preference simply will not survive a reload; not worth failing over.
    }
  }
}
