package com.veritech.BudgetKing.dto;

import com.veritech.BudgetKing.enumerator.TransactionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Wire representation of a recurring transaction template.
 *
 * <p>{@code categoryName} and {@code accountName} are denormalised so a list view can
 * render a row without a second round trip.</p>
 */
public record RecurringTransactionDTO(
        UUID id,
        @NotBlank(message = "Description is mandatory")
        String description,
        @NotNull(message = "Amount must be provided")
        BigDecimal amount,
        @NotBlank(message = "Transaction Type is mandatory")
        String type,
        String counterparty,
        UUID category, // only required for non-transfer templates
        String categoryName,
        @NotNull(message = "Account ID is mandatory")
        UUID account,
        String accountName,
        UUID destinationAccount,
        @NotBlank(message = "Frequency is mandatory")
        String frequency,
        @NotNull(message = "Next run date is mandatory")
        LocalDate nextRunDate,
        LocalDate endDate,
        boolean active
) {

    @AssertTrue(message = "destinationAccount is mandatory when transaction type is TRANSFER")
    public boolean isDestinationAccountValid() {
        if (!TransactionType.TRANSFER.name().equals(type)) {
            return true;
        }
        return destinationAccount != null;
    }

    @AssertTrue(message = "category is mandatory for INCOME and EXPENSE templates")
    public boolean isCategoryRequired() {
        if (TransactionType.TRANSFER.name().equals(type)) {
            return true; // TRANSFER templates carry no category
        }
        return category != null;
    }

    @AssertTrue(message = "endDate cannot be earlier than the next run date")
    public boolean isEndDateValid() {
        if (endDate == null || nextRunDate == null) {
            return true;
        }
        return !endDate.isBefore(nextRunDate);
    }

    @AssertTrue(message = "Amount must be greater than zero")
    public boolean isAmountPositive() {
        return amount == null || amount.signum() > 0;
    }
}
