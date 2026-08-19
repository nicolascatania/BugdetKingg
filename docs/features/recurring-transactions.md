# Recurring Transactions

Templates that describe a transaction repeating on a schedule (rent, subscriptions, salary), plus the engine that turns due occurrences into real `Transaction` records.

## What it does

- CRUD for recurring transaction templates (amount, description, category, account, `RecurrenceFrequency`, start/optional end date).
- `POST /recurring-transaction/run-due` — fires every due template across all users, creating real transactions and advancing each template's next-occurrence date. Supports catch-up: if multiple periods elapsed since the last run, it fires them all.
- `POST /recurring-transaction/{id}/run-now` — fires one template immediately regardless of schedule.
- `GET /recurring-transaction/upcoming` — read-only 30-day projection, does not mutate anything.

## How it works

- Persisting real transactions goes through the existing `TransactionService`, so account balances update the same way a manually-created transaction would.
- `run-due` isolates failures per template — one broken template doesn't block the rest of the batch.

## Important gap

**No in-process scheduler.** The codebase has no `@Scheduled`/`@EnableScheduling` anywhere, so this feature doesn't auto-fire. `run-due` is meant to be triggered by an external cron hitting the API. If automatic firing is wanted, someone needs to either add Spring's `@Scheduled` or wire an external cron to call `POST /recurring-transaction/run-due`.

## Files

`controller/RecurringTransactionController`, `service/RecurringTransactionService`, `repository/RecurringTransactionRepository`, `mapper/RecurringTransactionMapper`, `filter/RecurringTransactionFilter`, `dto/RecurringTransactionDTO`, `model/RecurringTransaction`, `enumerator/RecurrenceFrequency`, `exception/RecurringTransactionRuntimeException`.

## Not done

No frontend UI yet — backend only.
