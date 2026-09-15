# Budgets

Monthly spending limits per category, with progress tracking against actual transactions.

## What it does

- CRUD for budgets: each one is scoped to a `(user, category, year, month)` — a unique constraint blocks creating two budgets for the same category/period.
- A budget can be **recurring** (`Budget.recurring`, `BudgetDTO.recurring`): its period becomes a start, and the limit carries over to every later month that has no budget of its own for that category. That is how a fixed monthly limit is set once instead of once per month.
- `GET /budget/progress?year=&month=` returns spend-vs-limit for every category budgeted in that period, resolving recurring budgets as described below. Each reading carries the budget's own `year`/`month` and `recurring` flag so the UI can tell a carried-over limit apart and edit the right row.

## How it works

- `BudgetService.assertPeriodIsFree` checks the uniqueness rule before insert and raises `BudgetRuntimeException` (mapped to `409 CONFLICT`) instead of letting a raw DB constraint violation surface.
- Resolution per category for a period (`BudgetService.resolveBudgetsFor`): the budget created for that exact period wins, whether recurring or not; otherwise the recurring budget with the latest start on or before the period (`BudgetRepository.findRecurringStartingOnOrBefore`, ordered newest start first). Consequences: a one-off budget overrides a recurring one for its month only; a newer recurring budget replaces an older one from its start month onwards; editing or deleting a recurring budget affects every month it covers. There is no "end date" — to stop a recurring limit, delete it or start a new budget for the category.
- Progress reuses the existing `TransactionRepository.getExpensesByCategoryWithIcon` query (same one the dashboard uses), aggregating spend for `[start of month, start of next month)` and matching by category name.
- Status thresholds: `OK` (< 80%), `WARNING` (>= 80%), `EXCEEDED` (>= 100%). A zero-limit budget is guarded against divide-by-zero (forced to 0%, `OK`).

## Files

`controller/BudgetController`, `service/BudgetService`, `repository/BudgetRepository`, `mapper/BudgetMapper`, `filter/BudgetFilter`, `dto/BudgetDTO`, `dto/BudgetProgressDTO`, `model/Budget`, `exception/BudgetRuntimeException`.

## UI

`features/budgets/` — the progress card shows a repeat icon on recurring budgets ("Repeats monthly since MM/YYYY" when carried over from an earlier start). The create/edit form has a "Repeat every month" checkbox. Editing a carried-over reading opens the budget's own start period, not the selected one, so the start is never moved by accident.
