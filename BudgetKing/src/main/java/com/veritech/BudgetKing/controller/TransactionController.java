package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.*;
import com.veritech.BudgetKing.filter.DashBoardFilter;
import com.veritech.BudgetKing.filter.TransactionFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<LastMovesDTO>> movementsOfThisMonth() {
        return ResponseEntity.ok(service.movementsOfThisMonth());
    }

    @GetMapping("/monthly-balance")
    public ResponseEntity<MonthlyTransactionReportDTO> monthlyBalance() {
        return ResponseEntity.ok(service.getMonthlyBalance());
    }

    @PostMapping("dashboard")
    public ResponseEntity<DashBoardDTO> dashboard(@RequestBody DashBoardFilter filter) {
        return ResponseEntity.ok(service.getDataForDashBoard(filter));
    }

    @GetMapping("/income-expense-by-month")
    public ResponseEntity<List<MonthlyIncomeExpenseDTO>> incomeExpenseByMonth(
            @RequestParam(required = false) UUID accountId
    ) {
        return ResponseEntity.ok(service.getIncomeExpensePerMonth(accountId));
    }

}
