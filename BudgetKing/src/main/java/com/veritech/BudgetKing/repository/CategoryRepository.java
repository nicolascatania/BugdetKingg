package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    List<Category> findByUser(AppUser user);
}
