package com.veritech.BudgetKing.exception;

/**
 * Raised by the email/password endpoints when {@code app.auth.local-enabled}
 * is off (the current default: login is Google-only). The endpoints and their
 * underlying code stay in place so this path can be re-enabled without a
 * rewrite if a non-Google fallback is ever needed.
 */
public class LocalAuthDisabledException extends RuntimeException {
    public LocalAuthDisabledException(String message) {
        super(message);
    }
}
