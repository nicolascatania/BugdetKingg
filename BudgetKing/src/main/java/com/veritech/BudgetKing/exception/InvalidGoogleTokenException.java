package com.veritech.BudgetKing.exception;

/**
 * Raised when a Google ID token fails verification: rejected by Google,
 * issued for a different OAuth client, or its email is not verified.
 */
public class InvalidGoogleTokenException extends RuntimeException {
    public InvalidGoogleTokenException(String message) {
        super(message);
    }
}
