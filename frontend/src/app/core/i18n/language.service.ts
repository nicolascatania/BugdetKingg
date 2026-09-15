import { Injectable, inject, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import {
  LANGUAGES,
  LANGUAGE_STORAGE_KEY,
  LanguageCode,
  localeFor,
  resolveInitialLanguage,
} from './languages';

/**
 * Owns the active UI language.
 *
 * The language is resolved once, before bootstrap (`resolveInitialLanguage`),
 * because Angular's `LOCALE_ID` — which drives every `date`, `number` and
 * `currency` pipe — is fixed for the lifetime of the app. Switching therefore
 * persists the choice and reloads the page: one code path, and dates, amounts
 * and copy are guaranteed to change together rather than leaving the app half
 * translated until the next navigation.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly transloco = inject(TranslocoService);

  readonly languages = LANGUAGES;

  private readonly currentSignal = signal<LanguageCode>(resolveInitialLanguage());
  readonly current = this.currentSignal.asReadonly();

  constructor() {
    // Screen readers and hyphenation key off <html lang>; keep it in step with the UI.
    document.documentElement.lang = this.currentSignal();
  }

  /** Angular locale id for the active language (e.g. `es-AR`). */
  get locale(): string {
    return localeFor(this.currentSignal());
  }

  /** Persists the choice and reloads so `LOCALE_ID` follows the new language. */
  setLanguage(code: LanguageCode): void {
    if (code === this.currentSignal()) {
      return;
    }
    try {
      localStorage.setItem(LANGUAGE_STORAGE_KEY, code);
    } catch {
      // Storage blocked: the change still applies for this page load below.
    }
    this.currentSignal.set(code);
    this.transloco.setActiveLang(code);
    window.location.reload();
  }
}
