package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.RecurringTransactionDTO;
import com.veritech.BudgetKing.filter.RecurringTransactionFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for recurring transaction templates.
 *
 * <p>Standard CRUD comes from {@link ICrudController}; the extra endpoints below expose the
 * recurrence engine: firing every due template ({@code /run-due}, meant for an external
 * scheduler or a manual trigger since this project has no {@code @Scheduled} job wired up
 * yet), firing a single template immediately ({@code /{id}/run-now}), and projecting the
 * occurrences a user should expect over the next 30 days ({@code /upcoming}).</p>
 */
@RestController
@RequestMapping("/recurring-transaction")
@RequiredArgsConstructor
public class RecurringTransactionController
        implements ICrudController<RecurringTransactionDTO, UUID, RecurringTransactionFilter> {

    private final RecurringTransactionService service;

    @Override
    public ICrudService<RecurringTransactionDTO, UUID, RecurringTransactionFilter> getService() {
        return service;
    }

    /**
     * Fires every template due today, across all users.
     *
     * <p>Intended to be called by an external scheduler (e.g. a cron hitting this endpoint)
     * or manually from an admin tool, since the application does not run an in-process
     * scheduled job for this.</p>
     *
     * @return how many transactions were generated
     */
    @PostMapping("/run-due")
    public ResponseEntity<Map<String, Integer>> runDue() {
        int created = service.runDue();
        return ResponseEntity.ok(Map.of("created", created));
    }

    /**
     * Fires one occurrence of a single template right away, regardless of its
     * {@code nextRunDate}, and advances its cursor past today.
     *
     * @param id identifier of the template to fire
     * @return the template with its cursor already advanced
     */
    @PostMapping("/{id}/run-now")
    public ResponseEntity<RecurringTransactionDTO> runNow(@PathVariable UUID id) {
        return ResponseEntity.ok(service.runNow(id));
    }

    /**
     * Projects the occurrences the authenticated user can expect over the next 30 days,
     * without generating or mutating anything.
     *
     * @return upcoming occurrences ordered by date
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<RecurringTransactionDTO>> upcoming() {
        return ResponseEntity.ok(service.getUpcoming());
    }
}
