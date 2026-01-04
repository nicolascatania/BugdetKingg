package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;

public record MonthlyTransactionReportDTO(
        BigDecimal income,
        BigDecimal outcome
) {
}
