package com.veritech.BudgetKing.mapper;

import com.veritech.BudgetKing.dto.AppUserDTO;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Role;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AppUserMapper implements ICrudMapper<AppUser, AppUserDTO, Void> {

    @Override
    public AppUserDTO toDto(AppUser entity) {
        return new AppUserDTO(
                entity.getId(),
                entity.getEmail(),
                null,
                entity.getName(),
                entity.getLastName(),
                entity.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );
    }

    @Override
    public AppUser toEntity(AppUserDTO dto, Void unused) {
        AppUser user = new AppUser();
        user.setEmail(dto.email());
        user.setName(dto.name());
        user.setLastName(dto.lastName());
        return user;
    }
}