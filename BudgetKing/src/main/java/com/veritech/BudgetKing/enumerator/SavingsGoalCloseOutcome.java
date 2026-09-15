package com.veritech.BudgetKing.enumerator;

/**
 * What happens to the money a savings goal still holds when the user closes it.
 */
public enum SavingsGoalCloseOutcome {
    /** The saved money goes back to an account; the goal is abandoned or no longer needed. */
    RETURN,
    /**
     * The saved money was spent on what the goal was for. The balance is returned to
     * the account and an expense of the same amount is recorded against it, so the
     * account nets to zero and the purchase shows up in the expense history.
     */
    SPEND
}
