package com.veritech.BudgetKing.dto;

import com.veritech.BudgetKing.enumerator.TransactionType;
import jakarta.validation.constraints.AssertTrue;
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
        @NotNull(message = "Account ID is mandatory")
        UUID account,
        UUID destinationAccount
) {
        @AssertTrue(message = "destinationAccount is mandatory when transaction type is TRANSFER")
        public boolean isdestinationAccountValid() {
                if (!TransactionType.TRANSFER.equals(type)) {
                        return true;
                }
                return destinationAccount != null;
        }
}
