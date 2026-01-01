package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.dto.TransactionRelatedEntities;
import com.veritech.BudgetKing.filter.TransactionFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.TransactionMapper;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionController implements ICrudController<Transaction, TransactionDTO, UUID, TransactionFilter> {

    private final TransactionService service;
    private final TransactionMapper mapper;

    @Override
    public ICrudService<Transaction, UUID, TransactionFilter> getService() {
        return service;
    }

    @Override
    public ICrudMapper<Transaction, TransactionDTO, TransactionRelatedEntities> getMapper() {
        return mapper;
    }
}
