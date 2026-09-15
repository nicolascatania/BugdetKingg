# Dashboard range figures & month comparison

Two small readings that answer "how am I doing?" without building a filter by hand.

## Dashboard follows its range

- `POST /transaction/dashboard` now also returns `netBalance` (`income - expense` for the range). The dashboard shows it as a fourth key figure, "Range balance", next to the account total (`totalBalance`, which is a snapshot of every account right now and is *not* range-bound — the two cards answer different questions on purpose).
- `GET /transaction/movements?dateFrom=&dateTo=` (ISO dates, both inclusive) lists the movements of a range, most recent first, as `LastMovesDTO`. The dashboard's movements list uses it with the **applied** range (the one the figures were computed for, held in `Dashboard.appliedRange`), so the list and the cards never disagree while a new range is being typed. Previously the list always showed the current month regardless of the filter, which read as a bug.
- `last-moves` (`features/home/components/last-moves`) takes optional `dateFrom` / `dateTo` inputs; without them it keeps showing the current month, which is what the home page wants.

## Month comparison on the home page

- `GET /transaction/month-comparison` returns `MonthComparisonDTO`: income and expense of the current calendar month and of the whole previous month (two `getMonthlyReport` queries). The current month is partial by nature; the comparison is a running indicator, not a verdict.
- `monthly-summary` reads it and adds a line under each tile: income and expense as `±N% vs last month` (falls back to an absolute amount when last month was zero, since there is no ratio to show), and the balance tile as "$X more / less than last month" — the "you saved more than last month" reading from IDEAS.txt item 6.
- Colour follows meaning, not sign: more income is green, more spending is red, a bigger net is green. Every line pairs the colour with an arrow / icon.

## Files

Backend: `controller/TransactionController` (`movementsBetween`, `monthComparison`), `service/TransactionService` (`movementsBetween`, `getMonthComparison`, `netBalance` in `getDataForDashBoard`), `dto/DashBoardDTO`, `dto/MonthComparisonDTO`.

Frontend: `features/dashboard/pages/dashboard/`, `features/home/components/last-moves/`, `features/home/components/monthly-summary/`, `features/transactions/services/transaction-service.ts`, `features/transactions/interfaces/MonthComparisonDTO.interface.ts`, `features/dashboard/interfaces/DashBoardDTO.interface.ts`.
