# CSV Import / Export (Transactions)

Bulk-import transactions from a CSV file with a validation preview step, and export transactions to CSV.

## What it does

- `POST /transaction/import/preview` (multipart CSV) — parses and validates every row, never persists anything. Returns `ImportPreviewDTO` with per-row results (`ImportRowDTO`).
- `POST /transaction/import/commit` (multipart CSV + `accountId`) — re-parses/re-validates, then persists only the valid, non-duplicate rows via `TransactionService.create()` (so account balances update the same way manual creation does).
- `GET /transaction/export` — downloads the user's transactions as CSV (optionally filtered via the existing `TransactionFilter`), using the same column layout as import — exported files are re-importable.

## CSV format

Header (case-insensitive, exact column order): `date,description,amount,type,category,counterparty`.
Date accepts `yyyy-MM-ddTHH:mm:ss` or bare `yyyy-MM-dd` (normalized to midnight).

## How it works

- Uses `commons-csv` (added to `pom.xml` — no CSV library existed in the project before; manual parsing was ruled out because quoting/escaping edge cases are failure-prone).
- Row-level validation: valid date, positive amount, `INCOME`/`EXPENSE` type only (`TRANSFER` rejected), category must exist for the user. Duplicate detection via a new `TransactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType` query.
- A malformed physical row (e.g. wrong column count) is flagged invalid but doesn't abort the rest of the batch. Whole-file problems (empty file, bad header, no data rows, unreadable stream) throw `TransactionImportRuntimeException` (`409 CONFLICT`).
- Controller is mounted at `/transaction/...` to match the existing `TransactionController` base path, not `/transactions`.

## Files

`controller/TransactionImportExportController`, `service/TransactionImportService`, `service/TransactionExportService`, `dto/ImportPreviewDTO`, `dto/ImportRowDTO`, `exception/TransactionImportRuntimeException`. Also touches `repository/TransactionRepository` (new duplicate-check query) and `pom.xml` (commons-csv dependency).

## Not done

No frontend UI yet — backend only.
