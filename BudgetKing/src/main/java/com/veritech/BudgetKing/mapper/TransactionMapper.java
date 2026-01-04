package com.veritech.BudgetKing.mapper;

import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.dto.TransactionRelatedEntities;
import com.veritech.BudgetKing.enumerator.TransactionCategory;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.model.Transaction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
                entity.getCategory().name(),
                entity.getAccount().getId(),
                entity.getDestinationAccount() != null ? entity.getDestinationAccount().getId() : null
        );
    }

    @Override
    public Transaction toEntity(TransactionDTO dto, TransactionRelatedEntities r) {
        return new Transaction(
                dto.id(),
                LocalDateTime.parse(dto.date(), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                dto.amount(),
                TransactionType.valueOf(dto.type()),
                dto.description(),
                dto.counterparty(),
                TransactionCategory.valueOf(dto.category()),
                r.account(),
                r.destinationAccount(),
                r.user()
        );
    }


}