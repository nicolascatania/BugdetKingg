package com.veritech.BudgetKing.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read/write representation of a monthly budget.
 *
 * <p>{@code categoryName} and {@code categoryIcon} are denormalised for the UI
 * and ignored on the way in — the category is always resolved from
 * {@code category}, the identifier.</p>
 */
public record BudgetDTO(
        UUID id,
        @NotNull(message = "Category is mandatory")
        UUID category,
        String categoryName,
        String categoryIcon,
        int year,
        int month,
        @NotNull(message = "Limit amount is mandatory")
        BigDecimal limitAmount
) {

    /** A budget must land on a real calendar month. */
    @AssertTrue(message = "Month must be between 1 and 12")
    public boolean isMonthValid() {
        return month >= 1 && month <= 12;
    }

    /** A limit of zero (or less) would make the progress reading meaningless. */
    @AssertTrue(message = "Limit amount must be greater than zero")
    public boolean isLimitAmountValid() {
        return limitAmount != null && limitAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
