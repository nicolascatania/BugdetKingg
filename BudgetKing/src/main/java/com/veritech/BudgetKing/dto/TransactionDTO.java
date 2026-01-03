package com.veritech.BudgetKing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionDTO(
        UUID id,
        @NotBlank(message = "Date is mandatory")
        String date,
        @NotNull(message = "Amount must be provided")
        BigDecimal amount,
        @NotBlank(message = "Transaction Type is mandatory")
        String type,
        @NotBlank(message = "CounterParty is mandatory")
        String counterparty,
        @NotBlank(message = "Description is mandatory")
        String description,
        @NotBlank(message = "Category is mandatory")
        String category,
        @NotBlank(message = "Account is mandatory")
        String account
) {
}
