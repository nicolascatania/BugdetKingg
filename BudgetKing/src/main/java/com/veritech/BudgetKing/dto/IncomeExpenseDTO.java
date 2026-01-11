package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;

public record IncomeExpenseDTO(
        BigDecimal income,
        BigDecimal expense
) {}
