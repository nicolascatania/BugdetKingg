package com.veritech.BudgetKing.enumerator;

public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER;


    public static TransactionType fromString(String value) {
        try {
            return TransactionType.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Transaction type not valid: " + value);
        }
    }
}
