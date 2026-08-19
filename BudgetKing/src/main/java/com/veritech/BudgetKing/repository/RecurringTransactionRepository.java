package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.RecurringTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecurringTransactionRepository
        extends JpaRepository<RecurringTransaction, UUID>, JpaSpecificationExecutor<RecurringTransaction> {

    List<RecurringTransaction> findByUser(AppUser user);

    Page<RecurringTransaction> findAllByUser(AppUser user, Pageable pageable);

    Optional<RecurringTransaction> findByIdAndUser(UUID id, AppUser user);

    List<RecurringTransaction> findByUserAndActive(AppUser user, boolean active);

    /**
     * Templates the engine has to fire: still running, already due, and not past their
     * end date. Ordering by owner keeps every batch grouped per user, which is what the
     * engine needs to swap the security context only when the owner actually changes.
     *
     * @param today the date the run is executed for
     * @return due templates across every user
     */
    @Query("""
            SELECT r
            FROM RecurringTransaction r
            WHERE r.active = true
              AND r.nextRunDate <= :today
              AND (r.endDate IS NULL OR r.endDate >= :today)
            ORDER BY r.user.id ASC, r.nextRunDate ASC
            """)
    List<RecurringTransaction> findDue(@Param("today") LocalDate today);

    /**
     * Active templates of one user whose next occurrence falls inside a window.
     *
     * @param user  owner of the templates
     * @param from  inclusive lower bound of the window
     * @param to    inclusive upper bound of the window
     * @return templates that will fire at least once inside the window
     */
    @Query("""
            SELECT r
            FROM RecurringTransaction r
            WHERE r.user = :user
              AND r.active = true
              AND r.nextRunDate <= :to
              AND (r.endDate IS NULL OR r.endDate >= :from)
            ORDER BY r.nextRunDate ASC
            """)
    List<RecurringTransaction> findActiveInWindow(@Param("user") AppUser user,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);
}
