package com.veritech.BudgetKing.filter;

import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.filter.generic.GenericSpecifications;
import com.veritech.BudgetKing.filter.generic.SpecificationFilter;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.model.dict.Transaction_;
import com.veritech.BudgetKing.utils.StringUtils;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

@Data
public class TransactionFilter implements SpecificationFilter<Transaction> {

    private LocalDate dateFrom;
    private LocalDate dateTo;

    private Double minAmount;
    private Double maxAmount;

    private String account;
    private String category;

    private String type;
    private String description;
    private String counterparty;

    @Override
    public Specification<Transaction> toSpecification() {

        Specification<Transaction> spec =
                Specification.where((root, query, cb) -> cb.conjunction());

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

        // Account
        if (!StringUtils.isBlankOrNUll(account)) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get(Transaction_.account).get("id"), account));
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
