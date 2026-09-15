import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { jwtInterceptor } from './core/interceptor/jwt-interceptor';

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
  ],
};
