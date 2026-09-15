package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;

/**
 * Aggregate of a set of expenses, built directly by a JPQL constructor expression.
 *
 * @param total sum of the amounts, zero when nothing matched
 * @param count how many expenses were summed
 */
public record ExpenseTotalDTO(BigDecimal total, long count) {
}
