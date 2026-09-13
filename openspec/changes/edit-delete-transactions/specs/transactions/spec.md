# Transactions Specification (editing & deletion)

## Purpose

No prior spec exists for the `transactions` domain. This first spec covers only the behavior introduced by this change: editing and deleting a user's own transactions, with account balances kept consistent. Creation, search, import/export, and recurring transactions are out of scope and unspecified here.

## Requirements

### Requirement: Restricted transaction editing
The system MUST allow a user to update `amount`, `description`, `category`, `counterparty`, and `date` of a transaction they own. The system MUST NOT allow `account`, `destinationAccount`, or `type` to change via update.

#### Scenario: User edits amount and description
- GIVEN an EXPENSE transaction of $100 owned by the user
- WHEN the user submits an update changing amount to $150 and description
- THEN the transaction is saved with amount $150 and the new description
- AND the owning account's balance reflects only the $50 difference

#### Scenario: User attempts to change account
- GIVEN a transaction owned by the user on Account A
- WHEN the user submits an update with a different `account` id
- THEN the system rejects the request with 400 Bad Request
- AND no balance or transaction data changes

#### Scenario: User attempts to change type
- GIVEN an INCOME transaction owned by the user
- WHEN the user submits an update with `type` = TRANSFER
- THEN the system rejects the request with 400 Bad Request

### Requirement: Balance consistency on edit
The system MUST reverse the balance effect of the transaction's stored amount before applying the effect of the new amount, atomically, for EXPENSE, INCOME, and TRANSFER types.

#### Scenario: Editing a TRANSFER amount
- GIVEN a TRANSFER of $200 from Account A to Account B
- WHEN the user updates the amount to $300
- THEN Account A's balance reflects an additional $100 subtracted
- AND Account B's balance reflects an additional $100 added
- AND both updates commit together or neither does

### Requirement: Transaction deletion
The system MUST allow a user to delete a transaction they own, reversing its balance effect on the associated account(s).

#### Scenario: Deleting an EXPENSE
- GIVEN an EXPENSE of $80 owned by the user on Account A
- WHEN the user deletes it
- THEN the transaction no longer exists
- AND Account A's balance increases by $80

#### Scenario: Deleting a TRANSFER
- GIVEN a TRANSFER of $50 from Account A to Account B
- WHEN the user deletes it
- THEN Account A's balance increases by $50
- AND Account B's balance decreases by $50

### Requirement: Ownership enforcement
The system MUST NOT allow a user to update or delete a transaction owned by another user.

#### Scenario: Editing another user's transaction
- GIVEN a transaction owned by User B
- WHEN User A submits an update for that transaction's id
- THEN the system responds with 404 Not Found
- AND the transaction is unchanged

#### Scenario: Deleting another user's transaction
- GIVEN a transaction owned by User B
- WHEN User A submits a delete for that transaction's id
- THEN the system responds with 404 Not Found
- AND the transaction still exists

### Requirement: Frontend edit/delete access
The system SHOULD expose edit and delete actions per transaction row in the transaction list (table and mobile card views), and MUST require explicit confirmation before deleting.

#### Scenario: User opens edit from the list
- GIVEN the transaction list is showing the user's transactions
- WHEN the user triggers edit on a row
- THEN the edit form opens pre-filled with that transaction's data
- AND the account and type fields are read-only

#### Scenario: User cancels a delete
- GIVEN the user triggered delete on a row
- WHEN the confirmation dialog appears and the user cancels
- THEN the transaction is not deleted
