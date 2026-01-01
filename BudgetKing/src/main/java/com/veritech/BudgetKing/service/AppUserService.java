package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.AppUserDTO;
import com.veritech.BudgetKing.filter.AppUserFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.AppUserMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppUserService implements ICrudService<AppUserDTO, UUID, AppUserFilter> {

    private final AppUserRepository repository;
    private final AppUserMapper mapper;

    @Override
    public AppUserDTO getById(UUID id) {
        AppUser user = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return mapper.toDto(user);
    }

    @Override
    public AppUserDTO create(AppUserDTO dto) {
        AppUser user = mapper.toEntity(dto, null);
        AppUser saved = repository.save(user);
        return mapper.toDto(saved);
    }

    @Override
    public AppUserDTO update(UUID id, AppUserDTO dto) {
        AppUser existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        AppUser updated = mapper.toEntity(dto, null);
        existing.setEmail(updated.getEmail());
        existing.setName(updated.getName());
        existing.setLastName(updated.getLastName());
        return mapper.toDto(repository.save(existing));
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public List<AppUserDTO> search(AppUserFilter filter) {
        List<AppUser> users;
        if (filter == null) {
            users = repository.findAll();
        } else {
            users = repository.findAll(filter.toSpecification());
        }
        return users.stream().map(mapper::toDto).collect(Collectors.toList());
    }

    public AppUser getEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}