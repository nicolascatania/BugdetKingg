package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.LastMovesDTO;
import com.veritech.BudgetKing.dto.MonthlyTransactionReportDTO;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.dto.TransactionRelatedEntities;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.filter.TransactionFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.TransactionMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.AccountRepository;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.veritech.BudgetKing.enumerator.TransactionType.*;

@Service
@RequiredArgsConstructor
public class TransactionService implements ICrudService<TransactionDTO, UUID, TransactionFilter> {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper mapper;
    private final SecurityUtils securityUtils;
    private final AccountService accountService;
    private final AccountRepository accountRepository;


    @Override
    public TransactionDTO getById(UUID id) {
        AppUser user = securityUtils.getCurrentUser();
        Transaction t = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
        return mapper.toDto(t);
    }

    @Override
    @Transactional
    public TransactionDTO create(TransactionDTO dto) {

        AppUser user = securityUtils.getCurrentUser();
        Account sourceAccount = accountService.getEntityById(dto.account());
        Account destinationAccount = resolveDestinationAccount(dto);

        validateTransaction(dto, sourceAccount, destinationAccount);

        applyBalanceChanges(dto, sourceAccount, destinationAccount);

        TransactionRelatedEntities related = new TransactionRelatedEntities(
                user,
                sourceAccount,
                destinationAccount
        );

        Transaction transaction = mapper.toEntity(dto, related);
        return mapper.toDto(transactionRepository.save(transaction));
    }


    @Override
    @Transactional
    public TransactionDTO update(UUID id, TransactionDTO dto) {
        // shall not be used
        return null;
    }

    @Override
    public void deleteById(UUID id) {
        //shall not be used
    }

    @Override
    public List<TransactionDTO> search(TransactionFilter filter) {
        return transactionRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Gets the entity by UUID and current user in the session
     * If not found, throws Runtime Exception
     * @param id UUID from the transaction the user is looking for
     * @return Transaction entity
     */
    public Transaction getTransaction(UUID id) {
        AppUser user = securityUtils.getCurrentUser();
        return transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
    }

    /**
     * Gets the list of the last movements of the month of the user
     * @return
     */
    public List<LastMovesDTO> movementsOfThisMonth() {
        AppUser user = securityUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMonths(1);

        return transactionRepository
                .findByUserAndDateBetween(user, start, end)
                .stream()
                .map(mapper::toLastMovesDTO)
                .toList();
    }

    /**
     * Gets the user`s monthly balance
     * @return
     */
    public MonthlyTransactionReportDTO getMonthlyBalance() {
        AppUser user = securityUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMonths(1);

        return transactionRepository.getMonthlyReport(user, start, end);
    }

    private void validateTransaction(
            TransactionDTO dto,
            Account source,
            Account destination
    ) {
        TransactionType type = TransactionType.valueOf(dto.type());
        if (type.equals(TRANSFER)) {
            if (destination == null) {
                throw new IllegalArgumentException("Destination account is required for TRANSFER");
            }
            if (source.getId().equals(destination.getId())) {
                throw new IllegalArgumentException("Source and destination accounts must be different");
            }
        }

        if (dto.amount().signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void applyBalanceChanges(
            TransactionDTO dto,
            Account source,
            Account destination
    ) {

        TransactionType type = TransactionType.valueOf(dto.type());

        switch (type) {
            case EXPENSE -> source.setBalance(source.getBalance().subtract(dto.amount()));

            case INCOME -> source.setBalance(source.getBalance().add(dto.amount()));

            case TRANSFER -> {
                source.setBalance(source.getBalance().subtract(dto.amount()));
                destination.setBalance(destination.getBalance().add(dto.amount()));
            }
        }
    }

    private Account resolveDestinationAccount(TransactionDTO dto) {
        if (dto.destinationAccount() == null) {
            return null;
        }
        return accountService.getEntityById(dto.destinationAccount());
    }



}