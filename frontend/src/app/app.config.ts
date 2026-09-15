import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import localeEsAr from '@angular/common/locales/es-AR';
import localePl from '@angular/common/locales/pl';
import { ApplicationConfig, LOCALE_ID, isDevMode, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideTransloco } from '@jsverse/transloco';

import { routes } from './app.routes';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { jwtInterceptor } from './core/interceptor/jwt-interceptor';
import { DEFAULT_LANGUAGE, LANGUAGES, localeFor, resolveInitialLanguage } from './core/i18n/languages';
import { TranslocoHttpLoader } from './core/i18n/transloco-loader';

// Angular only bundles en-US; every other locale's date/number/currency
// formats have to be registered explicitly. English needs nothing.
registerLocaleData(localeEsAr);
registerLocaleData(localeDe);
registerLocaleData(localePl);

/**
 * Decided once, before bootstrap: both Transloco's initial language and
 * Angular's `LOCALE_ID` come from the same answer, so copy and formats can
 * never disagree (see LanguageService for why a switch reloads the page).
 */
const initialLanguage = resolveInitialLanguage();

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      routes,
      // The public pages (landing, terms) scroll the window and link to in-page
      // anchors (`fragment="features"`, the terms table of contents), so the
      // router has to honour fragments and start each new page at the top.
      // The app shell scrolls its own <main>, so this does not affect it.
      withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'top' }),
    ),
    provideHttpClient(withFetch(), withInterceptors([jwtInterceptor])),
    provideTransloco({
      config: {
        availableLangs: LANGUAGES.map((language) => language.code),
        defaultLang: initialLanguage,
        fallbackLang: DEFAULT_LANGUAGE,
        // A key missing from es/de/pl falls back to the English text rather
        // than rendering the raw key.
        missingHandler: { useFallbackTranslation: true },
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
      },
      loader: TranslocoHttpLoader,
    }),
    { provide: LOCALE_ID, useValue: localeFor(initialLanguage) },
  ],
};
