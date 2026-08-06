---
name: budgetking-ui
description: >
  Design system, reusable components and CSS primitives for the BudgetKing Angular frontend
  (token-based light/dark theming, motion rules, responsive conventions).
  Trigger: Any task that touches `frontend/` UI — building or editing a page, component, template,
  Tailwind class, color, theme, animation, form, table, modal, chart or responsive layout.
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
---

## When to Use

- Creating or reworking any view, page or component under `frontend/src/app/`
- Choosing colors, spacing, typography, radii or shadows
- Adding animations or transitions
- Building forms, tables, modals, lists or charts
- Making something responsive (mobile / tablet / laptop / desktop)
- Reviewing UI for accessibility or visual consistency

## Critical Patterns

### 1. Never hardcode a color. Use tokens.

All colors are CSS custom properties holding `R G B` channels (`frontend/src/styles.css`),
exposed to Tailwind via `rgb(var(--token) / <alpha-value>)`. Flipping the `dark` class on
`<html>` swaps the values — **so components never need `dark:` variants**.

| Token class | Purpose |
|---|---|
| `bg-app` | Page background |
| `bg-surface` | Card / panel background |
| `bg-surface-2` `bg-surface-3` | Inset, hover, elevated fills |
| `border-line` `border-line-strong` | Borders, dividers (`divide-line`) |
| `text-body` `text-muted` `text-subtle` | Primary / secondary / tertiary text |
| `brand` `brand-strong` `brand-soft` `brand-contrast` | Green accent + its readable foreground |
| `accent` | Teal secondary accent |
| `positive` `negative` `warning` | Income / expense / caution semantics |

Bare `border` already resolves to the line token. Never write `bg-slate-900`, `text-indigo-400`
or a raw hex in a template — if a shade is missing, add a token, don't inline it.

### 2. Use the CSS primitives, not ad-hoc class soup

Defined in `@layer components` in `frontend/src/styles.css`:

| Class | Use for |
|---|---|
| `.card` / `.card-interactive` / `.card-sheen` | Surfaces; `-interactive` adds lift + brand border on hover; `-sheen` adds the top highlight |
| `.panel` | Translucent blurred surface |
| `.field-label` `.field-input` `.field-select` `.field-hint` | Every form control |
| `.btn-primary` `.btn-secondary` `.btn-ghost` `.btn-danger` `.btn-icon` | Every button (all inherit `.btn`: 44px min height, cursor, active scale) |
| `.chip-positive` `.chip-negative` `.chip-neutral` `.chip-brand` | Status badges |
| `.page-title` `.section-title` `.eyebrow` | Typographic hierarchy |
| `.skeleton` | Loading placeholders (shimmer built in) |
| `.custom-scroll` | Themed scrollbars on any scrollable container |
| `.tabular` | **Mandatory on every money/metric figure** so digits don't jitter |
| `.hover-lift` `.text-gradient-brand` `.wash` | Motion / decorative utilities |

### 3. Reusable components

| Selector | File | Notes |
|---|---|---|
| `app-theme-toggle` | `shared/components/theme-toggle/` | Light/dark switch, icon-only + `aria-label` |
| `ui-modal` | `shared/modal/ui-modal/` | Owns Escape close, scroll lock, backdrop, animation. Bottom sheet on phones. Project into `<div modal-body>` and `<div modal-footer class="flex w-full justify-end gap-3">` |
| `app-pagination` | `shared/components/PaginationComponent/` | Numeric pager with ellipsis; collapses to `x / y` on phones |
| `app-multiselect` | `shared/components/multiselect/` | Token-styled tag input |
| `app-month-quick-picker` | `shared/components/month-quick-picker/` | Emits a full-month date range |
| `side-bar` | `shared/components/side-bar/` | Inputs `expanded` + `mobileOpen`; nav is a data array, not repeated markup |
| `[appReveal]` | `shared/directives/reveal.directive.ts` | Scroll-in entrance; `[revealDelay]` in ms |

### 4. Theming services and helpers

