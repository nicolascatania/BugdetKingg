package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.AccountDTO;
import com.veritech.BudgetKing.dto.AccountRelatedEntities;
import com.veritech.BudgetKing.filter.AccountFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.AccountMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.AccountRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AccountService implements ICrudService<AccountDTO, UUID, AccountFilter> {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final SecurityUtils securityUtils;

    @Override
    public AccountDTO getById(UUID uuid) {
        AppUser user = securityUtils.getCurrentUser();
        return accountRepository.findByIdAndUser(uuid, user)
                .map(accountMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    @Override
    @Transactional
    public AccountDTO create(AccountDTO dto) {

        AppUser user = securityUtils.getCurrentUser();

        AccountRelatedEntities accountRelatedEntities = new AccountRelatedEntities(user);
        Account account = accountMapper.toEntity(dto, accountRelatedEntities);

        Account saved = accountRepository.save(account);

        return accountMapper.toDto(saved);
    }

    @Override
    @Transactional
    public AccountDTO update(UUID uuid, AccountDTO dto) {

        AppUser user = securityUtils.getCurrentUser();

        Account existing = accountRepository.findByIdAndUser(uuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        existing.setName(dto.name());
        existing.setDescription(dto.description());
        existing.setBalance(dto.balance());

        Account updated = accountRepository.save(existing);
        return accountMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteById(UUID uuid) {

        AppUser user = securityUtils.getCurrentUser();

        Account existing = accountRepository.findByIdAndUser(uuid, user)
                        .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        accountRepository.delete(existing);
    }

    @Override
    public List<AccountDTO> search(AccountFilter filter) {
        List<Account> accounts;

        if (filter == null) {
            accounts = accountRepository.findAll();
        } else {
            accounts = accountRepository.findAll(filter.toSpecification());
        }

        return accounts.stream().map(accountMapper::toDto).collect(Collectors.toList());
    }

    
    public Account getEntityById(UUID uuid) {
        AppUser user = securityUtils.getCurrentUser();

        return accountRepository.findByIdAndUser(uuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    public List<AccountDTO> getByUser() {
        AppUser user = securityUtils.getCurrentUser();
        return accountRepository.findAllByUser(user);
    }
}