# Savings Goals

Targets like "Vacation - $2000 by December", optionally linked to an account.

## What it does

- CRUD for savings goals (name, target amount, target date, optional linked account).
- `GET /savings-goal/summary` returns an aggregate view (`SavingsGoalSummaryDTO`) across all of a user's goals.

## How it works

**There is no contribution ledger.** Progress is not stored — it's derived at read time from the linked account's balance. Every `create`/`update`/`getById`/`search` recomputes `currentAmount`, `progressPercentage`, `remainingAmount`, `monthlyRequired` and `daysRemaining` on the fly (`SavingsGoalService.enrich`). This was already the contract documented on the pre-existing `SavingsGoal` model/DTO — the account balance is the single source of truth, so there's no separate "add contribution" endpoint.

The one persisted/cached field is `achieved` (boolean), refreshed on every create/update, used as-is in the summary's `achievedGoals` count.

Validation: target amount must be > 0, target date can't be in the past — otherwise `SavingsGoalRuntimeException` (`409 CONFLICT`).

## Files

`controller/SavingsGoalController`, `service/SavingsGoalService`, `repository/SavingsGoalRepository`, `mapper/SavingsGoalMapper`, `filter/SavingsGoalFilter`, `dto/SavingsGoalDTO`, `dto/SavingsGoalSummaryDTO`, `model/SavingsGoal`, `exception/SavingsGoalRuntimeException`.

## Not done

No frontend UI yet — backend only. No contribution-tracking endpoint (progress is derived from account balance, by design).
