package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.dto.OptionDTO;
import com.veritech.BudgetKing.filter.CategoryFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICrudService<CategoryDTO, UUID, CategoryFilter> {

    private final CategoryRepository categoryRepository;
    private final SecurityUtils securityUtils;

    @Override
    public CategoryDTO getById(UUID uuid) {
        return null;
    }

    @Override
    public CategoryDTO create(CategoryDTO dto) {
        return null;
    }

    @Override
    public CategoryDTO update(UUID uuid, CategoryDTO dto) {
        return null;
    }

    @Override
    public void deleteById(UUID uuid) {

    }

    @Override
    public List<CategoryDTO> search(CategoryFilter categoryFilter) {
        return List.of();
    }

    public List<OptionDTO> getOptions() {
        AppUser user = securityUtils.getCurrentUser();

        return categoryRepository.findByUser(user)
                .stream()
                .map(c -> new OptionDTO(c.getId().toString(), c.getName()))
                .toList();
    }
}
