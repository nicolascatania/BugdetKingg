package com.veritech.BudgetKing.mapper;

import com.veritech.BudgetKing.dto.AccountDTO;
import com.veritech.BudgetKing.dto.AccountRelatedEntities;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper implements ICrudMapper<Account, AccountDTO, AccountRelatedEntities> {

    @Override
    public AccountDTO toDto(Account entity) {
        return new AccountDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getBalance(),
                entity.getUser().getId()
        );
    }

    @Override
    public Account toEntity(AccountDTO dto, AccountRelatedEntities relatedEntities) {
        return new Account(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.balance(),
                relatedEntities.user()
        );
    }
}
