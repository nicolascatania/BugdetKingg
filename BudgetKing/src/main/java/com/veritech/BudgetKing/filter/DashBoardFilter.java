package com.veritech.BudgetKing.filter;

import com.veritech.BudgetKing.dto.DashBoardDTO;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashBoardFilter {

    private String dateFrom; // ISO-8601: 2025-01-01
    private String dateTo;   // ISO-8601: 2025-01-31
}