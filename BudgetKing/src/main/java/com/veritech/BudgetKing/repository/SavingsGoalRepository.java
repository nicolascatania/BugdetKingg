package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.SavingsGoal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID>, JpaSpecificationExecutor<SavingsGoal> {

    /** User-scoped lookup: never resolve a goal by id alone. */
    Optional<SavingsGoal> findByIdAndUser(UUID id, AppUser user);

    List<SavingsGoal> findByUser(AppUser user);

    Page<SavingsGoal> findByUser(AppUser user, Pageable pageable);
}
