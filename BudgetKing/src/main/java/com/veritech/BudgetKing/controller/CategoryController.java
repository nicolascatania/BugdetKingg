package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.filter.CategoryFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
