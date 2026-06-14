package com.veritech.BudgetKing.dto.argentinaAPI;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

public record InflationResponseDTO (
        @JsonAlias("fecha") String date,
        @JsonAlias("valor") BigDecimal value
){
}
