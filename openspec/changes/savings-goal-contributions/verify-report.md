## Verification Report

**Change**: savings-goal-contributions
**Version**: N/A
**Mode**: Standard

---

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 27 |
| Tasks complete | 25 |
| Tasks incomplete | 2 |

Incomplete: 4.4 (`./mvnw clean test`, not run by the agent — standing user rule: never build), 5.2 (manual UI check — user).

---

### Build & Tests Execution

**Build**: ➖ NOT RUN — the user's standing instruction forbids the agent from building. Static review only.

**Tests**: ➖ NOT RUN. Written: `SavingsGoalServiceTest` (27 tests, rewritten for the new model), `TransactionServiceTest` (+12, existing ones adapted to the new `applyBalanceChanges`/`revertBalanceChanges` signature), `SavingsGoalRepositoryTest` (+3, Testcontainers MySQL), `TransactionImportServiceTest` (+1).

**Coverage**: ➖ Not available

---

### Spec Compliance Matrix

Result column reflects static mapping only; every row becomes COMPLIANT/FAILING once `./mvnw clean test` runs.

| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| Goal holds money | New goal starts empty | `SavingsGoalServiceTest > CreateAndUpdate.shouldCreateGoalSuccessfully` | ⏳ WRITTEN |
| Deposit | Deposit part of the target | `TransactionServiceTest > shouldApplySavingsDeposit`, `SavingsGoalServiceTest > Deposit.shouldDeposit` | ⏳ WRITTEN |
| Deposit | Insufficient funds | `SavingsGoalServiceTest > Deposit.shouldRejectInsufficientFunds` | ⏳ WRITTEN |
| Deposit | Over-saving | `TransactionServiceTest > shouldFlagAchievedOnOverSaving` | ⏳ WRITTEN |
| Withdraw | Partial withdrawal | `TransactionServiceTest > shouldApplySavingsWithdrawal`, `SavingsGoalServiceTest > Withdraw.shouldWithdraw` | ⏳ WRITTEN |
| Withdraw | Withdraw more than saved | `SavingsGoalServiceTest > Withdraw.shouldRejectOverWithdraw`, `TransactionServiceTest > shouldRejectNegativeGoalBalance` | ⏳ WRITTEN |
| Manual lifecycle | Date passes without reaching target | `SavingsGoalServiceTest > Read.shouldReportOverdue`, `CreateAndUpdate.shouldAllowRenamingOverdueGoal` | ⏳ WRITTEN |
| Close | Close an achieved goal | `SavingsGoalServiceTest > Close.shouldCloseWithFunds` | ⏳ WRITTEN |
| Close | Contribute to a closed goal | `SavingsGoalServiceTest > Deposit.shouldRejectDepositIntoClosedGoal`, `Withdraw.shouldRejectWithdrawFromClosedGoal`, `Close.shouldRejectReclose`, `CreateAndUpdate.shouldRejectUpdatingClosedGoal` | ⏳ WRITTEN |
| Delete only empty goals | Delete with funds | `SavingsGoalServiceTest > Delete.shouldRejectDeleteWithFunds`, `Delete.shouldDeleteEmptyGoal`, `SavingsGoalRepositoryTest > shouldUnlinkSavingsGoalFromTransactions` | ⏳ WRITTEN |
| Summary | Two goals | `SavingsGoalServiceTest > Summary.shouldGetSummarySuccessfully` | ⏳ WRITTEN |
| Ownership | Deposit into another user's goal | `SavingsGoalServiceTest > Deposit.shouldRejectForeignGoal` | ⏳ WRITTEN |
| Contribution transactions | Deposit shows in history | `SavingsGoalServiceTest > Deposit.shouldDeposit` (captures the saved `Transaction`) | ⏳ WRITTEN |
| Contribution transactions | Dashboard unaffected | (none) — report queries already filter `type = INCOME/EXPENSE` (`TransactionRepository`), verified by reading | ⚠️ PARTIAL |
| Not created manually | Manual create | `TransactionServiceTest > shouldRejectGenericCreateOfSavingsType`, `TransactionImportServiceTest > shouldFlagSavingsType` | ⏳ WRITTEN |
| Edit/delete keeps goal consistent | Delete a deposit | `TransactionServiceTest > shouldDeleteSavingsDeposit`, `shouldRevertSavingsDeposit`, `shouldRevertSavingsWithdrawal`, `shouldUpdateSavingsDepositAmount` | ⏳ WRITTEN |
| Edit/delete keeps goal consistent | Delete a deposit after withdrawing | `TransactionServiceTest > shouldRejectDeletingDepositAfterWithdrawal` | ⏳ WRITTEN |
| Restricted editing (modified) | Relink a contribution | `TransactionServiceTest > shouldRejectSavingsGoalChangeOnUpdate` | ⏳ WRITTEN |

