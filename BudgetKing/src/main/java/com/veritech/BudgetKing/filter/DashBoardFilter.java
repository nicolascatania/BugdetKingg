package com.veritech.BudgetKing.filter;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashBoardFilter {

    private String dateFrom; // ISO-8601: 2025-01-01
    private String dateTo;   // ISO-8601: 2025-01-31
}