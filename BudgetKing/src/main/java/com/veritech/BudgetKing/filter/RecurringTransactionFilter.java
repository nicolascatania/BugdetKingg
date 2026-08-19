package com.veritech.BudgetKing.filter;

import com.veritech.BudgetKing.enumerator.RecurrenceFrequency;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.filter.generic.PageableFilter;
import com.veritech.BudgetKing.filter.generic.SpecificationFilter;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.RecurringTransaction;
import com.veritech.BudgetKing.model.dict.RecurringTransaction_;
import com.veritech.BudgetKing.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Search criteria for recurring transaction templates.
 *
 * <p>Every criterion is optional; the user scope is always applied.</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RecurringTransactionFilter extends PageableFilter implements SpecificationFilter<RecurringTransaction> {

    private String description;
    private String counterparty;
    private String type;
    private String frequency;

    private String account;
    private String category;

    /** {@code null} means "active and paused alike". */
    private Boolean active;

    private LocalDate nextRunFrom;
    private LocalDate nextRunTo;

    @Override
    public Specification<RecurringTransaction> toSpecification() {
        return null;
    }

    @Override
    public Specification<RecurringTransaction> toSpecification(AppUser user) {

        Specification<RecurringTransaction> spec =
                Specification.where((root, query, cb) -> cb.conjunction());

        spec = spec.and((root, query, cb) -> cb.equal(root.get(RecurringTransaction_.user), user));

        // Next run window
        if (nextRunFrom != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get(RecurringTransaction_.nextRunDate), nextRunFrom));
        }

        if (nextRunTo != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get(RecurringTransaction_.nextRunDate), nextRunTo));
        }

        // Account
        if (!StringUtils.isBlankOrNUll(account)) {
            UUID accountId = UUID.fromString(account);
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get(RecurringTransaction_.account).get("id"), accountId));
        }

        // Category
        if (!StringUtils.isBlankOrNUll(category)) {
            UUID categoryId = UUID.fromString(category);
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get(RecurringTransaction_.category).get("id"), categoryId));
        }

        // Type
        if (!StringUtils.isBlankOrNUll(type)) {
            TransactionType transactionType = TransactionType.fromString(type);
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get(RecurringTransaction_.type), transactionType));
        }

        // Frequency
        if (!StringUtils.isBlankOrNUll(frequency)) {
            RecurrenceFrequency recurrenceFrequency = RecurrenceFrequency.fromString(frequency);
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get(RecurringTransaction_.frequency), recurrenceFrequency));
        }

        // Active / paused
        if (active != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get(RecurringTransaction_.active), active));
        }

        // Description
        if (!StringUtils.isBlankOrNUll(description)) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get(RecurringTransaction_.description)),
                            "%" + description.toLowerCase() + "%"));
        }

        // Counterparty
        if (!StringUtils.isBlankOrNUll(counterparty)) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get(RecurringTransaction_.counterparty)),
                            "%" + counterparty.toLowerCase() + "%"));
        }

        return spec;
    }
}
