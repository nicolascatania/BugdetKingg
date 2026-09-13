package com.veritech.BudgetKing.mapper;

import com.veritech.BudgetKing.dto.LastMovesDTO;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.dto.TransactionRelatedEntities;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.model.Transaction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TransactionMapper implements ICrudMapper<Transaction, TransactionDTO, TransactionRelatedEntities> {

    @Override
    public TransactionDTO toDto(Transaction entity) {
        return new TransactionDTO(
                entity.getId(),
                entity.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                entity.getAmount(),
                entity.getType().name(),
                entity.getCounterparty(),
                entity.getDescription(),
                entity.getCategory() != null ? entity.getCategory().getId() : null,
                entity.getCategory() != null ? entity.getCategory().getName() : null,
                entity.getAccount().getId(),
                entity.getDestinationAccount() != null ? entity.getDestinationAccount().getId() : null,
                entity.getAccount().getName(),
                entity.getSavingsGoal() != null ? entity.getSavingsGoal().getId() : null,
                entity.getSavingsGoal() != null ? entity.getSavingsGoal().getName() : null
        );
    }

    @Override
    public Transaction toEntity(TransactionDTO dto, TransactionRelatedEntities r) {
        return new Transaction(
                dto.id(),
                LocalDateTime.parse(dto.date()),
                dto.amount(),
                TransactionType.valueOf(dto.type()),
                dto.description(),
                dto.counterparty(),
                r.Category(),
                r.account(),
                r.destinationAccount(),
                r.savingsGoal(),
                r.user()
        );
    }

    /**
     * Compact row for the home "last moves" widget. Uncategorised movements show
     * what they are instead of a blank: the goal name for savings movements,
     * "Transfer" for account-to-account ones.
     */
    public LastMovesDTO toLastMovesDTO(Transaction entity) {
        String categoryLabel;
        if (entity.getCategory() != null) {
            categoryLabel = entity.getCategory().getName();
        } else if (entity.getSavingsGoal() != null) {
            categoryLabel = entity.getSavingsGoal().getName();
        } else {
            categoryLabel = "Transfer";
        }

        return new LastMovesDTO(
                entity.getId(),
                entity.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                entity.getAmount(),
                entity.getType().name(),
                entity.getCounterparty(),
                entity.getDescription(),
                categoryLabel,
                entity.getAccount().getName()
        );
    }
}
