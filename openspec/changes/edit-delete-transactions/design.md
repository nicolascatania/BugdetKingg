# Design: Edit & delete transactions

## Technical Approach

Implement `TransactionService.update()`/`deleteById()` for real, replacing the stubs. Both load the entity via the existing `findByIdAndUser(id, user)` (ownership + 404 via `EntityNotFoundException`, already mapped by `GlobalExceptionHandler`). `update` rejects `account`/`destinationAccount`/`type` changes with `IllegalArgumentException` before touching balances; `IllegalArgumentException` currently has no handler (bubbles to 500 today, including in `create()`) — add one mapped to 400, fixing a latent gap and satisfying the spec's 400 requirement. Balance math reuses `applyBalanceChanges`'s shape via a new mirrored `revertBalanceChanges`. Frontend gets row actions in `transaction-list` and disables account/type in `edit-transaction` when editing.

## Architecture Decisions

### Decision: Revert-then-reapply for balance updates on edit
**Choice**: On `update`, call `revertBalanceChanges(oldAmount, oldType, account, destinationAccount)` then `applyBalanceChanges(newDto, account, destinationAccount)`, same accounts (type/account is immutable per spec, so only amount changes).
**Alternatives considered**: Diff-based single delta (compute `newAmount - oldAmount` and apply once) — fewer operations but duplicates the type-switch logic in a third place instead of reusing `applyBalanceChanges`/`revertBalanceChanges` symmetrically.
**Rationale**: Reuses existing `applyBalanceChanges` unchanged; `revertBalanceChanges` is a straight mirror (opposite signs), easy to test independently, and generalizes to delete without new code paths.

### Decision: Reject immutable-field changes with IllegalArgumentException → 400
**Choice**: Compare `dto.account()`/`dto.destinationAccount()`/`dto.type()` against the loaded entity before any mutation; throw `IllegalArgumentException` on mismatch.
**Alternatives considered**: New dedicated `TransactionRuntimeException` mapped to 409 (matches the pattern other domains use, e.g. `CategoryRuntimeException`).
**Rationale**: 400 is semantically correct (malformed/invalid request field), not a state conflict. `IllegalArgumentException` is already the vocabulary `validateTransaction` uses in this class for equivalent invalid-input cases.

### Decision: Frontend guards via disabled controls, backend is the real gate
**Choice**: In `edit-transaction.ts`, when `this.transaction` is set (edit mode), call `.disable()` on `account` and `type` controls; `submit()` must read undisabled values with `form.value` (not `getRawValue()`) for those two fields specifically, or explicitly overwrite them from `this.transaction` before building the payload.
**Alternatives considered**: Hide the fields entirely — worse UX, user can't see what account/type the transaction is on while editing everything else.
**Rationale**: Backend 400 is the actual security/correctness boundary; frontend disabling is UX only — payload MUST be built by spreading `this.transaction`'s `account`/`type`/`destinationAccount` over `form.getRawValue()` (current code already spreads `...this.transaction` first, then form values override — this needs flipping order or explicit override for these 3 fields to avoid regressing the disabled-field guarantee).

## Data Flow

    transaction-list row action ──edit──→ EditTransaction modal (account/type disabled)
                                              │ submit()
                                              ▼
                                   TransactionService.update() [PUT /transaction/{id}]
                                              │
                                              ▼
                          TransactionController (ICrudController default) ──→ TransactionService.update()
                                              │
                              findByIdAndUser (404 if missing/not owned)
                                              │
                        reject if account/destinationAccount/type changed (400)
                                              │
                    revertBalanceChanges(old) ──→ validateTransaction(new) ──→ applyBalanceChanges(new)
                                              │
                                mutate entity fields, JPA dirty-checks Account + Transaction
                                              │
                                        mapper.toDto(entity)

    transaction-list row action ──delete──→ confirm dialog (ui-modal) ──confirmed──→
                                   TransactionService.delete() [DELETE /transaction/{id}]
                                              │
                        findByIdAndUser (404) → revertBalanceChanges → repository.delete(entity)

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `BudgetKing/.../service/TransactionService.java` | Modify | Real `update()`/`deleteById()`, new `revertBalanceChanges()`, field-immutability guard |
| `BudgetKing/.../exception/GlobalExceptionHandler.java` | Modify | Add `@ExceptionHandler(IllegalArgumentException.class)` → 400 |
| `frontend/.../edit-transaction.ts` | Modify | Disable `account`/`type` controls in edit mode; fix payload assembly order |
| `frontend/.../edit-transaction.html` | Modify | Reflect disabled state visually (existing disabled styling from budgetking-ui) |
| `frontend/.../transaction-list.ts` | Modify | `openEditModal(tx)`, `confirmDelete(tx)`, `deleteTransaction(tx)`, track modal state for delete confirm |
| `frontend/.../transaction-list.html` | Modify | Edit/Delete icon buttons per row (table) + per card (mobile); confirm `ui-modal` instance |
| `BudgetKing/src/test/.../service/TransactionServiceTest.java` | Modify | Unit tests: update/delete for EXPENSE/INCOME/TRANSFER, ownership rejection, field-change rejection |
| `BudgetKing/src/test/.../controller/TransactionControllerIT.java` (or equivalent) | Modify/Create | Integration: PUT/DELETE happy path + 400/404 via Testcontainers |

## Interfaces / Contracts

No DTO shape changes. `TransactionDTO` on update carries the same `account`/`type`/`destinationAccount` as stored (frontend responsibility); backend is the enforced contract:

```java
private void assertImmutableFieldsUnchanged(Transaction existing, TransactionDTO dto) {
    if (!existing.getAccount().getId().equals(dto.account())
            || !Objects.equals(existing.getType().name(), dto.type())
            || !Objects.equals(
                    existing.getDestinationAccount() != null ? existing.getDestinationAccount().getId() : null,
                    dto.destinationAccount())) {
        throw new IllegalArgumentException("account, destinationAccount and type cannot be changed on update");
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `revertBalanceChanges` math per type, `assertImmutableFieldsUnchanged` | Mockito, mirrors existing `applyBalanceChanges`/`validateTransaction` test style |
| Integration | Full PUT/DELETE round trip incl. balance persisted, 404 cross-user, 400 field-change | Testcontainers MySQL, existing pattern in repo |
| Frontend | None required (`CLAUDE.md`: "avoid doing test in frontend") | Manual verification via `npm start` |

## Migration / Rollout

No migration required. No schema change, no feature flag — plain deploy.

## Open Questions

None blocking.
