# Category spending

Clicking a category on the Categories page opens a read-only modal with what that category costs: the selected month in detail and the all-time total. It answers "how much do I spend on this?" without building a transaction filter by hand.

## Endpoint

- `GET /category/{id}/spending?year=&month=` — `CategorySpendingDTO`: category identity, the period, `monthTotal` / `monthCount`, `allTimeTotal` / `allTimeCount` and `monthTransactions` (the month's expenses as `LastMovesDTO`, most recent first). User-scoped through `CategoryService.getEntityById`; a category the user does not own is a `404`.

## Rules

- Only `EXPENSE` transactions count. Income booked under a category, transfers and savings movements are ignored, so the figures read as "what this category cost me".
- `monthTransactions` is the full list for the month, not a page: a single category rarely has more than a few dozen expenses per month and the modal scrolls.
- Totals come from JPQL constructor expressions into `ExpenseTotalDTO` (`TransactionRepository.sumExpensesByCategoryBetween` / `sumExpensesByCategory`); `COALESCE` keeps an empty month at zero rather than `null`.

## UI

`features/categories/components/category-spending` — opens on the current month; chevrons step through months (the next-month arrow is disabled on the current month since nothing can be spent there yet). Each step reloads the figures. The list row's identity block (icon + name) is the trigger; edit and delete stay as separate buttons.

## Files

Backend: `controller/CategoryController` (`getSpending`), `service/CategoryService` (`getSpending`), `repository/TransactionRepository` (the three category-scoped queries), `dto/CategorySpendingDTO`, `dto/ExpenseTotalDTO`.

Frontend: `features/categories/components/category-spending/`, `features/categories/interfaces/CategorySpendingDTO.interface.ts`, `features/categories/service/category-service.ts` (`getSpending`), `features/categories/pages/category-list/`.
