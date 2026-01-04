package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.dto.MonthlyTransactionReportDTO;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByUser(AppUser user);

    List<Transaction> findByUserAndType(AppUser user, TransactionType type);

    Optional<Transaction> findByIdAndUser(UUID id, AppUser user);

    List<Transaction> findByUserAndDateBetween(AppUser user, LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT new com.veritech.BudgetKing.dto.MonthlyTransactionReportDTO(
                COALESCE(SUM(
                    CASE 
                        WHEN t.type = com.veritech.BudgetKing.enumerator.TransactionType.INCOME 
                        THEN t.amount 
                        ELSE CAST(0 AS bigdecimal)
                    END
                ), CAST(0 AS bigdecimal)),
                COALESCE(SUM(
                    CASE 
                        WHEN t.type = com.veritech.BudgetKing.enumerator.TransactionType.EXPENSE 
                        THEN t.amount 
                        ELSE CAST(0 AS bigdecimal)
                    END
                ), CAST(0 AS bigdecimal))
            )
            FROM Transaction t
            WHERE t.user = :user
              AND t.date >= :start
              AND t.date < :end
            """)
    MonthlyTransactionReportDTO getMonthlyReport(
            @Param("user") AppUser user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
