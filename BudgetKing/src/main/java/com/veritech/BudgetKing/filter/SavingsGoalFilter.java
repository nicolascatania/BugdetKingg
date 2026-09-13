package com.veritech.BudgetKing.filter;

import com.veritech.BudgetKing.enumerator.SavingsGoalStatus;
import com.veritech.BudgetKing.filter.generic.GenericSpecifications;
import com.veritech.BudgetKing.filter.generic.PageableFilter;
import com.veritech.BudgetKing.filter.generic.SpecificationFilter;
import com.veritech.BudgetKing.model.SavingsGoal;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.dict.SavingsGoal_;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Search criteria for {@link SavingsGoal}. Every component is optional; a
 * {@code null} component is simply left out of the resulting specification.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SavingsGoalFilter extends PageableFilter implements SpecificationFilter<SavingsGoal> {

    private String name;
    private Boolean achieved;
    private SavingsGoalStatus status;
    private BigDecimal targetAmountMin;
    private BigDecimal targetAmountMax;

    /**
     * Builds the specification without a user restriction. Callers that need
     * results scoped to a single user must use {@link #toSpecification(AppUser)}.
     */
    @Override
    public Specification<SavingsGoal> toSpecification() {
        Specification<SavingsGoal> spec = (root, query, cb) -> cb.conjunction();
        return applyCriteria(spec);
    }

    /** Builds the specification restricted to the goals owned by {@code user}. */
    @Override
    public Specification<SavingsGoal> toSpecification(AppUser user) {
        Specification<SavingsGoal> spec = (root, query, cb) -> cb.equal(root.get(SavingsGoal_.user), user);
        return applyCriteria(spec);
    }

    private Specification<SavingsGoal> applyCriteria(Specification<SavingsGoal> spec) {
        if (name != null && !name.isBlank()) {
            spec = spec.and(GenericSpecifications.likeIgnoreCase(SavingsGoal_.name, name));
        }
        if (achieved != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(SavingsGoal_.achieved), achieved));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(SavingsGoal_.status), status));
        }
        if (targetAmountMin != null || targetAmountMax != null) {
            spec = spec.and(GenericSpecifications.between(
                    SavingsGoal_.targetAmount, targetAmountMin, targetAmountMax, true));
        }
        return spec;
    }
}
