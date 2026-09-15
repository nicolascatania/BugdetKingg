package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.dto.CategorySpendingDTO;
import com.veritech.BudgetKing.filter.CategoryFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("category")
@RequiredArgsConstructor
public class CategoryController implements ICrudController<CategoryDTO, UUID, CategoryFilter> {

    private final CategoryService categoryService;

    @Override
    public ICrudService<CategoryDTO, UUID, CategoryFilter> getService() {
        return categoryService;
    }

    /**
     * Spending on one category: the requested month in detail plus the all-time total.
     *
     * @param id    identifier of the category
     * @param year  calendar year of the month to detail
     * @param month calendar month to detail, 1 through 12
     * @return totals, counts and the month's expenses
     */
    @GetMapping("/{id}/spending")
    public ResponseEntity<CategorySpendingDTO> getSpending(
            @PathVariable UUID id,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(categoryService.getSpending(id, year, month));
    }
}
