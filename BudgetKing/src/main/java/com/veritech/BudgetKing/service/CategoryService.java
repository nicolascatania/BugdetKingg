package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.dto.CategoryRelatedEntities;
import com.veritech.BudgetKing.dto.OptionDTO;
import com.veritech.BudgetKing.exception.CategoryRuntimeException;
import com.veritech.BudgetKing.filter.CategoryFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.CategoryMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICrudService<CategoryDTO, UUID, CategoryFilter> {

    private final CategoryRepository categoryRepository;
    private final SecurityUtils securityUtils;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryDTO getById(UUID uuid) {
        AppUser user = securityUtils.getCurrentUser();
        return categoryRepository.findByIdAndUser(uuid, user).map(categoryMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    @Override
    public CategoryDTO create(CategoryDTO dto) {
        AppUser user = securityUtils.getCurrentUser();
        CategoryRelatedEntities relatedEntities = new CategoryRelatedEntities(user);

        Category category = categoryMapper.toEntity(dto, relatedEntities);

        Category saved = categoryRepository.save(category);

        return categoryMapper.toDto(saved);
    }

    @Override
    public CategoryDTO update(UUID uuid, CategoryDTO dto) {
        AppUser user = securityUtils.getCurrentUser();
        Category found = categoryRepository.findByIdAndUser(uuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        found.setName(dto.name());

        Category saved = categoryRepository.save(found);
        return categoryMapper.toDto(saved);

    }

    @Override
    public void deleteById(UUID uuid) {
        AppUser user = securityUtils.getCurrentUser();
        Category found = categoryRepository.findByIdAndUser(uuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        if(categoryRepository.countTransactionsByCategory(found.getId(), user) > 0) {
            throw new CategoryRuntimeException("This category has transactions associated, cannot delete it");
        }

        if(categoryRepository.countByUser(user) < 2)
            throw new CategoryRuntimeException("Can`t delete this category, you have only one, create new ones and then delete this.");

        categoryRepository.delete(found);
    }

    @Override
    public Page<CategoryDTO> search(CategoryFilter categoryFilter) {
        AppUser user = securityUtils.getCurrentUser();

        Pageable pageable = (Pageable) PageRequest.of(
                categoryFilter.page(),
                categoryFilter.size(),
                Sort.by("name").ascending()
        );

        Page<Category> categoryPage = categoryRepository.findByUser(user, pageable);
        return categoryPage.map(categoryMapper::toDto);
    }

    public List<OptionDTO> getOptions() {
        AppUser user = securityUtils.getCurrentUser();

        return categoryRepository.findByUser(user)
                .stream()
                .map(c -> new OptionDTO(c.getId().toString(), c.getName()))
                .toList();
    }

    public Category getEntityById(UUID id) {
        AppUser user = securityUtils.getCurrentUser();
        return categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

}
