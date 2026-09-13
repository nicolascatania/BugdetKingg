package com.veritech.BudgetKing.dto;

import com.veritech.BudgetKing.enumerator.SavingsGoalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Transport shape of a savings goal.
 *
 * <p>The first block of components mirrors what the client may set. Everything
 * from {@code status} onwards is <strong>read-only</strong>: the service fills
 * it in on every read and ignores whatever the client sends, so clients may
 * safely omit those components on create/update payloads. Money only enters or
 * leaves a goal through the deposit/withdraw/close endpoints, never through
 * this DTO.</p>
 *
 * @param id                 identifier, {@code null} when creating
 * @param name               user facing label of the goal
 * @param icon               Font Awesome class rendered next to the name
 * @param targetAmount       how much has to be saved
 * @param targetDate         the day the target should be met
 * @param linkedAccountId    optional default source account for contributions
 * @param linkedAccountName  denormalised account name, for display only
 * @param status             persisted lifecycle ({@code ACTIVE} / {@code CLOSED})
 * @param state              derived lifecycle for display: {@code ACTIVE}, {@code ACHIEVED},
 *                           {@code OVERDUE} or {@code CLOSED}
 * @param achieved           whether {@code currentAmount} already covers the target
 * @param currentAmount      money set aside in the goal
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

        SavingsGoalStatus status,
        String state,
        boolean achieved,

        BigDecimal currentAmount,
        BigDecimal progressPercentage,
        BigDecimal remainingAmount,
        BigDecimal monthlyRequired,
        long daysRemaining
) {

    /** Derived display states; {@code CLOSED} mirrors the persisted status. */
    public static final String STATE_ACTIVE = "ACTIVE";
    public static final String STATE_ACHIEVED = "ACHIEVED";
    public static final String STATE_OVERDUE = "OVERDUE";
    public static final String STATE_CLOSED = "CLOSED";

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
