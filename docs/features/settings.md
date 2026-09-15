# Settings

A home for user preferences at `/settings` (behind `AuthGuard`, entry in the sidebar nav). Today it holds appearance and language; it exists so later options (currency, notifications) have a place to land.

## What it does

- **Appearance** — light / dark, as two option cards. Writes through `ThemeService.setTheme`, which persists to `localStorage` (`budgetking-theme`) and flips the `dark` class on `<html>`; the pre-paint script in `index.html` reads the same key, so the choice survives reloads without a flash.
- **Language** — one option card per entry in `LANGUAGES` (`core/i18n/languages.ts`), labelled with the language's own name. Writes through `LanguageService.setLanguage`, which persists (`budgetking-lang`) and **reloads the page** on purpose: `LOCALE_ID` is fixed at bootstrap, so dates and amounts only follow the new locale after a reload. The page says so under the cards.

## Design decisions

- No backend, no new state: both preferences already lived in their services; the page is a view over them. Adding a persisted server-side preference later means changing the service, not the page.
- Option cards instead of the sidebar's compact controls (`app-theme-toggle`, `app-language-switcher`): a settings page has room to explain each choice. The sidebar controls stay — they are the quick path.
- `role="radiogroup"` / `role="radio"` + `aria-checked` on the cards, so the selection reads correctly to assistive technology.

## Files

`frontend/src/app/features/settings/pages/settings/` (component, template, css), `app.routes.ts` (`/settings`), `shared/components/side-bar/side-bar.ts` (nav entry), `public/i18n/{en,es,de,pl}.json` (`nav.settings`, `settings.*`).
