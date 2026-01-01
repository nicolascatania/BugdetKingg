package com.veritech.BudgetKing.mapper;

import com.veritech.BudgetKing.dto.AppUserDTO;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Role;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AppUserMapper implements ICrudMapper<AppUser, AppUserDTO> {

    @Override
    public AppUserDTO toDto(AppUser entity) {
        return new AppUserDTO(
                entity.getId(),
                entity.getEmail(),
                null, // nunca devolvemos password
                entity.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );
    }

    @Override
    public AppUser toEntity(AppUserDTO dto) {
        AppUser user = new AppUser();
        user.setEmail(dto.email());
        // password se setea en el service (bcrypt)
        return user;
    }

    @Override
    public void updateEntity(AppUser entity, AppUserDTO dto) {
        if (dto.email() != null) {
            entity.setEmail(dto.email());
        }
        // password también se maneja en el service
    }
}
