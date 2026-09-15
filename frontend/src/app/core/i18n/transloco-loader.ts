import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Translation, TranslocoLoader } from '@jsverse/transloco';
import { Observable } from 'rxjs';

/**
 * Fetches `public/i18n/<lang>.json`. The path is relative on purpose: the
 * app is served under `/BugdetKingg/` on GitHub Pages, and a relative URL
 * resolves against `<base href>` while an absolute `/i18n/...` would not.
 */
@Injectable({ providedIn: 'root' })
export class TranslocoHttpLoader implements TranslocoLoader {
  private readonly http = inject(HttpClient);

  getTranslation(lang: string): Observable<Translation> {
    return this.http.get<Translation>(`i18n/${lang}.json`);
  }
}
