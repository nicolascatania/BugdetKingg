package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LastMovesDTO(
        UUID id,
        String date,
        BigDecimal amount,
        String type,
        String counterparty,
        String description,
        String category,
        String accountName
)  {
}
