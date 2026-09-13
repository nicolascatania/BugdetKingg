package com.veritech.BudgetKing.security.enumerator;

/**
 * How an {@link com.veritech.BudgetKing.model.AppUser} authenticates.
 *
 * <p>{@code LOCAL} (email + password) is currently disabled at the controller
 * level — see {@code app.auth.local-enabled} — but the code path is kept so it
 * can be turned back on without a rewrite if Google-only login ever needs a
 * fallback.</p>
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