**Compliance summary**: 17/18 scenarios have tests written; 0/18 executed.

---

### Correctness (Static — Structural Evidence)
| Requirement | Status | Notes |
|------------|--------|-------|
| Goal holds money | ✅ Implemented | `SavingsGoal.currentAmount` + `status`; `enrich` derives from it |
| Deposit / Withdraw | ✅ Implemented | `SavingsGoalService.deposit/withdraw` → `TransactionService.applyBalanceChanges` |
| Manual lifecycle | ✅ Implemented | `deriveState`, no scheduler; date validated only when changed |
| Close | ✅ Implemented | full withdrawal + `CLOSED`; empty goal closes without account |
| Delete only empty | ✅ Implemented | 409 guard + `unlinkSavingsGoal` before delete |
| Summary excludes closed | ✅ Implemented | `getSummary` filters `isActive()` |
| Ownership | ✅ Implemented | all lookups via `findByIdAndUser` / `AccountService.getEntityById` |
| Contribution transactions | ✅ Implemented | new enum values, FK, mapper, `LastMovesDTO` label |
| Not created manually | ✅ Implemented | `assertNotSavingsType` on create (400), import error row |
| Edit/delete consistency | ✅ Implemented | `moveGoalBalance` guards inside apply/revert; goal moved before account so a refusal is side-effect free |

---

### Coherence (Design)
| Decision | Followed? | Notes |
|----------|-----------|-------|
| New transaction types + FK | ✅ Yes | |
| Balance maths in one place | ✅ Yes | 5-way switch with `SavingsGoal` parameter |
| Reject negative / closed in revert-apply | ✅ Yes | `SavingsGoalRuntimeException` → 409 |
| `linkedAccount` repurposed | ✅ Yes | copy updated in modal, tutorial, docs |
| Manual lifecycle | ✅ Yes | |
| `status` persisted, `state` derived | ✅ Yes | |
| `@ColumnDefault` for old rows | ✅ Yes | `"0"` / `"'ACTIVE'"` |
| Unlink on delete | ✅ Yes | `@Modifying` JPQL update |
| Date validated only when changed | ✅ Yes | |
| Savings-type rejection location | ⚠️ Deviated | Design put it in `validateTransaction`; moved to a separate `assertNotSavingsType` called only from `create()`, otherwise editing a contribution's amount via the generic endpoint would be blocked, contradicting the spec |
| `TransactionRelatedEntities.Category` `@NotNull` | ⚠️ Deviated | Removed: it was never validated and is wrong for TRANSFER/savings; a 4-arg convenience constructor keeps existing call sites |
| Repository tests on H2 | ⚠️ Deviated | Design said H2; the existing `BaseRepositoryTest` uses Testcontainers MySQL, followed the codebase |

---

### Issues Found

**CRITICAL** (must fix before archive):
- Tests and build not executed. Run `./mvnw clean test` (backend) and `npm run build` (frontend) before merging.

**WARNING** (should fix):
- "Dashboard unaffected" scenario has no automated test; relies on existing JPQL type filters.
- `SavingsGoalServiceTest` introduces `@Nested` classes (first use in the codebase). Requires Mockito ≥ 3.x nested-instance support, which the Spring Boot 4 BOM provides; if `@InjectMocks` turns out null inside nested classes, flatten them.
- Pre-existing non-English comment found while working: `interfaces/ICrudController.java` — `// El servicio maneja los DTO directamente`. Should be translated (project rule).

**SUGGESTION** (nice to have):
- A status filter toggle on the goals page (backend `SavingsGoalFilter.status` exists; the UI shows all goals with closed ones desaturated).
- Controller-level test for the three new endpoints once an HTTP test harness exists (none in the codebase today).

---

### Verdict
PASS WITH WARNINGS (pending execution)

Implementation matches specs and design statically; behavioral verification is blocked on the user running the test suite.
