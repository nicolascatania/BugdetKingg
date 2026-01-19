package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.AppUserDTO;
import com.veritech.BudgetKing.dto.OptionDTO;
import com.veritech.BudgetKing.filter.AppUserFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.AppUserMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


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
    public Page<AppUserDTO> search(AppUserFilter filter) {
        Page<AppUser> users;

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());

        if (filter == null) {
            users = repository.findAll(pageable);
        } else {
            users = repository.findAll(filter.toSpecification(), pageable);
        }
        return users.map(mapper::toDto);
    }

    @Override
    public List<OptionDTO> getOptions() {
        return repository.findAll()
                .stream()
                .map(u -> new OptionDTO(u.getId().toString(), u.getName()))
                .toList();
    }

    public AppUser getEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}