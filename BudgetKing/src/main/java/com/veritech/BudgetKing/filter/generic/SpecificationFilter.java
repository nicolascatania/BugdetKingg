package com.veritech.BudgetKing.filter.generic;

import com.veritech.BudgetKing.model.AppUser;
import org.springframework.data.jpa.domain.Specification;

public interface SpecificationFilter<E> {

    Specification<E> toSpecification();

    Specification<E> toSpecification(AppUser user);
}
