package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.BudgetDTO;
import com.veritech.BudgetKing.dto.BudgetProgressDTO;
import com.veritech.BudgetKing.filter.BudgetFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("budget")
@RequiredArgsConstructor
public class BudgetController implements ICrudController<BudgetDTO, UUID, BudgetFilter> {

    private final BudgetService budgetService;

    @Override
    public ICrudService<BudgetDTO, UUID, BudgetFilter> getService() {
        return budgetService;
    }

    /**
     * Progress of every budget defined for a calendar period.
     *
     * @param year  calendar year of the period
     * @param month calendar month of the period, 1 through 12
     * @return the period's budgets with their spent, remaining and status
     */
    @GetMapping("/progress")
    public ResponseEntity<List<BudgetProgressDTO>> getProgress(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(budgetService.getProgress(year, month));
    }
}
