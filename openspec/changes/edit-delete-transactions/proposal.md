# Proposal: Edit & delete transactions

## Intent

App has no legal/audit requirement — personal finance only. Users need to fix logging mistakes (wrong amount, description, category, date, counterparty) or remove a bad entry without recreating an account from scratch. Backend `update()`/`deleteById()` exist as HTTP endpoints already (generic `ICrudController`) but are dead-stubbed (`// shall not be used`); frontend `EditTransaction` + `TransactionService.update()/delete()` are wired but never invoked with a real transaction, and PUT today silently no-ops (200 + null body).

## Scope

### In Scope
- Backend: real `update()` — restricted fields only (amount, description, category, counterparty, date). Reverses old balance effect, reapplies new one. Ownership check via `findByIdAndUser`.
- Backend: real `deleteById()` — reverses balance effect, ownership check, removes row.
- Backend: reject update payloads that change `account`, `destinationAccount`, or `type` (400 Bad Request).
- Frontend: row-level Edit/Delete actions in `transaction-list` (table + mobile cards), wired to existing `EditTransaction` modal and a new delete-confirmation flow.
- Frontend: `EditTransaction` — when editing (not creating), disable account/type controls (read-only), keep the rest editable.
- Frontend: simple confirm dialog for delete (reuse `UiModalComponent`, no new modal primitive).

### Out of Scope
- Changing account or type in-place. If the user logged the wrong type/account, they delete and recreate — no migration/merge logic.
- Bulk edit/delete.
- Undo/soft-delete or audit trail beyond existing `AuditedEntity` timestamps.
- Any change to `RecurringTransaction`.

## Approach

Extract `revertBalanceChanges` (mirror of existing `applyBalanceChanges`) in `TransactionService`. `update`: load owned entity via `findByIdAndUser`, reject if `account`/`destinationAccount`/`type` differ from stored values, revert old balance effect, validate new amount, apply new effect, mutate entity fields, let JPA dirty-check persist. `deleteById`: load owned entity, revert balance effect, `repository.delete(entity)`. Both inside existing `@Transactional`. Frontend disables account/type selects in edit mode (form controls stay in payload but `disabled` in the DOM won't submit changed values — verify via `getRawValue()` usage already in `submit()`, must exclude disabled fields from mutation or backend rejection is the real guard).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `BudgetKing/.../service/TransactionService.java` | Modified | Real `update()`/`deleteById()`, new `revertBalanceChanges`, field-change guard |
| `BudgetKing/.../exception/` | New (maybe) | `ImmutableFieldChangeException` or reuse `IllegalArgumentException` → 400 |
| `frontend/.../transaction-list.html` + `.ts` | Modified | Row actions (edit/delete), delete confirm flow |
| `frontend/.../edit-transaction.ts` + `.html` | Modified | Disable account/type in edit mode |
| Backend tests | New | Update/delete unit (Mockito) + integration (Testcontainers): EXPENSE/INCOME/TRANSFER revert correctness, ownership rejection, field-change rejection |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| TRANSFER revert touches two accounts — partial failure leaves balances inconsistent | Med | Single `@Transactional` method, no early return between revert/apply |
| Backend accepts changed account/type silently if guard is missed | Med | Explicit equality check before any balance mutation, covered by dedicated test |
| Frontend disabled fields still leak into PUT payload | Low | Test `submit()` payload explicitly excludes account/type when editing |

## Rollback Plan

Revert the two service methods to their stub bodies and drop the new frontend row actions (feature-flag-free — plain git revert of the change's commits); no schema/migration involved since no DB structure changes.

## Dependencies

None external. Depends on existing `findByIdAndUser`, `applyBalanceChanges`, `AuditedEntity`.

## Success Criteria

- [ ] User can edit amount/description/category/counterparty/date of an owned transaction; account balance reflects the diff correctly for EXPENSE/INCOME/TRANSFER
- [ ] User can delete an owned transaction; account balance(s) revert correctly
- [ ] Attempting to change account/type via API returns 400
- [ ] Attempting to edit/delete another user's transaction returns 404 (ownership check)
- [ ] `./mvnw clean test` green including new tests
