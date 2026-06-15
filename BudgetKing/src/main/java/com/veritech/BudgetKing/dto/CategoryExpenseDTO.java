package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;

public record CategoryExpenseDTO(
        String name,
        String icon,
        BigDecimal amount,
        double percentage
) {
}
