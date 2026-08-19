package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Budget;
import com.veritech.BudgetKing.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID>, JpaSpecificationExecutor<Budget> {

    /** User-scoped lookup by identifier, so one user can never read another's budget. */
    Optional<Budget> findByIdAndUser(UUID id, AppUser user);

    /** Every budget the user defined for a calendar period. */
    List<Budget> findByUserAndYearAndMonth(AppUser user, int year, int month);

    /** Guards the one-budget-per-category-and-period rule before hitting the unique constraint. */
    boolean existsByUserAndCategoryAndYearAndMonth(AppUser user, Category category, int year, int month);

    List<Budget> findByUser(AppUser user);

    Page<Budget> findByUser(AppUser user, Pageable pageable);
}
