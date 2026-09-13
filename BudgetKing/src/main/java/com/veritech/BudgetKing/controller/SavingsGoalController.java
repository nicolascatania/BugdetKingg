package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.SavingsGoalCloseDTO;
import com.veritech.BudgetKing.dto.SavingsGoalContributionDTO;
import com.veritech.BudgetKing.dto.SavingsGoalDTO;
import com.veritech.BudgetKing.dto.SavingsGoalSummaryDTO;
import com.veritech.BudgetKing.filter.SavingsGoalFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.service.SavingsGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** Aggregated progress across every open savings goal owned by the current user. */
    @GetMapping("/summary")
    public ResponseEntity<SavingsGoalSummaryDTO> getSummary() {
        return ResponseEntity.ok(savingsGoalService.getSummary());
    }

    /** Sets money aside: moves {@code amount} from the given account into the goal. */
    @PostMapping("/{id}/deposit")
    public ResponseEntity<SavingsGoalDTO> deposit(
            @PathVariable UUID id,
            @Valid @RequestBody SavingsGoalContributionDTO dto
    ) {
        return ResponseEntity.ok(savingsGoalService.deposit(id, dto));
    }

    /** Takes money back: moves {@code amount} from the goal into the given account. */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<SavingsGoalDTO> withdraw(
            @PathVariable UUID id,
            @Valid @RequestBody SavingsGoalContributionDTO dto
    ) {
        return ResponseEntity.ok(savingsGoalService.withdraw(id, dto));
    }

    /**
     * Ends the goal, returning whatever it holds to the given account. The body is
     * optional for goals that are already empty.
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<SavingsGoalDTO> close(
            @PathVariable UUID id,
            @RequestBody(required = false) SavingsGoalCloseDTO dto
    ) {
        return ResponseEntity.ok(savingsGoalService.close(id, dto));
    }
}
