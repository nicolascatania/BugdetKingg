# CSV Import / Export (Transactions & Categories)

Bulk-import records from a CSV file with a validation preview step, and export records to CSV.
The same two-step pattern (preview → commit, one bad row never aborts the rest) is used for
both transactions and categories.

## Transactions

- `POST /transaction/import/preview` (multipart CSV) — parses and validates every row, never persists anything. Returns `ImportPreviewDTO` with per-row results (`ImportRowDTO`).
- `POST /transaction/import/commit` (multipart CSV) — re-parses/re-validates, then persists only the valid, non-duplicate rows via `TransactionService.create()` (so account balances update the same way manual creation does). Each row resolves its own target account by name — a single file can spread transactions across every account the user has.
- `GET /transaction/export` — downloads the user's transactions as CSV (optionally filtered via the existing `TransactionFilter`), using the same column layout as import — exported files are re-importable.

### Transactions CSV format

Header (case-insensitive, exact column order): `date,description,amount,type,category,counterparty,account`.
Date accepts `yyyy-MM-ddTHH:mm:ss` or bare `yyyy-MM-dd` (normalized to midnight).

### How transaction import/export works

- Uses `commons-csv` (added to `pom.xml`).
- Row-level validation: valid date, positive amount, `INCOME`/`EXPENSE` type only (`TRANSFER` rejected), category must exist for the user, account must exist for the user (matched by exact name — no match means the row is invalid, same treatment as an unmatched category). Duplicate detection via `TransactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType`.
- A malformed physical row (e.g. wrong column count) is flagged invalid but doesn't abort the rest of the batch. Whole-file problems (empty file, bad header, no data rows, unreadable stream) throw `TransactionImportRuntimeException` (`409 CONFLICT`).
- Controller is mounted at `/transaction/...` to match the existing `TransactionController` base path, not `/transactions`.

### Transaction import/export files

`controller/TransactionImportExportController`, `service/TransactionImportService`, `service/TransactionExportService`, `dto/ImportPreviewDTO`, `dto/ImportRowDTO`, `exception/TransactionImportRuntimeException`. Also touches `repository/TransactionRepository` (duplicate-check query) and `repository/AccountRepository` (`findByNameAndUser`).

## Categories

- `POST /category/import/preview` (multipart CSV) — parses and validates every row, never persists anything. Returns `CategoryImportPreviewDTO` with per-row results (`CategoryImportRowDTO`).
- `POST /category/import/commit` (multipart CSV) — re-parses/re-validates, then persists only the valid, non-duplicate rows via `CategoryService.create()`.
- `GET /category/export` — downloads the user's categories as CSV, same column layout as import.

### Categories CSV format

Header (case-insensitive, exact column order): `name,icon`.

### How category import/export works

- `name` is mandatory; a blank name invalidates the row.
- **Golden rule for `icon`**: an unrecognised or blank icon never invalidates the row — it silently falls back to `CategoryIconCatalog.DEFAULT_ICON` (`fa-tags`). The set of valid icons (`CategoryIconCatalog.VALID_ICONS`) mirrors `frontend/src/app/features/icons/interfaces/iconsenum.interace.ts` (`FINANCIAL_ICONS` + `CATEGORY_ICONS`) — there is no shared source of truth between Angular and Spring, so keep both lists in sync by hand when icons change.
- Duplicate detection: a category with the same name already existing for the user marks the row `duplicate` (skipped on commit, not an error).
- Whole-file problems throw `CategoryImportRuntimeException` (`409 CONFLICT`), same as the transaction flow.

### Category import/export files

`controller/CategoryImportExportController`, `service/CategoryImportService`, `service/CategoryExportService`, `service/CategoryIconCatalog`, `dto/CategoryImportPreviewDTO`, `dto/CategoryImportRowDTO`, `exception/CategoryImportRuntimeException`.

## Not done

No frontend UI for category import/export yet (transactions already have `ImportTransactions`
component). No UI anywhere lets the export be filtered by account.
