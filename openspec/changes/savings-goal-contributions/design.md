# Design: Savings goal contributions

## Technical Approach

`SavingsGoal` becomes a money aggregate (`currentAmount`, `status`). Every deposit/withdrawal is a `Transaction` with a new type and a `savingsGoal` FK, so account history, generic edit/delete and CSV export keep working. Balance maths for the new types live in the existing `TransactionService.applyBalanceChanges` / `revertBalanceChanges` (single source of truth); `SavingsGoalService` validates goal rules, builds the entity and delegates the maths there. Dependency direction stays acyclic: `SavingsGoalService → TransactionService`, never the reverse (`TransactionService` reaches the goal through `transaction.getSavingsGoal()`).

## Architecture Decisions

| Decision | Options | Tradeoff | Choice |
|----------|---------|----------|--------|
| Money movement | (a) new `TransactionType`s + FK; (b) hidden savings `Account` per goal; (c) contributions table only | (b) leaks into every account listing; (c) loses account history | **(a)** — user-confirmed |
| Where balance maths live | `SavingsGoalService` own switch vs extend `applyBalanceChanges`/`revertBalanceChanges` | Two switches drift; one switch keeps generic edit/delete consistent for free | **Extend existing pair** with a `SavingsGoal` parameter |
| Goal integrity on generic edit/delete | Allow negative `currentAmount` vs reject | Negative pot is meaningless | **Reject** with `SavingsGoalRuntimeException` (409) inside revert/apply when result < 0 or goal `CLOSED` |
| `linkedAccount` | Remove vs repurpose | Column exists, cheap default for the form | **Repurpose** as default source account; drops out of progress maths |
| Lifecycle | Scheduler auto-close vs manual close | No scheduler infra; money must not move unasked | **Manual**: derived `state`, explicit `POST /close` |
| Status persistence | Enum `status` only vs full state | `ACHIEVED`/`OVERDUE` change with time/money — derive; `CLOSED` is a user act — persist | `status ∈ {ACTIVE, CLOSED}` persisted; `state` derived in DTO; `achieved` flag kept for the existing filter |
| Schema defaults for old rows | rely on MySQL implicit default vs `@ColumnDefault` | `status` NOT NULL without default yields `''` → enum mapping crash | `@ColumnDefault("0")` / `@ColumnDefault("'ACTIVE'")` so `ddl-auto=update` emits DDL defaults |
| Goal delete with history | Cascade delete transactions vs unlink | History must survive in the account | **Unlink**: `@Modifying` bulk `set savingsGoal = null` before delete; only when `currentAmount == 0` |
| Update of overdue goals | Keep "date must be future" always vs only when changed | Renaming an overdue goal must not fail | Validate date **only when it changes** |

