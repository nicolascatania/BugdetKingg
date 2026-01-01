package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUser(AppUser user);

    List<Transaction> findByUserAndType(AppUser user, TransactionType type);
}
