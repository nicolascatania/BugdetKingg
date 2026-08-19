package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.SavingsGoalDTO;
import com.veritech.BudgetKing.dto.SavingsGoalSummaryDTO;
import com.veritech.BudgetKing.filter.SavingsGoalFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.service.SavingsGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("savings-goal")
@RequiredArgsConstructor
public class SavingsGoalController implements ICrudController<SavingsGoalDTO, UUID, SavingsGoalFilter> {

    private final SavingsGoalService savingsGoalService;

    @Override
    public ICrudService<SavingsGoalDTO, UUID, SavingsGoalFilter> getService() {
        return savingsGoalService;
    }

    /** Aggregated progress across every savings goal owned by the current user. */
    @GetMapping("/summary")
    public ResponseEntity<SavingsGoalSummaryDTO> getSummary() {
        return ResponseEntity.ok(savingsGoalService.getSummary());
    }
}
