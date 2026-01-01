package com.veritech.BudgetKing.filter.generic;

import org.springframework.data.jpa.domain.Specification;

public interface SpecificationFilter<E> {

    Specification<E> toSpecification();
}