- `core/services/theme.service.ts` — signal-based, persists to `localStorage` key `budgetking-theme`,
  follows the OS while no explicit choice exists. An inline script in `index.html` applies the theme
  **before first paint** — do not remove it or the app flashes the wrong background.
- `shared/utils/chartTheme.ts` — `chartTheme()`, `categoricalPalette()`, `chartTooltipStyle()`.
  Chart.js paints to canvas and cannot inherit CSS variables, so **any chart must be re-rendered
  inside an `effect` that reads `themeService.theme()`**.

### 5. Motion rules

- Entrances: `appReveal` for sections, `nth-child` delays in the component's own CSS for list
  cascades. **Never bind a per-item index in the template.**
- Durations 150–450ms; ease with `ease-smooth` (`cubic-bezier(0.22,1,0.36,1)`) or `ease-spring`.
- Animate `opacity` and `transform` only — never `width`/`height`/`top`.
- Every custom `@keyframes` block must be paired with a `prefers-reduced-motion: reduce` opt-out.
- Motion carries meaning (arrival, state change, hierarchy) — decorative-only animation is rejected.

### 6. Responsive & accessibility floor

- Mobile-first. Verify at **375 / 768 / 1024 / 1440**.
- Tables render as a real table from `md` up and as cards below it — never a horizontally
  scrolling table on a phone.
- Touch targets ≥ 44×44px (`.btn` enforces it).
- Focus ring is global in `@layer base`; never remove it.
- Icons are Font Awesome (`fa-solid …`) — **never emoji as an icon**.
- Color is never the only signal: pair with an icon, a label or a swatch.
- Every icon-only control needs `aria-label`; toggles need `role="switch"` + `aria-checked`.

### 7. Angular conventions

- Standalone components, `ChangeDetectionStrategy.OnPush`, signals for state.
- Control flow blocks (`@if` / `@for` / `@empty`), always `track` a stable id.
- Use `[ngClass]` for conditional classes — **`[class.x]` breaks on names containing `/` or `:`**
  (e.g. `bg-brand/10`, `md:w-64`).
- Arbitrary Tailwind values must appear as literal text in the file so JIT picks them up.
- All code, comments and UI copy in **English**.

## Code Examples

Conditional classes with tokens:

```html
<span [ngClass]="tx.type === 'INCOME' ? 'chip-positive' : 'chip-negative'">{{ tx.type }}</span>

<p class="tabular text-xl font-bold"
   [ngClass]="account.balance >= 0 ? 'text-positive' : 'text-negative'">
  {{ account.balance | currency: 'ARS' : 'symbol' : '1.2-2' }}
</p>
```

Section entrance:

```html
<section appReveal [revealDelay]="60" class="card p-5">…</section>
```

List cascade — delay lives in the component CSS, not the template:

```css
.account-card { animation: card-enter 460ms cubic-bezier(0.22, 1, 0.36, 1) both; }
.account-card:nth-child(1) { animation-delay: 40ms; }
.account-card:nth-child(2) { animation-delay: 90ms; }

@media (prefers-reduced-motion: reduce) { .account-card { animation: none; } }
```

Theme-aware chart:

```ts
effect(() => {
  this.themeService.theme();          // dependency: re-run on theme change
  this.chart?.destroy();
  this.render();                      // reads chartTheme() at paint time
});
```

## Commands

```bash
cd frontend
npm start                                        # ng serve, http://localhost:4200
npm run build                                    # ng build
npm test                                         # Karma + Jasmine

# Find violations before shipping
rg "bg-slate-|text-indigo-|#[0-9a-fA-F]{6}" frontend/src --glob "*.html"
rg "\[class\.[a-z-]+[/:]" frontend/src --glob "*.html"
```

## Resources

- **Tokens & primitives**: `frontend/src/styles.css`
- **Tailwind mapping**: `frontend/tailwind.config.js`
- **Pre-paint theme script**: `frontend/src/index.html`
- **Project rules**: `CLAUDE.md`
