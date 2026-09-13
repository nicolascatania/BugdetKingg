# Proposal: Savings goal contributions (set money aside)

## Intent

Today a savings goal stores no money. `currentAmount` is the *whole* balance of `linkedAccount`, so a goal "borrows" an account instead of setting money aside — nothing leaves the user's day-to-day flow, and two goals linked to the same account both claim 100% of it. Users need to move a concrete amount out of an account into a goal, see it excluded from the regular balance, and get it back (to any account) when the goal ends.

## Scope

### In Scope
- Backend: persisted `SavingsGoal.currentAmount` + `status` (`ACTIVE` / `CLOSED`); `achieved` and `overdue` derived.
- Backend: two new `TransactionType` values, `SAVINGS_DEPOSIT` (account → goal) and `SAVINGS_WITHDRAWAL` (goal → account), with nullable `Transaction.savingsGoal` FK. Balance apply/revert extended for both, so generic edit/delete of those transactions stays consistent.
- Backend: endpoints on `SavingsGoalController`: `POST /{id}/deposit`, `POST /{id}/withdraw`, `POST /{id}/close` (withdraw everything to a chosen account, then mark `CLOSED`).
- Backend: `linkedAccount` repurposed as *default source account* (preselected in the contribution form); it no longer drives progress.
- Backend: delete goal only when `currentAmount == 0` (409 otherwise); its transactions are unlinked (FK set null), not deleted.
- Frontend: Deposit / Withdraw / Close actions + modal on goal cards; status badge (Active / Achieved / Overdue / Closed); progress from `currentAmount`.
- Frontend: "Savings" figure next to "Total balance" on home heading (from `/savings-goal/summary`).
- Frontend: transaction list renders the two new types (badge + filter); manual create form does NOT offer them.
- Backend tests (unit + repository) for every rule above.

### Out of Scope
- Automatic action at `targetDate` (no scheduler; money never moves without the user).
- CSV import of the new types (import rejects them); export just prints the type.
- Recurring auto-deposits, budgets interaction, goal-to-goal transfers.

## Approach

Goal becomes its own money aggregate. Every contribution is a `Transaction` (history stays in the account and in the transaction list) whose balance effect is `account.balance ∓ amount` and `goal.currentAmount ± amount`, applied inside one `@Transactional`. Income/expense reports already filter by explicit `type`, so the new types do not leak into them. Lifecycle is manual: past `targetDate` the goal reads as Overdue or Achieved; the user extends the date, keeps contributing, or closes it (funds returned to a chosen account).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `BudgetKing/.../enumerator/TransactionType.java` | Modified | `SAVINGS_DEPOSIT`, `SAVINGS_WITHDRAWAL` |
| `BudgetKing/.../enumerator/SavingsGoalStatus.java` | New | `ACTIVE`, `CLOSED` |
| `BudgetKing/.../model/SavingsGoal.java`, `model/Transaction.java`, `model/dict/*_` | Modified | `currentAmount`, `status`; `Transaction.savingsGoal` |
| `BudgetKing/.../dto/SavingsGoalDTO.java`, `SavingsGoalContributionDTO.java`, `TransactionDTO.java` | Modified/New | status/overdue components; contribution payload; `savingsGoalId/Name` |
| `BudgetKing/.../service/SavingsGoalService.java` | Modified | deposit/withdraw/close, delete guard, derived fields |
| `BudgetKing/.../service/TransactionService.java` | Modified | apply/revert for new types, validation, DTO `@AssertTrue` |
| `BudgetKing/.../service/TransactionImportService.java` | Modified | reject new types |
| `BudgetKing/.../controller/SavingsGoalController.java` | Modified | 3 new endpoints |
| `frontend/.../features/savings-goals/**` | Modified/New | contribution modal, actions, badges |
| `frontend/.../features/home/components/heading/**` | Modified | Savings figure |
| `frontend/.../features/transactions/**`, `shared/models/TransactionType.enum.ts` | Modified | new type rendering/filter |
| `BudgetKing/src/test/.../service/SavingsGoalServiceTest.java`, `TransactionServiceTest.java`, `repository/SavingsGoalRepositoryTest.java` | Modified | new rules |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Existing goals lose their "progress" (was account balance) | High (by design) | `currentAmount` defaults to 0; docs + tutorial explain the new model |
| Generic transaction edit/delete on a contribution leaves `currentAmount` negative | Med | Validate in revert/apply; reject with 409 when the goal cannot absorb the change |
| New enum values break `switch` statements / frontend maps | Med | Exhaustive switches (compile error), grep every `TransactionType` usage, tests per type |
| `ddl-auto=update` adds columns but no defaults for old rows | Low | `currentAmount` mapped `nullable=false` with Java default `ZERO`; status default `ACTIVE` — verify on a copy of `backup_presupuesto.sql` |

## Rollback Plan

Plain git revert of the change's commits. Schema: new nullable FK column and two goal columns stay in place harmlessly (Hibernate never drops); rows with the new enum values would have to be deleted manually before rollback (`DELETE FROM transactions WHERE type IN ('SAVINGS_DEPOSIT','SAVINGS_WITHDRAWAL')`).

## Dependencies

None external. Builds on `applyBalanceChanges` / `revertBalanceChanges`, `findByIdAndUser`, `GlobalExceptionHandler` mappings.

## Success Criteria

- [ ] Deposit 50k from account A to goal G: A drops 50k, G shows 50k / target, home "Total balance" drops 50k, "Savings" shows 50k
- [ ] Withdraw / close returns money to the chosen account and closed goals are read-only
- [ ] Deposit above account balance, withdraw above `currentAmount`, any contribution on a closed goal, delete with funds → 409
- [ ] Income/expense dashboard and budgets unchanged by contributions
- [ ] Editing/deleting a contribution transaction keeps account and goal consistent
- [ ] `./mvnw clean test` green with new tests
