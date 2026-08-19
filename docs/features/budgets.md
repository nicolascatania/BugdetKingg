# Budgets

Monthly spending limits per category, with progress tracking against actual transactions.

## What it does

- CRUD for budgets: each one is scoped to a `(user, category, year, month)` — a unique constraint blocks creating two budgets for the same category/period.
- `GET /budget/progress?year=&month=` returns spend-vs-limit for every budget in that period.

## How it works

- `BudgetService.assertPeriodIsFree` checks the uniqueness rule before insert and raises `BudgetRuntimeException` (mapped to `409 CONFLICT`) instead of letting a raw DB constraint violation surface.
- Progress reuses the existing `TransactionRepository.getExpensesByCategoryWithIcon` query (same one the dashboard uses), aggregating spend for `[start of month, start of next month)` and matching by category name.
- Status thresholds: `OK` (< 80%), `WARNING` (>= 80%), `EXCEEDED` (>= 100%). A zero-limit budget is guarded against divide-by-zero (forced to 0%, `OK`).

## Files

`controller/BudgetController`, `service/BudgetService`, `repository/BudgetRepository`, `mapper/BudgetMapper`, `filter/BudgetFilter`, `dto/BudgetDTO`, `dto/BudgetProgressDTO`, `model/Budget`, `exception/BudgetRuntimeException`.

## Not done

No frontend UI yet — backend only.
