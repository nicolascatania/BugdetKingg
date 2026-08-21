package com.veritech.BudgetKing.filter;

import com.veritech.BudgetKing.filter.generic.PageableFilter;
import com.veritech.BudgetKing.filter.generic.SpecificationFilter;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Budget;
import com.veritech.BudgetKing.model.dict.Budget_;
import com.veritech.BudgetKing.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/** Search criteria for budgets: calendar period and/or a single category. */
@Data
@EqualsAndHashCode(callSuper = false)
public class BudgetFilter extends PageableFilter implements SpecificationFilter<Budget> {

    private Integer year;
    private Integer month;

    /** Category identifier as sent by the client, parsed to a UUID when present. */
    private String category;

    @Override
    public Specification<Budget> toSpecification() {
        return null;
    }

    @Override
    public Specification<Budget> toSpecification(AppUser user) {

        Specification<Budget> spec =
                Specification.where((root, query, cb) -> cb.conjunction());

        // Budgets are always scoped to the authenticated user first.
        spec = spec.and((root, query, cb) -> cb.equal(root.get(Budget_.user), user));

        // Period
        if (year != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(Budget_.year), year));
        }

        if (month != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(Budget_.month), month));
        }

        // Category
        if (!StringUtils.isBlankOrNUll(category)) {
            UUID categoryId = UUID.fromString(category);
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get(Budget_.category).get("id"), categoryId));
        }

        return spec;
    }
}
