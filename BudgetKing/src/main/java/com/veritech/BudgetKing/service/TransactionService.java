package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.dto.TransactionRelatedEntities;
import com.veritech.BudgetKing.filter.TransactionFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.TransactionMapper;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.TransactionRepository;
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
    private final AppUserService appUserService;
    private final AccountService accountService;

    @Override
    public TransactionDTO getById(UUID id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
        return mapper.toDto(t);
    }

    @Override
    @Transactional
    public TransactionDTO create(TransactionDTO dto) {

        TransactionRelatedEntities related = new TransactionRelatedEntities(
                appUserService.getEntityById(dto.userId()),
                accountService.getEntityById(dto.id())
        );

        Transaction t = mapper.toEntity(dto, related);
        Transaction saved = transactionRepository.save(t);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public TransactionDTO update(UUID id, TransactionDTO dto) {
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        TransactionRelatedEntities related = new TransactionRelatedEntities(
                dto.userId() != null ? appUserService.getEntityById(dto.userId()) : existing.getUser(),
                dto.id() != null ? accountService.getEntityById(dto.id()) : existing.getAccount()
        );

        Transaction updated = mapper.toEntity(dto, related);
        updated.setId(existing.getId());
        transactionRepository.save(updated);
        return mapper.toDto(updated);
    }

    @Override
    public void deleteById(UUID id) {
        transactionRepository.deleteById(id);
    }

    @Override
    public List<TransactionDTO> search(TransactionFilter filter) {
        return transactionRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }
}