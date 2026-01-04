package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.dto.AccountDTO;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {
    List<Account> findByUser(AppUser user);

    Optional<Account> findByIdAndUser(UUID uuid, AppUser user);

    List<AccountDTO> findAllByUser(AppUser user);
}
