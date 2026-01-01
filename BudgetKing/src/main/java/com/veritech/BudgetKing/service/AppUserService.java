package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.filter.AppUserFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppUserService
        implements ICrudService<AppUser, UUID, AppUserFilter> {

    private final AppUserRepository repository;

    @Override
    public AppUser getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public AppUser create(AppUser entity) {
        return repository.save(entity);
    }

    @Override
    public AppUser update(UUID id, AppUser entity) {
        AppUser existing = getById(id);
        existing.setEmail(entity.getEmail());
        existing.setEnabled(entity.isEnabled());
        return repository.save(existing);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public List<AppUser> search(AppUserFilter filter) {
        if (filter == null) {
            return repository.findAll();
        }
        return repository.findAll(filter.toSpecification());
    }

}
