package com.veritech.BudgetKing.filter;

import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.filter.generic.GenericSpecifications;
import com.veritech.BudgetKing.filter.generic.PageableFilter;
import com.veritech.BudgetKing.filter.generic.SpecificationFilter;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.model.dict.Transaction_;
import com.veritech.BudgetKing.utils.StringUtils;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TransactionFilter extends PageableFilter implements SpecificationFilter<Transaction> {

    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;

    private Double minAmount;
    private Double maxAmount;

    private String account;
    private String category;

    private String type;
    private String description;
    private String counterparty;

    @Override
    public Specification<Transaction> toSpecification() {
        return null;
    }

    @Override
    public Specification<Transaction> toSpecification(AppUser user) {



        Specification<Transaction> spec =
                Specification.where((root, query, cb) -> cb.conjunction());

        spec = spec.and((root, query, cb) -> cb.equal(root.get(Transaction_.user), user));

        // Dates
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get(Transaction_.date), dateFrom));
        }

        if (dateTo != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get(Transaction_.date), dateTo));
        }

        // Amount
        if (minAmount != null || maxAmount != null) {
            spec = spec.and(
                    GenericSpecifications.between(Transaction_.amount, minAmount, maxAmount, true)
            );
        }

        if (!StringUtils.isBlankOrNUll(account)) {
            UUID accountId = UUID.fromString(account);
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get(Transaction_.account).get("id"), accountId));
        }

        // Category
        if (!StringUtils.isBlankOrNUll(category)) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get(Transaction_.category).get("id"), category));
        }

        // Type
        if (!StringUtils.isBlankOrNUll(type)) {
            TransactionType transactionType = TransactionType.valueOf(type);
            spec = spec.and(
                    GenericSpecifications.equalIgnoreCase(Transaction_.type, String.valueOf(transactionType))
            );
        }

        // Description
        if (!StringUtils.isBlankOrNUll(description)) {
            spec = spec.and(
                    GenericSpecifications.likeIgnoreCase(Transaction_.description, description)
            );
        }

        // Counterparty
        if (!StringUtils.isBlankOrNUll(counterparty)) {
            spec = spec.and(
                    GenericSpecifications.likeIgnoreCase(Transaction_.counterparty, counterparty)
            );
        }

        return spec;
    }
}
