package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByUser(AppUser user);
}
