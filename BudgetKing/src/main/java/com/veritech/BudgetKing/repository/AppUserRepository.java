package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID>, JpaSpecificationExecutor<AppUser> {
    Optional<AppUser> findByEmail(String email);
}
