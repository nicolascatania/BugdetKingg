package com.veritech.BudgetKing.dto;

import com.veritech.BudgetKing.enumerator.TransactionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Transport shape of a transaction.
 *
 * @param savingsGoal     goal linked to a {@code SAVINGS_DEPOSIT} / {@code SAVINGS_WITHDRAWAL},
 *                        {@code null} for every other type; immutable on update
 * @param savingsGoalName denormalised goal name, for display only
 */
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
        UUID category, // only required for INCOME and EXPENSE transactions
        String categoryName,
        @NotNull(message = "Account ID is mandatory")
        UUID account,
        UUID destinationAccount,
        String accountName,
        UUID savingsGoal,
        String savingsGoalName
) {
        @AssertTrue(message = "destinationAccount is mandatory when transaction type is TRANSFER")
        public boolean isDestinationAccountValid() {
                if (!TransactionType.TRANSFER.name().equals(type)) {
                        return true;
                }
                return destinationAccount != null;
        }

        /**
         * TRANSFER and the savings types merely move money between pockets, so
         * they carry no category; INCOME and EXPENSE must be categorised.
         */
        @AssertTrue(message = "category is mandatory for INCOME and EXPENSE transactions")
        public boolean isCategoryRequired() {
                if (!TransactionType.INCOME.name().equals(type) && !TransactionType.EXPENSE.name().equals(type)) {
                        return true;
                }
                return category != null;
        }
}
