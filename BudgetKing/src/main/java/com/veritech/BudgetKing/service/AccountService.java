package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.AccountDTO;
import com.veritech.BudgetKing.dto.AccountRelatedEntities;
import com.veritech.BudgetKing.enumerator.TransactionCategory;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.exception.AccountRuntimeException;
import com.veritech.BudgetKing.filter.AccountFilter;
import com.veritech.BudgetKing.dto.OptionDTO;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.AccountMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.AccountRepository;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AccountService implements ICrudService<AccountDTO, UUID, AccountFilter> {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final SecurityUtils securityUtils;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

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

        Category category = categoryService.getDefaultCategory(user);

        Transaction firstTransaction = new Transaction();
        firstTransaction.setAccount(account);
        firstTransaction.setAmount(account.getBalance());
        firstTransaction.setDescription("First transaction generating a new account");
        firstTransaction.setCounterparty("None");
        firstTransaction.setType(TransactionType.INCOME);
        firstTransaction.setCategory(category);
        firstTransaction.setDate(LocalDateTime.now());
        firstTransaction.setUser(user);

        transactionRepository.save(firstTransaction);

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
        // existing.setBalance(dto.balance()); it should just change name and description, not balance

        Account updated = accountRepository.save(existing);
        return accountMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteById(UUID uuid) {

        AppUser user = securityUtils.getCurrentUser();

        Account existing = accountRepository.findByIdAndUser(uuid, user)
                        .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if(accountRepository.countTransactionsByAccountAndUser(existing.getId(), user) > 0)
            throw new AccountRuntimeException("This account has transactions associated, can`t be deleted");


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

    public List<OptionDTO> getOptions() {
        AppUser user = securityUtils.getCurrentUser();

        return accountRepository.findAllByUser(user)
                .stream().map( a ->
                        new OptionDTO(a.id().toString(), a.name())
                ).toList();

    }
}