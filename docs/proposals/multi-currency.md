# Multi-currency support (proposal)

Status: **proposal, not started**. Written 2026-09-13.

Goal: a user in Poland, Spain or the USA should see their own currency, not ARS
everywhere. Currency lives on each **account**; every amount is displayed in the
currency of the account it belongs to.

## Current state (what has to change)

- **Backend has no notion of currency.** `Account`, `Transaction`, `Budget`,
  `SavingsGoal` and `RecurringTransaction` hold bare `BigDecimal` amounts. ARS is
  implicit.
- **Frontend hardcodes `'ARS'` ~35 times across 13 templates** (`| currency: 'ARS'`).
- **`DollarApiClient` is Argentina-only** (`dolarapi.com`, official USD/ARS). Not
  usable as a generic FX source.
- **Aggregations sum across accounts blindly** — these are the places that break
  once accounts hold different currencies:
  - `TransactionService.getDashboard` — `totalBalance` is the sum of every account
    balance.
  - `TransactionRepository.getMonthlyReport`, `getIncomeAndExpense`,
    `getExpensesByCategory*`, `getMonthlyIncomeExpense` — all `SUM(t.amount)` per
    user, ignoring the account.
  - `TransactionService.applyBalanceChanges` / `revertBalanceChanges` — a TRANSFER
    subtracts `amount` from the source and adds **the same** `amount` to the
    destination. Wrong for USD -> PLN.
  - Budgets: `limitAmount` has no currency, `spent` sums expenses from any account.
  - Savings goals: `currentAmount` receives deposits from any account.

## Two concepts, not one

Per-account currency is the right model (Firefly III, YNAB and Actual all do it),
but it needs a second concept to make totals meaningful:

1. **`Account.currency`** — where the money lives. Every transaction inherits the
   currency of its account.
2. **`AppUser.baseCurrency`** — the *reporting* currency. Without it, "total
   balance" and "this month's expenses" are undefined when a user has EUR and PLN
   accounts.

Displaying `zł` instead of `$` is trivial. The real problem is adding apples and
oranges in the dashboard, budgets and goals.

## Proposed schema

```java
// Account
@ColumnDefault("'ARS'")          // backfills existing rows, same pattern as SavingsGoal.currentAmount
@Column(nullable = false, length = 3)
private String currency;         // ISO 4217, validated with java.util.Currency.getInstance()

// AppUser
@ColumnDefault("'ARS'")
@Column(name = "base_currency", nullable = false, length = 3)
private String baseCurrency;

// Transaction and RecurringTransaction
@Column(name = "destination_amount", precision = 19, scale = 2)
private BigDecimal destinationAmount;  // cross-currency TRANSFER only, in the destination currency
```

- `amount` stays in the source account currency.
- Cross-currency transfer: source loses `amount`, destination gains
  `destinationAmount`. `revertBalanceChanges` mirrors it.
- The rate is implicit (`destinationAmount / amount`) and **frozen in the
  transaction**. Never recompute history with today's rate.
- ISO string vs enum: an enum makes "supported currencies" compile-time, but every
  new currency needs a deploy. Prefer a String column + ISO validation + a
  configurable list of currencies the FX provider supports.

## FX rate provider

Verified 2026-09-13:

| Provider | Key | Coverage | Notes |
|---|---|---|---|
| Frankfurter (`api.frankfurter.dev`, ECB) | none | 31 currencies: PLN, EUR, USD... **no ARS** | Ruled out as the only source |
| `open.er-api.com` | none | ARS, PLN, EUR and more; daily updates | ARS = 1509.9 per USD that day |
| `dolarapi.com` (already integrated) | none | ARS/USD only | Official rate; Argentine users may prefer "blue" |

Design:

- `ExchangeRateProvider` interface.
- Generic implementation backed by `open.er-api.com`; the existing `dolarapi.com`
  client stays as an override for ARS pairs.
- Persist rates in an `exchange_rates(base, quote, date, rate)` table, refreshed by
  a daily scheduled job. Gives history for reporting old months and removes the
  per-request dependency on the external API.

## Reporting — the key decision

| Option | What it does | Tradeoff |
|---|---|---|
| **A. Per currency, no conversion** | Dashboard shows "ARS 120,000 · USD 300". Budgets and goals bound to one currency. | No FX dependency, no misleading numbers. Less "wow". |
| **B. Everything converted to `baseCurrency` at today's rate** | One headline number. | Past months change whenever the rate moves. Misleading in high-inflation countries (Argentina). |
| **C. Rate to `baseCurrency` stored on every transaction** | Exact history. | Extra column on every transaction, backfill, most complex. |

Recommendation: **A as the base, plus B only for the `totalBalance` in the heading**,
labelled "converted at today's rate". C only if it is actually requested later.

## Frontend

- Remove every hardcoded `'ARS'`. Every DTO carrying an amount also carries its
  `currency`; templates use `| currency: tx.currency`.
- **Locale**: `1234.56` is `1 234,56 zł` in pl-PL and `$ 1.234,56` in es-AR. Use
  Angular `LOCALE_ID` + `registerLocaleData`, or `Intl.NumberFormat(navigator.language)`.
  Ties in with the i18n item in `IDEAS.txt`.
- Account form: currency select.
- Transfer form: when currencies differ, show a `destinationAmount` field and the
  implied rate.
- Heading `dolarCompra` widget: only when `baseCurrency === 'ARS'`, or replace with a
  generic rate widget.
- CSV import/export: add a `currency` column to `AccountImportService` and
  `TransactionImportService` (transactions could also validate the currency
  against the resolved account).

## Phases (each one shippable)

1. Schema + `Account.currency` + DTOs + per-account display in the frontend.
   Cross-currency transfers **rejected** by validation. Dashboard grouped by currency.
2. Cross-currency transfers with `destinationAmount`.
3. `ExchangeRateProvider` + `AppUser.baseCurrency` + converted headline total.
4. Budgets and savings goals: decide whether they get their own currency or always
   use `baseCurrency`.

## Open questions

1. **Budgets and savings goals**: own currency per budget/goal (more flexible, more
   UI) or always in the user's `baseCurrency` (simpler, but contributing to a EUR
   goal from a PLN account needs conversion)?
2. **Reporting**: confirm A + B for the headline total, or go with C from the start?
3. **Argentina**: which ARS rate for conversions — official (`open.er-api.com`,
   `dolarapi.com/oficial`) or blue? Possibly a per-user setting.