## Data Flow

    Goal card [Deposit/Withdraw/Close] → ContributeSavingsGoal modal
        │ POST /savings-goal/{id}/deposit|withdraw|close  {accountId, amount?, date?, note?}
        ▼
    SavingsGoalService.deposit/withdraw/close  (@Transactional)
        ├─ findByIdAndUser(goal) 404 · accountService.getEntityById(account) 404
        ├─ guards: status ACTIVE, amount > 0, funds (409 SavingsGoalRuntimeException)
        ├─ transactionService.applyBalanceChanges(type, amount, account, null, goal)
        │      SAVINGS_DEPOSIT:    account −= amount ; goal.currentAmount += amount
        │      SAVINGS_WITHDRAWAL: account += amount ; goal.currentAmount −= amount
        ├─ refreshAchieved(goal) · close → status=CLOSED
        ├─ transactionRepository.save(Transaction{type, account, savingsGoal, description="Savings · <goal>"})
        └─ return enrich(dto)

    Generic PUT/DELETE /transaction/{id} on a contribution
        → revertBalanceChanges(..., existing.getSavingsGoal()) → applyBalanceChanges(...)
        → guard: goal ACTIVE and currentAmount ≥ 0 after change, else 409

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `enumerator/TransactionType.java` | Modify | `SAVINGS_DEPOSIT`, `SAVINGS_WITHDRAWAL`; `isSavings()` helper |
| `enumerator/SavingsGoalStatus.java` | Create | `ACTIVE`, `CLOSED` |
| `model/SavingsGoal.java` | Modify | `currentAmount` (`@ColumnDefault("0")`), `status` (`@ColumnDefault("'ACTIVE'")`) |
| `model/Transaction.java`, `model/dict/Transaction_.java`, `model/dict/SavingsGoal_.java` | Modify | `savingsGoal` ManyToOne EAGER (matches siblings); dict entries |
| `dto/SavingsGoalDTO.java` | Modify | `status`, derived `state` |
| `dto/SavingsGoalContributionDTO.java`, `dto/SavingsGoalCloseDTO.java` | Create | request bodies |
| `dto/TransactionDTO.java`, `dto/LastMovesDTO.java` | Modify | `savingsGoal`, `savingsGoalName`; `@AssertTrue` treat savings like TRANSFER for category |
| `mapper/TransactionMapper.java`, `mapper/SavingsGoalMapper.java` | Modify | map new fields |
| `dto/TransactionRelatedEntities.java` | Modify | add `savingsGoal` |
| `repository/TransactionRepository.java` | Modify | `unlinkSavingsGoal(goal)` bulk update |
| `service/TransactionService.java` | Modify | apply/revert 5-way switch with goal param; `validateTransaction` rejects savings types on generic create; immutability guard includes `savingsGoal` |
| `service/TransactionImportService.java` | Modify | error row when type is a savings type |
| `service/SavingsGoalService.java` | Modify | `deposit`, `withdraw`, `close`; delete guard + unlink; update guard (CLOSED, date-only-if-changed); summary excludes CLOSED; `enrich` from `currentAmount` |
| `filter/SavingsGoalFilter.java` | Modify | optional `status` |
| `controller/SavingsGoalController.java` | Modify | 3 endpoints |
| `frontend/.../shared/models/TransactionType.enum.ts` | Modify | two values |
| `frontend/.../savings-goals/components/contribute-savings-goal/*` | Create | modal (mode deposit/withdraw/close) |
| `frontend/.../savings-goals/pages/savings-goal-list/*` | Modify | actions, state badge, closed styling, tutorial copy |
| `frontend/.../savings-goals/interfaces/*`, `service/savings-goal-service.ts` | Modify | DTOs + 3 calls |
| `frontend/.../home/components/heading/*` | Modify | "Savings" figure from summary |
| `frontend/.../transactions/pages/transaction-list/*.html`, `home/components/last-moves/*.html` | Modify | chip + sign for new types |
| `frontend/.../transactions/components/edit-transaction/edit-transaction.ts` | Modify | category optional for savings types when editing |
| `docs/features/savings-goals.md` | Modify | new model |

## Interfaces / Contracts

```java
public record SavingsGoalContributionDTO(
        @NotNull UUID accountId,
        @NotNull BigDecimal amount,
        LocalDateTime date,   // defaults to now
        String note) {}       // defaults to "Savings · <goal name>"

public record SavingsGoalCloseDTO(UUID accountId) {} // may be null only when currentAmount == 0

// TransactionService (package-private, reused by SavingsGoalService)
void applyBalanceChanges(TransactionType type, BigDecimal amount, Account source, Account destination, SavingsGoal goal);
void revertBalanceChanges(TransactionType type, BigDecimal amount, Account source, Account destination, SavingsGoal goal);
```

`SavingsGoalDTO` gains `status` (persisted) and `state` (`ACTIVE|ACHIEVED|OVERDUE|CLOSED`, derived).

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (`SavingsGoalServiceTest`) | deposit/withdraw/close happy paths; insufficient funds; over-withdraw; closed goal; delete with funds; delete unlinks; summary excludes closed; state derivation; update of overdue goal name | Mockito, existing fixtures |
| Unit (`TransactionServiceTest`) | apply/revert for both savings types; generic create rejects savings types; delete deposit after withdrawal → 409; immutability of `savingsGoal` | Mockito |
| Repository (`SavingsGoalRepositoryTest`, H2) | `unlinkSavingsGoal` bulk update; status filter | `@DataJpaTest` |
| Manual | UI flows + home Savings figure | `npm start` + `spring-boot:run` |

## Migration / Rollout

No migration tool. `ddl-auto=update` adds `savings_goals.current_amount`, `savings_goals.status`, `transactions.savings_goal_id`; `@ColumnDefault` covers existing rows. Existing goals restart at 0 — documented in the tutorial modal.

## Open Questions

None — functional decisions confirmed with the user.
