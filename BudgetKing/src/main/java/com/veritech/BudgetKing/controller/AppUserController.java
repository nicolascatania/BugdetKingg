package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.AppUserDTO;
import com.veritech.BudgetKing.filter.AppUserFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.service.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Role;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AppUserController implements
        ICrudController<AppUserDTO, UUID, AppUserFilter> {

    private final AppUserService service;

    @Override
    public ICrudService<AppUserDTO, UUID, AppUserFilter> getService() {
        return service;
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AppUserDTO>> getListForWebsite(@RequestParam int page, @RequestParam int size) {
        Page<AppUserDTO> result = service.getListForWebsite(page, size);
        return ResponseEntity.ok(result);
    }

}
