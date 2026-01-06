package com.veritech.BudgetKing.mapper;

import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.dto.CategoryRelatedEntities;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper implements ICrudMapper<Category, CategoryDTO, CategoryRelatedEntities> {
    @Override
    public CategoryDTO toDto(Category entity) {
        return new CategoryDTO(entity.getId(), entity.getName(), entity.getDescription());
    }

    @Override
    public Category toEntity(CategoryDTO dto, CategoryRelatedEntities relatedEntities) {
        return new Category(
                dto.id(),
                dto.name(),
                dto.description(),
                relatedEntities.user()
        );
    }
}
