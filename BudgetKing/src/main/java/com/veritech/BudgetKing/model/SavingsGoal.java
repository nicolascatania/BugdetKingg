package com.veritech.BudgetKing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A forward-looking savings target owned by a single user.
 *
 * <p>Progress is never stored: it is derived at read time from the balance of the
 * optional {@link Account} the goal is linked to, so the figures shown to the user
 * always reflect the current state of that account. Only the {@code achieved} flag
 * is persisted, and it is refreshed on every write so reports and filters can rely
 * on it without recomputing balances.</p>
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
     * Account whose balance funds this goal. Optional: a goal may be tracked
     * without linking it to a concrete account, in which case progress is zero.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "linked_account_id")
    private Account linkedAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** Cached completion flag, refreshed by the service on every create/update. */
    @Column(nullable = false)
    private boolean achieved;
}
