package com.veritech.BudgetKing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Monthly spending limit the user sets for a single category.
 *
 * <p>A budget is always scoped to one user, one category and one calendar
 * period (year + month). The unique constraint enforces that pairing at the
 * database level, so a category can never hold two limits for the same month.</p>
 */
@Entity
@Table(
        name = "budgets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category_id", "year", "month"})
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class Budget extends AuditedEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    // Eagerly fetched: every budget is rendered together with its category name and icon.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Many budgets belong to one user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** Calendar year the limit applies to. */
    @Column(name = "year", nullable = false)
    private int year;

    /** Calendar month the limit applies to, 1 (January) through 12 (December). */
    @Column(name = "month", nullable = false)
    private int month;

    /** Maximum amount the user intends to spend on this category during the period. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;
}
