import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LanguageService } from '../i18n/language.service';

/** One entry of the country picker: ISO code plus its name in the UI language. */
export interface CountryOption {
  readonly code: string;
  readonly name: string;
}

/**
 * Country list for the Settings picker. Codes come from the backend
 * (`GET /countries`, the same list `PATCH /me` validates against); names come
 * from the browser's own CLDR data via `Intl.DisplayNames`, so nothing has to
 * be translated by hand in the i18n dictionaries.
 */
@Injectable({ providedIn: 'root' })
export class CountryService {
  private readonly http = inject(HttpClient);
  private readonly language = inject(LanguageService);

  /**
   * Fetched once per app load and cached: the list is static, and the UI
   * language it is sorted by cannot change without a page reload.
   */
  private readonly options$ = this.http
    .get<string[]>(`${environment.apiUrl}/countries`)
    .pipe(
      map((codes) => this.toOptions(codes)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

  /** Countries sorted by localized name. */
  getOptions(): Observable<CountryOption[]> {
    return this.options$;
  }

  /** Built once: the locale is fixed for the page lifetime (see LanguageService). */
  private readonly displayNames = this.createDisplayNames();

  /** Localized name for a code, falling back to the code where the browser has no CLDR data. */
  displayName(code: string): string {
    return this.displayNames?.of(code) ?? code;
  }

  private toOptions(codes: string[]): CountryOption[] {
    const locale = this.language.locale;
    return codes
      .map((code) => ({ code, name: this.displayName(code) }))
      .sort((a, b) => a.name.localeCompare(b.name, locale));
  }

  private createDisplayNames(): Intl.DisplayNames | null {
    try {
      return new Intl.DisplayNames([this.language.locale], { type: 'region' });
    } catch {
      // Very old browsers: the picker still works, it just shows raw codes.
      return null;
    }
  }
}
