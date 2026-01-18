package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.dto.AccountDTO;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

    Page<Account> findByUser(AppUser user, Pageable pageable);

    List<Account> findByUser(AppUser user);

    Optional<Account> findByIdAndUser(UUID uuid, AppUser user);

    Page<Account> findAllByUser(AppUser user, Pageable pageRequest);

    List<AccountDTO> findAllByUser(AppUser user);

    @Query("""
    select count(t)
    from Transaction t
    where t.account.id = :accountId
      and t.account.user = :user
""")
    long countTransactionsByAccountAndUser(
            @Param("accountId") UUID accountId,
            @Param("user") AppUser user
    );

    Page<Account> searchByUser(AppUser user, Specification<Account> specification, Pageable pageRequest);
}
