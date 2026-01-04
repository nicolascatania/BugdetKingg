package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.MonthlyTransactionReportDTO;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.filter.TransactionFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionController implements ICrudController<TransactionDTO, UUID, TransactionFilter> {

    private final TransactionService service;

    @Override
    public ICrudService<TransactionDTO, UUID, TransactionFilter> getService() {
        return service;
    }

    @GetMapping("/movements-this-month")
    public ResponseEntity<List<TransactionDTO>> movementsOfThisMonth() {
        return ResponseEntity.ok(service.movementsOfThisMonth());
    }

    @GetMapping("/monthly-balance")
    public ResponseEntity<MonthlyTransactionReportDTO> monthlyBalance() {
        return ResponseEntity.ok(service.getMonthlyBalance());
    }

}
