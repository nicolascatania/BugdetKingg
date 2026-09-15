# CSV Import / Export (Transactions, Categories & Accounts)

Bulk-import records from a CSV file with a validation preview step, and export records to CSV.
The same two-step pattern (preview → commit, one bad row never aborts the rest) is used for
transactions, categories and accounts.

## Transactions

- `POST /transaction/import/preview` (multipart CSV) — parses and validates every row, never persists anything. Returns `ImportPreviewDTO` with per-row results (`ImportRowDTO`).
- `POST /transaction/import/commit` (multipart CSV) — re-parses/re-validates, then persists only the valid, non-duplicate rows via `TransactionService.create()` (so account balances update the same way manual creation does). Each row resolves its own target account by name — a single file can spread transactions across every account the user has.
- `GET /transaction/export` — downloads the user's transactions as CSV (optionally filtered via the existing `TransactionFilter`), using the same column layout as import — exported files are re-importable.

### Transactions CSV format

Header (case-insensitive, exact column order): `date,description,amount,type,category,counterparty,account,destination_account`.
Date accepts `dd/MM/yyyy HH:mm` or bare `dd/MM/yyyy` (normalized to midnight); the ISO forms `yyyy-MM-ddTHH:mm:ss` / `yyyy-MM-dd` are still accepted so older exports stay importable (`DateUtils.parseCsvDate`). Exports write `dd/MM/yyyy HH:mm`, the same format the UI shows.

### How transaction import/export works

- Uses `commons-csv` (added to `pom.xml`).
- Row-level validation: valid date, positive amount, `INCOME`/`EXPENSE`/`TRANSFER` type, account must exist for the user (matched by exact name — no match means the row is invalid). `category` is mandatory for `INCOME`/`EXPENSE` and optional for `TRANSFER`, mirroring `TransactionDTO`'s own validation. `destination_account` only applies to `TRANSFER` rows: mandatory, must exist for the user, and must differ from `account` — for `INCOME`/`EXPENSE` rows the column is ignored even if present. Duplicate detection via `TransactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType`.
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

## Accounts

- `POST /account/import/preview` (multipart CSV) — parses and validates every row, never persists anything. Returns `AccountImportPreviewDTO` with per-row results (`AccountImportRowDTO`).
- `POST /account/import/commit` (multipart CSV) — re-parses/re-validates, then persists only the valid, non-duplicate rows via `AccountService.create()`. Every imported account starts with a balance of zero; the file cannot carry an opening balance.
- `GET /account/export` — downloads the user's accounts as CSV, same column layout as import (balance is never exported either).

### Accounts CSV format

Header (case-insensitive, exact column order): `name,description,icon`.

### How account import/export works

- `name` and `description` are mandatory; either blank invalidates the row.
- **Golden rule for `icon`**: same as categories — an unrecognised or blank icon never invalidates the row, it falls back to `AccountIconCatalog.DEFAULT_ICON` (`fa-building-columns`, the bank icon). The set of valid icons (`AccountIconCatalog.VALID_ICONS`) mirrors the `FINANCIAL_ICONS` array in `frontend/src/app/features/icons/interfaces/iconsenum.interace.ts` (a different, smaller set than the one categories validate against) — keep both lists in sync by hand when icons change.
- Duplicate detection: an account with the same name already existing for the user marks the row `duplicate` (skipped on commit, not an error), reusing `AccountRepository.findByNameAndUser` (also used to resolve the `account` column on transaction import).
- Whole-file problems throw `AccountImportRuntimeException` (`409 CONFLICT`), same as the other two flows.

### Account import/export files

`controller/AccountImportExportController`, `service/AccountImportService`, `service/AccountExportService`, `service/AccountIconCatalog`, `dto/AccountImportPreviewDTO`, `dto/AccountImportRowDTO`, `exception/AccountImportRuntimeException`.

## Not done

No frontend UI for category or account import/export yet (transactions already have
`ImportTransactions` component). No UI anywhere lets the export be filtered by account.
