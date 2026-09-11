package com.veritech.BudgetKing.exception;

/**
 * Raised when a CSV import cannot be processed at all, e.g. the uploaded file is
 * empty, its header does not match the expected columns or the confirmed payload
 * carries no importable row.
 *
 * <p>Individual bad rows are never signalled with this exception: they are flagged
 * inside the preview so the user can review and fix them.</p>
 */
public class AccountImportRuntimeException extends RuntimeException {
    public AccountImportRuntimeException(String message) {
        super(message);
    }
}
