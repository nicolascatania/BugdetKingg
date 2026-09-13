package com.veritech.BudgetKing.enumerator;

/**
 * Kinds of money movement a {@link com.veritech.BudgetKing.model.Transaction} can represent.
 *
 * <p>{@link #SAVINGS_DEPOSIT} and {@link #SAVINGS_WITHDRAWAL} move money between an
 * account and a savings goal. They are only produced by the savings-goal endpoints,
 * never by the generic transaction create endpoint, and they are excluded from every
 * income/expense report because the money merely changes pocket.</p>
 */
public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    /** Account → savings goal: the account balance drops, the goal's saved amount grows. */
    SAVINGS_DEPOSIT,
    /** Savings goal → account: the goal's saved amount drops, the account balance grows. */
    SAVINGS_WITHDRAWAL;

    /** Whether this type moves money to or from a savings goal. */
    public boolean isSavings() {
        return this == SAVINGS_DEPOSIT || this == SAVINGS_WITHDRAWAL;
    }

    public static TransactionType fromString(String value) {
        try {
            return TransactionType.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Transaction type not valid: " + value);
        }
    }
}
