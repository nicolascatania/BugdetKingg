package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    List<Category> findByUser(AppUser user);

    Optional<Category> findByIdAndUser(UUID uuid, AppUser user);

    Optional<Category> getByNameAndUser(String aDefault, AppUser user);

    long countByUser(AppUser user);

    @Query("""
    select count(t)
    from Transaction t
    where t.category.id = :categoryId
      and t.category.user = :user
""")
    long countTransactionsByCategory(@Param("categoryId") UUID categoryId,
                                     @Param("user") AppUser user);

}
