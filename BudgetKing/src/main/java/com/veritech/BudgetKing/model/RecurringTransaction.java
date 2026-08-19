package com.veritech.BudgetKing.model;

import com.veritech.BudgetKing.enumerator.RecurrenceFrequency;
import com.veritech.BudgetKing.enumerator.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Template that materialises a {@link Transaction} on a fixed cadence.
 *
 * <p>The first block of fields mirrors the transaction that will be produced
 * (description, amount, type, accounts, category); the second block describes the
 * schedule itself: how often it repeats, when the next occurrence is due, when the
 * series ends and whether it is still running.</p>
 */
@Entity
@Table(name = "recurring_transactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class RecurringTransaction extends AuditedEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    /* ----------------------------- transaction template ----------------------------- */

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** Who gets paid / who pays, copied verbatim onto every generated transaction. */
    private String counterparty;

    /** Optional: TRANSFER templates carry no category. */
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** Only used by TRANSFER templates. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    /* --------------------------------- schedule ------------------------------------- */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    /**
     * Date of the next occurrence still to be generated. The engine treats it as the
     * cursor of the series: it only ever moves forward, and only after a transaction
     * has actually been created, which is what makes a repeated run idempotent.
     */
    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    /** Inclusive last date the series may fire on. {@code null} means "runs forever". */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Paused templates are skipped by the engine without being deleted. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
}
