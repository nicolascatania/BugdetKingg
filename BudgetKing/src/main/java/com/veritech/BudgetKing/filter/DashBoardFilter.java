package com.veritech.BudgetKing.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashBoardFilter {

    private String dateFrom;
    private String dateTo;
    private List<UUID> accountIds;
    private List<UUID> categoryIds;
    private List<UUID> transactionTypes;

}
