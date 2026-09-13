package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.dto.IncomeExpenseDTO;
import com.veritech.BudgetKing.dto.MonthlyIncomeExpenseDTO;
import com.veritech.BudgetKing.dto.MonthlyTransactionReportDTO;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.SavingsGoal;
import com.veritech.BudgetKing.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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


    @Query("""
                SELECT new com.veritech.BudgetKing.dto.IncomeExpenseDTO(
                    COALESCE(SUM(
                        CASE WHEN t.type = com.veritech.BudgetKing.enumerator.TransactionType.INCOME
                             THEN t.amount
                             ELSE CAST(0 AS bigdecimal)
                        END
                    ), CAST(0 AS bigdecimal)),
                    COALESCE(SUM(
                        CASE WHEN t.type = com.veritech.BudgetKing.enumerator.TransactionType.EXPENSE
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
    IncomeExpenseDTO getIncomeAndExpense(
            @Param("user") AppUser user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );



    @Query("""
                SELECT c.name, COALESCE(SUM(t.amount), 0)
                FROM Transaction t
                JOIN t.category c
                WHERE t.user = :user
                  AND t.type = com.veritech.BudgetKing.enumerator.TransactionType.EXPENSE
                  AND t.date >= :start
                  AND t.date < :end
                GROUP BY c.name
            """)
    List<Object[]> getExpensesByCategory(
            @Param("user") AppUser user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    SELECT c.name, c.icon, COALESCE(SUM(t.amount), 0) as total
    FROM Transaction t
    JOIN t.category c
    WHERE t.user = :user
      AND t.type = com.veritech.BudgetKing.enumerator.TransactionType.EXPENSE
      AND t.date >= :start
      AND t.date < :end
    GROUP BY c.name, c.icon
    ORDER BY total DESC
""")
    List<Object[]> getExpensesByCategoryWithIcon(
            @Param("user") AppUser user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );




    @Query("""
    SELECT new com.veritech.BudgetKing.dto.MonthlyIncomeExpenseDTO(
        MONTH(t.date),
        COALESCE(SUM(
            CASE 
                WHEN t.type = com.veritech.BudgetKing.enumerator.TransactionType.INCOME
                THEN t.amount
                ELSE 0
            END
        ), 0),
        COALESCE(SUM(
            CASE 
                WHEN t.type = com.veritech.BudgetKing.enumerator.TransactionType.EXPENSE
                THEN t.amount
                ELSE 0
            END
        ), 0)
    )
    FROM Transaction t
    WHERE t.user = :user
      AND YEAR(t.date) = :year
      AND (:accountId IS NULL OR t.account.id = :accountId)
    GROUP BY MONTH(t.date)
    ORDER BY MONTH(t.date)
""")
    List<MonthlyIncomeExpenseDTO> getIncomeExpenseByMonth(
            @Param("user") AppUser user,
            @Param("year") int year,
            @Param("accountId") UUID accountId
    );


    Page<Transaction> findAllByUser(AppUser user, Pageable pageRequest);

    /**
     * Used by the CSV import to flag rows that look like they already exist for the user,
     * so they are not imported twice. The match is a simple heuristic on the fields carried
     * by the CSV file (date, amount, description, type) - it does not consider account or
     * counterparty since those are not always comparable (account is stamped by the client
     * only at commit time).
     */
    boolean existsByUserAndDateAndAmountAndDescriptionAndType(
            AppUser user,
            LocalDateTime date,
            BigDecimal amount,
            String description,
            TransactionType type
    );


    /**
     * Detaches every contribution from {@code goal} so the goal row can be deleted
     * while the movements stay in the account history. Returns the rows touched.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Transaction t SET t.savingsGoal = null WHERE t.savingsGoal = :goal")
    int unlinkSavingsGoal(@Param("goal") SavingsGoal goal);
}
