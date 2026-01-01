package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.AppUserDTO;
import com.veritech.BudgetKing.filter.AppUserFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.AppUserMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.service.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AppUserController implements
        ICrudController<AppUser, AppUserDTO, UUID, AppUserFilter, Void> {

    private final AppUserService service;
    private final AppUserMapper mapper;


    @Override
    public ICrudService<AppUser, UUID, AppUserFilter> getService() {
        return service;
    }

    @Override
    public ICrudMapper<AppUser, AppUserDTO, Void> getMapper() {
        return mapper;
    }
}
