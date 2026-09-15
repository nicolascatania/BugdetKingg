# User region (country)

Status: **shipped**. Decided 2026-09-15.

The home page used to show two Argentina-only tiles to everyone: the official USD/ARS
quote (`dolarapi.com`) and INDEC monthly inflation. For a user in Poland or Germany that
is visual noise plus two external API calls they cannot use. This change makes those
tiles depend on where the user lives.

## Intent

- A user picks their **country** once, in Settings. It follows them across devices.
- Argentina keeps the dollar + inflation tiles. Any other country, or no choice yet,
  shows nothing region-specific.
- Adding country-specific data for another country later must be a local change
  (one registry entry + one widget), not a rewrite.

## Decisions and why

### Country, not "show Argentine data" toggle, not language

- **Country ≠ language ≠ currency.** Language is what the UI says (already in
  Settings, localStorage). Currency will live per *account*
  (`docs/proposals/multi-currency.md`), because one user can hold ARS and USD.
  Country is the third thing: which macro data matters to this person. Keeping them
  separate means none of them has to be inferred from another.
- **Not derived from the language.** `es` is Argentina, Spain, Mexico… A wrong
  inference is worse than one explicit question.
- **Not a plain on/off toggle for the Argentine tiles.** A toggle is the bluntest fix
  today but dies the moment a second country wants its own data. Country + registry
  costs about the same and scales.

### Stored on the backend, not in localStorage

- Theme and language are *device* preferences, so localStorage fits them. Country is
  part of who the user is; sign-in is Google, so multi-device is the normal case.
- `AppUser.country` (`VARCHAR(2)`, nullable) is exposed on `GET /me` and updated with
  `PATCH /me { "country": "AR" | null }`. `null` clears the choice. Hibernate
  `ddl-auto=update` adds the column; no migration tooling exists in this repo.
- Alternative considered: localStorage now, backend "when multi-currency lands".
  Rejected because the backend change is small (one column, one endpoint) and doing it
  twice is more work than doing it once.

### Hidden until chosen

- A user with no country sees no market tiles. No timezone or language guessing.
- Consequence: existing Argentine users lose the tiles until they set the country
  once. Accepted as a one-time cost; the Settings hint tells them what the setting
  unlocks.
- Alternative considered: default `AR` from
  `Intl.DateTimeFormat().resolvedOptions().timeZone` (`America/Argentina/*`). More
  convenient, but a guess; the owner chose explicit over clever.

### No hand-written country list anywhere

- Backend: `IsoCountries` wraps `Locale.getISOCountries()` (JDK, ISO 3166-1 alpha-2).
  Used both to validate `PATCH /me` and to serve `GET /countries`, so the list the
  picker shows and the list the server accepts are the same object.
- Frontend: names come from the browser's CLDR data via
  `Intl.DisplayNames(locale, { type: 'region' })` — localized for free in `en`, `es`,
  `de`, `pl` without a single new dictionary entry. Falls back to the raw code on a
  browser without `Intl.DisplayNames` (pre-2021).
- Alternative considered: a 250-entry constant in the frontend. Rejected: two sources
  of truth for one list. `i18n-iso-countries` (npm) rejected: ships every locale's
  names when the browser already has them.

### Native `<select>`, not a custom dropdown

- ~250 entries need search. The browser's `<select>` has type-ahead on desktop and a
  native scrolling picker on phones, is keyboard- and screen-reader-accessible out of
  the box, and needs zero component code. Styled with the existing `.field-select`
  primitive.
- `tom-select` is in `package.json` but unused in `src/`; the only in-house select
  (`app-multiselect`) is multi-value only. Neither was a fit.
- Selection is bound with `[selected]` on each `<option>`, not `[value]` on the
  `<select>`: options render after the select, so a value set on the select would not
  stick.

### Region registry, not `if (country === 'AR')`

- `frontend/src/app/core/regions/regions.ts` maps a country to the market widgets the
  home heading renders (`AR` → `usd-ars`, `inflation-indec`). `RegionService` derives
  everything from the profile signal; components only ask `hasWidget(...)`.
- The heading requests each external feed only while its widget is enabled
  (`toObservable(showX).pipe(switchMap(...))`), so non-Argentine users make no calls to
  `dolarapi.com` / the Argentina API. Skeletons only appear for enabled feeds.
- Adding e.g. Poland = one `REGIONS` entry + one widget block in the heading. Nothing
  else changes.

## Surface

Backend (`BudgetKing/`):

- `model/AppUser.country`
- `utils/IsoCountries`
- `security/dto/UserProfileDTO` (`country`, `from(AppUser)`), `security/dto/UpdateProfileRequest`
- `security/service/UserProfileService`
- `security/controller/UserProfileController` — `GET /me`, `PATCH /me`
- `controller/CountryController` — `GET /countries` (sorted ISO codes)
- `SecurityConfig` CORS now allows `PATCH` (it did not before; the browser would have blocked the preflight).
- Tests: `IsoCountriesTest`, `UpdateProfileRequestTest`, `UserProfileServiceTest`, `UserProfileControllerTest` (auth, validation, CORS preflight).

Frontend (`frontend/`):

- `core/interfaces/UserProfile.interface.ts` (`country`, `UpdateProfileRequest`)
- `core/services/auth.ts` — `updateProfile()` refreshes `currentUser`
- `core/regions/regions.ts`, `core/regions/region.service.ts`
- `core/services/country.service.ts` — codes from `/countries`, names from `Intl.DisplayNames`, cached
- `features/settings/pages/settings/` — "Region" card
- `features/home/components/heading/` — region-gated market column
- i18n: `settings.region.*` in all four dictionaries

## Out of scope / next

- Country-specific data for other countries (needs a data source per country; the
  registry is ready for it).
- Currency per account: separate proposal, `docs/proposals/multi-currency.md`.
- Pre-filling the country from the Google profile or timezone: intentionally not done.
