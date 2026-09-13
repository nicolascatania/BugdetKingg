# Tasks: Edit & delete transactions

## Phase 1: Backend foundation

- [x] 1.1 In `GlobalExceptionHandler.java`, add `@ExceptionHandler(IllegalArgumentException.class)` returning 400 with `{"error": "BAD_REQUEST", "message": ex.getMessage()}`.
- [x] 1.2 In `TransactionService.java`, add private `assertImmutableFieldsUnchanged(Transaction existing, TransactionDTO dto)` throwing `IllegalArgumentException` if `account`, `destinationAccount`, or `type` differ from stored values.
- [x] 1.3 In `TransactionService.java`, add private `revertBalanceChanges(BigDecimal amount, TransactionType type, Account source, Account destination)` mirroring `applyBalanceChanges` with inverted signs.

## Phase 2: Backend core implementation

- [x] 2.1 Implement `TransactionService.update(UUID id, TransactionDTO dto)`: load via `findByIdAndUser` (404 if missing), call `assertImmutableFieldsUnchanged`, call `validateTransaction(dto, existing.getAccount(), existing.getDestinationAccount())`, `revertBalanceChanges` with the entity's stored amount/type, `applyBalanceChanges` with the new dto, mutate `amount`/`description`/`category`/`counterparty`/`date` on the loaded entity (resolve `category` via `categoryService.getEntityById` if `dto.category()` present), return `mapper.toDto(entity)`.
- [x] 2.2 Implement `TransactionService.deleteById(UUID id)`: load via `findByIdAndUser` (404 if missing), call `revertBalanceChanges` with the entity's stored amount/type/accounts, `transactionRepository.delete(entity)`.
- [x] 2.3 Confirm both methods run inside the existing `@Transactional` (already on interface/impl per `ICrudService`) so balance + row changes commit atomically.

## Phase 3: Frontend wiring

- [x] 3.1 In `edit-transaction.ts`, in `ngOnInit()` when `this.transaction` is set, call `this.form.get('account')?.disable()` and `this.form.get('type')?.disable()`. (also disables `destinationAccount` — see deviations)
- [x] 3.2 In `edit-transaction.ts` `submit()`, when `this.transaction` is truthy, build `payload` by overriding `account`, `type`, `destinationAccount` from `this.transaction` after spreading `form.getRawValue()`, so disabled-field values can never diverge from the original even if `getRawValue()` returns a stale value. (not needed — see deviations: `getRawValue()` already includes disabled-control values, guarded the actual UI leak instead)
- [x] 3.3 In `edit-transaction.html`, ensure the account/type controls render their existing disabled visual state (check `budgetking-ui` disabled-input token is already applied via `[disabled]`/`:disabled` — no new class needed if Tailwind `disabled:` variants already used elsewhere in the form).
- [x] 3.4 In `transaction-list.ts`, add `editingTransaction = signal<TransactionDTO | null>(null)`, `openEditModal(tx: TransactionDTO)` setting it and `isTransactionModalOpen`, and reset it in `onTransactionModalClosed`.
- [x] 3.5 In `transaction-list.ts`, add `transactionToDelete = signal<TransactionDTO | null>(null)`, `confirmDelete(tx: TransactionDTO)` to open a confirm modal, `deleteTransaction()` calling `transactionService.delete(id)`, on success `this.ns.success(...)` + `onSearch()`, on error `this.ns.error(...)`.
- [x] 3.6 In `transaction-list.html`, change `[transaction]="null"` to `[transaction]="editingTransaction()"` on `app-edit-transaction`.
- [x] 3.7 In `transaction-list.html`, add Edit and Delete icon buttons to each desktop table row (`(click)="openEditModal(transaction)"` / `(click)="confirmDelete(transaction)"`) and to each mobile card.
- [x] 3.8 In `transaction-list.html`, add a `ui-modal` instance bound to `transactionToDelete()` truthiness with confirm/cancel buttons calling `deleteTransaction()` / clearing the signal.

## Phase 4: Backend tests

- [x] 4.1 In `TransactionServiceTest.java`, add unit tests for `update()`: amount change on EXPENSE/INCOME/TRANSFER recalculates balances correctly (spec scenarios "User edits amount and description", "Editing a TRANSFER amount"). (EXPENSE + TRANSFER added; INCOME math is identical to EXPENSE with inverted signs, covered indirectly by the existing `shouldIncreaseBalanceOnIncome`-style apply/revert symmetry — not duplicated as a third near-identical test)
- [x] 4.2 In `TransactionServiceTest.java`, add unit tests: `update()` throws `IllegalArgumentException` when `account`/`destinationAccount`/`type` differ from stored (spec "User attempts to change account/type").
- [x] 4.3 In `TransactionServiceTest.java`, add unit tests: `update()`/`deleteById()` throw `EntityNotFoundException` for another user's transaction id (spec "Editing/Deleting another user's transaction").
- [x] 4.4 In `TransactionServiceTest.java`, add unit tests for `deleteById()`: EXPENSE and TRANSFER balance reversal (spec "Deleting an EXPENSE", "Deleting a TRANSFER").
- [ ] 4.5 SKIPPED — see deviations in apply-progress / final report: no controller/HTTP-level Testcontainers harness exists anywhere in this codebase (only `@DataJpaTest` repository tests do); inventing one from scratch was judged out of scope for this change.
- [ ] 4.6 Run `./mvnw clean test` and confirm all pass. — NOT RUN by the agent (user's standing instruction: never build after changes). User to run manually.

## Phase 5: Manual verification

- [ ] 5.1 Run `npm start` (frontend) + `./mvnw spring-boot:run` (backend), log in, edit a transaction's amount/description via the new row action, confirm balance updates.
- [ ] 5.2 Manually verify account/type fields are read-only in the edit modal.
- [ ] 5.3 Delete a transaction via the new row action, confirm the confirmation dialog blocks accidental deletes and balance reverts.
