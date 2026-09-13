# Savings Goals Specification

## Purpose

A savings goal is a named money target with a date. Money is set aside into the goal from the user's accounts and returned to an account when the goal ends. Money inside a goal is excluded from the regular account balance. This is the first spec for the domain; it replaces the previous behaviour where progress equalled the balance of a linked account.

## Requirements

### Requirement: Goal holds money
The system MUST persist how much money is currently set aside in each goal (`currentAmount`), starting at zero. Progress, remaining amount, monthly required and `achieved` (`currentAmount >= targetAmount`) MUST derive from it. The optional linked account MUST NOT influence progress; it only preselects the source account when contributing.

#### Scenario: New goal starts empty
- GIVEN a user creates a goal of $100k with a linked account holding $300k
- WHEN the goal is read
- THEN `currentAmount` is 0, progress 0%, remaining $100k, not achieved

### Requirement: Deposit into a goal
The system MUST allow the owner to move a positive amount from one of their accounts into an `ACTIVE` goal. The account balance MUST decrease and `currentAmount` MUST increase by the same amount, atomically. The system MUST reject deposits larger than the account balance and MUST allow `currentAmount` to exceed the target.

#### Scenario: Deposit part of the target
- GIVEN account A has $300k and goal G targets $100k
- WHEN the user deposits $50k from A into G
- THEN A has $250k, G shows $50k / 50%, remaining $50k

#### Scenario: Insufficient funds
- GIVEN account A has $30k
- WHEN the user deposits $50k from A
- THEN the system responds 409 and nothing changes

#### Scenario: Over-saving
- GIVEN goal G targets $100k and holds $90k
- WHEN the user deposits $20k
- THEN G holds $110k and is achieved

### Requirement: Withdraw from a goal
The system MUST allow the owner to move a positive amount from an `ACTIVE` goal back into one of their accounts. The system MUST reject withdrawals larger than `currentAmount`.

#### Scenario: Partial withdrawal
- GIVEN goal G holds $50k
- WHEN the user withdraws $20k to account B
- THEN G holds $30k and B increases by $20k

#### Scenario: Withdraw more than saved
- GIVEN goal G holds $10k
- WHEN the user withdraws $20k
- THEN the system responds 409 and nothing changes

### Requirement: Manual lifecycle, no automation
The system MUST NOT move money or change a goal's status on its own when `targetDate` passes. A goal MUST report a derived state: `ACHIEVED` when covered, `OVERDUE` when past date and not covered, `ACTIVE` otherwise, `CLOSED` once closed. Overdue goals MUST keep accepting contributions and date edits.

#### Scenario: Date passes without reaching target
- GIVEN goal G targets $100k by yesterday and holds $60k
- WHEN the goal is read today
- THEN its state is `OVERDUE`, `currentAmount` is still $60k and a deposit is still accepted

### Requirement: Close a goal
The system MUST allow the owner to close an `ACTIVE` goal by choosing a destination account. All of `currentAmount` MUST be returned to that account and the goal marked `CLOSED`, atomically. A goal with zero balance MAY be closed without a destination account. Closed goals MUST reject deposits, withdrawals, updates and re-closing, but MUST remain readable.

#### Scenario: Close an achieved goal
- GIVEN goal G holds $100k
- WHEN the user closes it into account B
- THEN B increases by $100k, G holds $0 and is `CLOSED`

#### Scenario: Contribute to a closed goal
- GIVEN goal G is `CLOSED`
- WHEN the user deposits $10k
- THEN the system responds 409

### Requirement: Delete only empty goals
The system MUST reject deleting a goal whose `currentAmount` is above zero. Deleting an empty goal MUST keep its past contribution transactions in the account history.

#### Scenario: Delete with funds
- GIVEN goal G holds $5k
- WHEN the user deletes it
- THEN the system responds 409

### Requirement: Summary reflects set-aside money
`GET /savings-goal/summary` MUST report `totalSaved` as the sum of `currentAmount` across the user's non-closed goals.

#### Scenario: Two goals
- GIVEN goals G1 ($50k) and G2 ($20k) and a closed goal G3
- WHEN the summary is read
- THEN `totalSaved` is $70k

### Requirement: Ownership
Every operation above MUST respond 404 when the goal or account belongs to another user.

#### Scenario: Deposit into another user's goal
- GIVEN goal G owned by user B
- WHEN user A deposits into G
- THEN the system responds 404
