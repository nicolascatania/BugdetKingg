# Delta for Transactions

Base: `openspec/changes/edit-delete-transactions/specs/transactions/spec.md` (not yet archived into main specs).

## ADDED Requirements

### Requirement: Savings contribution transactions
The system MUST record every goal deposit and withdrawal as a transaction of type `SAVINGS_DEPOSIT` or `SAVINGS_WITHDRAWAL`, linked to the goal and to the account involved. These types MUST NOT require a category or a destination account, MUST NOT count as income or expense in any report or budget, and MUST appear in the account's transaction history.

#### Scenario: Deposit shows in history
- GIVEN the user deposits $50k from account A into goal G
- WHEN the user lists transactions of account A
- THEN a `SAVINGS_DEPOSIT` of $50k referencing G is listed

#### Scenario: Dashboard unaffected
- GIVEN a month with $100k income, $40k expenses and a $50k deposit into a goal
- WHEN the dashboard totals are read
- THEN income is $100k and expense is $40k

### Requirement: Contribution types are not created manually
The generic create endpoint MUST reject `SAVINGS_DEPOSIT` and `SAVINGS_WITHDRAWAL`; they are only produced through the savings-goal endpoints. CSV import MUST reject rows with these types.

#### Scenario: Manual create
- GIVEN a payload with type `SAVINGS_DEPOSIT`
- WHEN it is posted to the generic transaction endpoint
- THEN the system responds 400

### Requirement: Editing or deleting a contribution keeps the goal consistent
Editing the amount of a contribution transaction or deleting it MUST revert and reapply its effect on both the account and the goal. The system MUST reject the change when it would leave `currentAmount` negative or when the goal is `CLOSED`.

#### Scenario: Delete a deposit
- GIVEN a `SAVINGS_DEPOSIT` of $50k into goal G holding $50k
- WHEN the user deletes it
- THEN the account regains $50k and G holds $0

#### Scenario: Delete a deposit after withdrawing
- GIVEN a `SAVINGS_DEPOSIT` of $50k into G, then a withdrawal of $30k (G holds $20k)
- WHEN the user deletes the deposit
- THEN the system responds 409 and nothing changes

## MODIFIED Requirements

### Requirement: Restricted transaction editing
`savingsGoal` joins `account`, `destinationAccount` and `type` as immutable fields on update.
(Previously: only `account`, `destinationAccount`, `type` were immutable.)

#### Scenario: User attempts to relink a contribution
- GIVEN a `SAVINGS_DEPOSIT` linked to goal G1
- WHEN the user submits an update pointing it to goal G2
- THEN the system responds 400
