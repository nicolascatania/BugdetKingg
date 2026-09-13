# Tasks: Savings goal contributions

Paths relative to `BudgetKing/src/main/java/com/veritech/BudgetKing/` (backend) and `frontend/src/app/` (frontend).

## Phase 1: Backend foundation

- [x] 1.1 `enumerator/TransactionType.java`: add `SAVINGS_DEPOSIT`, `SAVINGS_WITHDRAWAL` and `boolean isSavings()`.
- [x] 1.2 Create `enumerator/SavingsGoalStatus.java` (`ACTIVE`, `CLOSED`).
- [x] 1.3 `model/SavingsGoal.java`: add `currentAmount` (`precision 19, scale 2`, `@ColumnDefault("0")`, builder default `ZERO`) and `status` (`@Enumerated(STRING)`, `@ColumnDefault("'ACTIVE'")`, builder default `ACTIVE`). Update `model/dict/SavingsGoal_.java`.
- [x] 1.4 `model/Transaction.java`: add `@ManyToOne(EAGER) @JoinColumn(name = "savings_goal_id") SavingsGoal savingsGoal`. Update `model/dict/Transaction_.java`.
- [x] 1.5 `dto/TransactionDTO.java`: append `UUID savingsGoal`, `String savingsGoalName`; `isCategoryRequired()` returns true for TRANSFER and savings types. `dto/TransactionRelatedEntities.java`: add `SavingsGoal savingsGoal`. Fix every `new TransactionDTO(...)` / `new TransactionRelatedEntities(...)` call site (main + tests).
- [x] 1.6 `dto/SavingsGoalDTO.java`: add `SavingsGoalStatus status` (persisted) and `String state` (derived). Create `dto/SavingsGoalContributionDTO.java` and `dto/SavingsGoalCloseDTO.java` per design.
- [x] 1.7 `mapper/TransactionMapper.java`, `mapper/SavingsGoalMapper.java`, `dto/LastMovesDTO` mapping: map new fields (`toLastMovesDTO` uses goal name when category is null and goal present).
- [x] 1.8 `repository/TransactionRepository.java`: `@Modifying @Query("update Transaction t set t.savingsGoal = null where t.savingsGoal = :goal") int unlinkSavingsGoal(SavingsGoal goal)`.

## Phase 2: Backend core

- [x] 2.1 `service/TransactionService.java`: change `applyBalanceChanges`/`revertBalanceChanges` signature to `(TransactionType, BigDecimal, Account source, Account destination, SavingsGoal goal)`; add the two savings cases; throw `SavingsGoalRuntimeException` when goal is `CLOSED` or resulting `currentAmount < 0`; refresh `goal.achieved`. Update internal callers.
- [x] 2.2 `service/TransactionService.java` `validateTransaction`: reject savings types on generic create with `IllegalArgumentException` (400). `assertImmutableFieldsUnchanged`: include `savingsGoal`. `update`/`deleteById`: pass `existing.getSavingsGoal()`.
- [x] 2.3 `service/TransactionImportService.java`: add error `"Savings transactions cannot be imported"` when parsed type `isSavings()`.
- [x] 2.4 `service/SavingsGoalService.java`: inject `TransactionService`, `TransactionRepository`. Add `deposit(UUID, SavingsGoalContributionDTO)`, `withdraw(UUID, SavingsGoalContributionDTO)`, `close(UUID, SavingsGoalCloseDTO)` following design data flow (guards → apply → save `Transaction` with description `note ?: "Savings · <name>"`, counterparty = goal name → enrich).
- [x] 2.5 `service/SavingsGoalService.java`: `enrich` uses `currentAmount`; add `state` derivation (`CLOSED` > `ACHIEVED` > `OVERDUE` > `ACTIVE`); `update` rejects `CLOSED` (409) and validates date only when changed; `deleteById` rejects `currentAmount > 0` (409) and calls `unlinkSavingsGoal` first; `getSummary` skips `CLOSED`.
- [x] 2.6 `filter/SavingsGoalFilter.java`: optional `status` criterion.
- [x] 2.7 `controller/SavingsGoalController.java`: `POST /{id}/deposit`, `POST /{id}/withdraw`, `POST /{id}/close` with `@Valid` bodies, returning `SavingsGoalDTO`.

## Phase 3: Frontend

- [x] 3.1 `shared/models/TransactionType.enum.ts`: add both values. `features/transactions/interfaces` + `savings-goals/interfaces`: `savingsGoal`, `savingsGoalName`, `status`, `state`, `SavingsGoalContributionDTO`, `SavingsGoalCloseDTO`.
- [x] 3.2 `features/savings-goals/service/savings-goal-service.ts`: `deposit(id, dto)`, `withdraw(id, dto)`, `close(id, dto)` wrapped with refresh.
- [x] 3.3 Create `features/savings-goals/components/contribute-savings-goal/` (ts/html/css): `mode` input `deposit|withdraw|close`; fields account (default `linkedAccountId`), amount (hidden for close), date, note; spinner on submit; emits `submitEvent(saved)`. Follow `budgetking-ui` skill.
- [x] 3.4 `savings-goal-list.ts/html`: Deposit / Withdraw / Close buttons (hidden when `state === 'CLOSED'`), state badge (`chip-positive` achieved, `chip-negative` overdue, `chip-neutral` closed), muted closed card, Edit disabled when closed; update tutorial sections to the new model.
- [x] 3.5 `features/home/components/heading/*`: add "Savings" figure under Total balance using `SavingsGoalService.getSummary()` via `toSignal`.
- [x] 3.6 `transaction-list.html`, `last-moves.html`: chip + `-` sign for `SAVINGS_DEPOSIT`, `+` style for `SAVINGS_WITHDRAWAL`. `edit-transaction.ts`: category optional when type is a savings type (edit mode only).

## Phase 4: Backend tests

- [x] 4.1 `test/.../service/SavingsGoalServiceTest.java`: deposit happy path; insufficient funds 409; over-saving achieved; withdraw happy/over-withdraw; close returns funds and marks CLOSED; contribute/update on CLOSED 409; delete with funds 409; delete empty unlinks; summary excludes closed; OVERDUE state; overdue rename allowed.
- [x] 4.2 `test/.../service/TransactionServiceTest.java`: apply/revert for both savings types; generic create rejects savings type (400); delete deposit after withdrawal 409; update changing `savingsGoal` 400; existing tests updated for new signatures.
- [x] 4.3 `test/.../repository/SavingsGoalRepositoryTest.java`: `unlinkSavingsGoal` nulls FK; status filter spec.
- [ ] 4.4 `./mvnw clean test` — NOT RUN by the agent (standing rule: never build). User to run.

## Phase 5: Docs & manual check

- [x] 5.1 `docs/features/savings-goals.md`: rewrite "How it works" for the contribution model and lifecycle.
- [ ] 5.2 Manual: deposit 50k, check account, goal, home Total balance + Savings; withdraw; close; delete guard; dashboard totals unchanged.
