package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.dto.AccountDTO;
import com.veritech.BudgetKing.filter.AccountFilter;
import com.veritech.BudgetKing.interfaces.ICrudController;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("account")
@RequiredArgsConstructor
public class AccountController implements ICrudController<AccountDTO, UUID, AccountFilter> {

    private final AccountService accountService;

    @Override
    public ICrudService<AccountDTO, UUID, AccountFilter> getService() {
        return accountService;
    }

    @GetMapping("/by-user")
    public ResponseEntity<List<AccountDTO>> getByUser() {
        return ResponseEntity.ok(this.accountService.getByUser());
    }
}
