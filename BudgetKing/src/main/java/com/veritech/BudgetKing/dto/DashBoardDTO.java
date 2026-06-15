package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashBoardDTO(
        BigDecimal totalBalance,
        BigDecimal expense,
        BigDecimal income,
        List<CategoryExpenseDTO> expensesByCategory

) {
}
