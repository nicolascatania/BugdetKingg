package com.veritech.BudgetKing.mapper;

import com.veritech.BudgetKing.dto.BudgetDTO;
import com.veritech.BudgetKing.dto.BudgetRelatedEntities;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.model.Budget;
import com.veritech.BudgetKing.model.Category;
import org.springframework.stereotype.Component;

/** Hand-written mapper between {@link Budget} and {@link BudgetDTO}. */
@Component
public class BudgetMapper implements ICrudMapper<Budget, BudgetDTO, BudgetRelatedEntities> {

    @Override
    public BudgetDTO toDto(Budget entity) {
        Category category = entity.getCategory();

        return new BudgetDTO(
                entity.getId(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                category != null ? category.getIcon() : null,
                entity.getYear(),
                entity.getMonth(),
                entity.getLimitAmount()
        );
    }

    @Override
    public Budget toEntity(BudgetDTO dto, BudgetRelatedEntities relatedEntities) {
        return Budget.builder()
                .id(dto.id())
                .category(relatedEntities.category())
                .user(relatedEntities.user())
                .year(dto.year())
                .month(dto.month())
                .limitAmount(dto.limitAmount())
                .build();
    }
}
