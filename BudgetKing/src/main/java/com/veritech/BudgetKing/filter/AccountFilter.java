package com.veritech.BudgetKing.filter;

import com.veritech.BudgetKing.filter.generic.SpecificationFilter;
import com.veritech.BudgetKing.model.Account;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class AccountFilter implements SpecificationFilter<Account> {


    private String name;
    private String description;
    private BigDecimal balanceMin;
    private BigDecimal balanceMax;

    @Override
    public Specification<Account> toSpecification() {
        return null;
    }
}
