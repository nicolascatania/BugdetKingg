package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByUser(AppUser user);

    List<Transaction> findByUserAndType(AppUser user, TransactionType type);

    Optional<Transaction> findByIdAndUser(UUID id, AppUser user);
}
