package com.veritech.BudgetKing.filter;

public record CategoryFilter(
        String name,
        int page,
        int size
) {}