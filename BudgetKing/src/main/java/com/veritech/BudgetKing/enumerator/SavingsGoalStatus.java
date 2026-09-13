package com.veritech.BudgetKing.enumerator;

/**
 * Persisted lifecycle of a {@link com.veritech.BudgetKing.model.SavingsGoal}.
 *
 * <p>Only the user's explicit decision to close a goal is stored. Whether a goal is
 * achieved or overdue changes with time and money, so those are derived at read
 * time instead (see {@code SavingsGoalDTO.state()}).</p>
 */
public enum SavingsGoalStatus {
    /** Accepts deposits, withdrawals and edits. */
    ACTIVE,
    /** Money returned to an account by the user; read-only from then on. */
    CLOSED
}
