# Savings Goals

Targets like "Vacation - $2000 by December" that hold real money set aside from the user's accounts.

## What it does

- CRUD for savings goals (name, icon, target amount, target date, optional default source account).
- `POST /savings-goal/{id}/deposit` — moves money from one of the user's accounts into the goal.
- `POST /savings-goal/{id}/withdraw` — moves money from the goal back into an account.
- `POST /savings-goal/{id}/close` — returns everything the goal holds to a chosen account and freezes it.
- `GET /savings-goal/summary` returns an aggregate view (`SavingsGoalSummaryDTO`) across the user's open goals.

## How it works

**A goal is a money aggregate.** `SavingsGoal.currentAmount` is persisted and is the running total of the contributions made against it. Every contribution is a regular `Transaction` with one of two dedicated types:

| Type | Effect |
|------|--------|
| `SAVINGS_DEPOSIT` | `account.balance -= amount`, `goal.currentAmount += amount` |
| `SAVINGS_WITHDRAWAL` | `account.balance += amount`, `goal.currentAmount -= amount` |

Both carry a `savingsGoal` FK, no category and no destination account. They show up in the account's history and in the transaction list, but income/expense reports and budgets filter by explicit type, so they never count as earned or spent money. Because the account balance already dropped, the home "Total balance" excludes what is set aside; the heading shows the set-aside total next to it, from the summary endpoint.

The balance maths for every type live in one place, `TransactionService.applyBalanceChanges` / `revertBalanceChanges` (now taking the goal as a parameter). `SavingsGoalService` validates the goal-level rules and delegates there, which is also why editing or deleting a contribution through the generic transaction endpoints keeps account and goal consistent. A change that would leave the goal negative, or touches a closed goal, is rejected with `SavingsGoalRuntimeException` (`409`). The generic create endpoint refuses the savings types (`400`), and CSV import flags them as errors: they only come from the savings-goal endpoints.

**Derived figures.** `progressPercentage`, `remainingAmount`, `monthlyRequired`, `daysRemaining` and `achieved` (`currentAmount >= targetAmount`, cached on the entity) are computed from `currentAmount` on every read (`SavingsGoalService.enrich`). The optional `linkedAccount` no longer drives progress: it is only the default source account preselected in the contribution form.

**Lifecycle is manual.** The persisted `status` is `ACTIVE` or `CLOSED`; the DTO also exposes a derived `state`: `CLOSED` > `ACHIEVED` > `OVERDUE` (past `targetDate`, target not reached) > `ACTIVE`. Nothing happens automatically when the date passes — there is no scheduler and money never moves without the user. The user extends the date, keeps contributing, or closes the goal. Closing withdraws the whole balance into the chosen account (optional when the goal is empty) and marks it `CLOSED`; closed goals are read-only and excluded from the summary. Deleting is only allowed when `currentAmount == 0`; the goal's past contributions are detached (`TransactionRepository.unlinkSavingsGoal`) and stay in the account history.

Validation: target amount > 0; target date can't be in the past on create, and on update only when it changes (so an overdue goal can still be renamed); deposits can't exceed the account balance; withdrawals can't exceed `currentAmount`; amounts must be positive. All of these raise `SavingsGoalRuntimeException` (`409 CONFLICT`).

Schema: `ddl-auto=update` adds `savings_goals.current_amount` and `savings_goals.status` (with `@ColumnDefault` so pre-existing rows get `0` / `ACTIVE`) and `transactions.savings_goal_id`. Goals that existed before this change restart at zero — their old "progress" was just the linked account's balance.

## Files

Backend: `controller/SavingsGoalController`, `service/SavingsGoalService`, `service/TransactionService` (balance maths), `repository/SavingsGoalRepository`, `repository/TransactionRepository` (`unlinkSavingsGoal`), `mapper/SavingsGoalMapper`, `filter/SavingsGoalFilter`, `dto/SavingsGoalDTO`, `dto/SavingsGoalContributionDTO`, `dto/SavingsGoalCloseDTO`, `dto/SavingsGoalSummaryDTO`, `model/SavingsGoal`, `enumerator/SavingsGoalStatus`, `enumerator/TransactionType`, `exception/SavingsGoalRuntimeException`.

Frontend: `features/savings-goals/` (list page, `edit-savings-goal`, `contribute-savings-goal` modal with deposit / withdraw / close modes), `features/home/components/heading` (savings figure), `shared/utils/transactionType.util.ts` (badge/sign rules for the new types).

## Not done

Recurring automatic deposits, goal-to-goal transfers, CSV import of savings movements, any automatic action at the target date.
