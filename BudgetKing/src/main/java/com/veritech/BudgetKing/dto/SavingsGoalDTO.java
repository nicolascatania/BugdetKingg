package com.veritech.BudgetKing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Transport shape of a savings goal.
 *
 * <p>The first block of components mirrors what is persisted. Everything from
 * {@code currentAmount} onwards is <strong>derived and read-only</strong>: the
 * service recomputes it on every read and ignores whatever the client sends, so
 * clients may safely omit those components on create/update payloads.</p>
 *
 * @param id                 identifier, {@code null} when creating
 * @param name               user facing label of the goal
 * @param icon               Font Awesome class rendered next to the name
 * @param targetAmount       how much has to be saved
 * @param targetDate         the day the target should be met
 * @param linkedAccountId    optional account funding the goal
 * @param linkedAccountName  denormalised account name, for display only
 * @param achieved           whether the linked balance already covers the target
 * @param currentAmount      balance of the linked account, zero when unlinked
 * @param progressPercentage {@code currentAmount / targetAmount * 100}, uncapped
 * @param remainingAmount    how much is still missing, never negative
 * @param monthlyRequired    remaining amount spread over the whole months left
 * @param daysRemaining      days until {@code targetDate}, never negative
 */
public record SavingsGoalDTO(
        UUID id,

        @NotBlank(message = "Name is mandatory")
        String name,

        @NotBlank(message = "Icon is mandatory")
        String icon,

        @NotNull(message = "Target amount is mandatory")
        BigDecimal targetAmount,

        @NotNull(message = "Target date is mandatory")
        LocalDate targetDate,

        UUID linkedAccountId,
        String linkedAccountName,
        boolean achieved,

        BigDecimal currentAmount,
        BigDecimal progressPercentage,
        BigDecimal remainingAmount,
        BigDecimal monthlyRequired,
        long daysRemaining
) {

    /**
     * Normalises the derived money components so consumers never have to deal
     * with nulls. Persisted components are deliberately left untouched: they are
     * validated by Bean Validation and by the service's business rules.
     */
    public SavingsGoalDTO {
        currentAmount = currentAmount == null ? BigDecimal.ZERO : currentAmount;
        progressPercentage = progressPercentage == null ? BigDecimal.ZERO : progressPercentage;
        remainingAmount = remainingAmount == null ? BigDecimal.ZERO : remainingAmount;
        monthlyRequired = monthlyRequired == null ? BigDecimal.ZERO : monthlyRequired;
    }
}
