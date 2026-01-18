package com.veritech.BudgetKing.filter;

import com.veritech.BudgetKing.filter.generic.PageableFilter;
import com.veritech.BudgetKing.filter.generic.SpecificationFilter;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class AccountFilter extends PageableFilter implements SpecificationFilter<Account> {


    private String name;
    private String description;
    private BigDecimal balanceMin;
    private BigDecimal balanceMax;

    @Override
    public Specification<Account> toSpecification() {
        return null;
    }

    @Override
    public Specification<Account> toSpecification(AppUser user) {
        return null;
    }
}
