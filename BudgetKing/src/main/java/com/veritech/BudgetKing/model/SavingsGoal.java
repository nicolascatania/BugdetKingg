package com.veritech.BudgetKing.model;

import com.veritech.BudgetKing.enumerator.SavingsGoalStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A forward-looking savings target owned by a single user that holds real money.
 *
 * <p>Money enters and leaves through {@link Transaction}s of type
 * {@code SAVINGS_DEPOSIT} / {@code SAVINGS_WITHDRAWAL}; {@link #currentAmount} is
 * the running total of those movements and is what every progress figure derives
 * from. Money inside a goal is no longer part of any account balance, so it is
 * excluded from the user's regular balance by construction.</p>
 *
 * <p>Only {@link #status} and the cached {@link #achieved} flag describe lifecycle;
 * whether a goal is overdue is derived at read time from {@link #targetDate}.</p>
 */
@Entity
@Table(name = "savings_goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SavingsGoal extends AuditedEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    private String icon;

    /** How much money the user wants to have saved when the goal completes. */
    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    /** The day the user wants to reach {@link #targetAmount} by. */
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    /**
     * Money currently set aside in this goal. Never negative; it can exceed
     * {@link #targetAmount} when the user over-saves. The DDL default covers rows
     * created before this column existed (Hibernate {@code ddl-auto=update}).
     */
    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "current_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    /** Persisted lifecycle; see {@link SavingsGoalStatus}. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ACTIVE'")
    @Column(nullable = false, length = 20)
    private SavingsGoalStatus status = SavingsGoalStatus.ACTIVE;

    /**
     * Optional default source account, preselected when the user contributes to
     * the goal. It has no effect on progress.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "linked_account_id")
    private Account linkedAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** Cached {@code currentAmount >= targetAmount}, refreshed on every write. */
    @Column(nullable = false)
    private boolean achieved;

    /** Convenience for guards: whether the goal still accepts money and edits. */
    public boolean isActive() {
        return status == SavingsGoalStatus.ACTIVE;
    }
}
