package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.dto.TransactionRelatedEntities;
import com.veritech.BudgetKing.filter.TransactionFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.TransactionMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService implements ICrudService<TransactionDTO, UUID, TransactionFilter> {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper mapper;
    private final SecurityUtils securityUtils;
    private final AccountService accountService;


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

        TransactionRelatedEntities related = new TransactionRelatedEntities(
                user,
                accountService.getEntityById(dto.id())
        );

        Transaction t = mapper.toEntity(dto, related);
        return mapper.toDto(transactionRepository.save(t));
    }

    @Override
    @Transactional
    public TransactionDTO update(UUID id, TransactionDTO dto) {
        AppUser user = securityUtils.getCurrentUser();

        Transaction existing = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        TransactionRelatedEntities related = new TransactionRelatedEntities(
                user,
                dto.id() != null ? accountService.getEntityById(dto.id()) : existing.getAccount()
        );

        Transaction updated = mapper.toEntity(dto, related);
        updated.setId(existing.getId());
        transactionRepository.save(updated);
        return mapper.toDto(updated);
    }

    @Override
    public void deleteById(UUID id) {
        AppUser user = securityUtils.getCurrentUser();

        Transaction existing = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        transactionRepository.delete(existing);
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
}