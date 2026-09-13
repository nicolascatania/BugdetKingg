package com.veritech.BudgetKing.mapper;

import com.veritech.BudgetKing.dto.RecurringTransactionDTO;
import com.veritech.BudgetKing.dto.RecurringTransactionRelatedEntities;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.enumerator.RecurrenceFrequency;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.model.RecurringTransaction;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Hand-written mapper between {@link RecurringTransaction} and its DTO.
 *
 * <p>It also knows how to turn a template into the {@link TransactionDTO} handed to
 * {@code TransactionService.create(...)}, which keeps that translation in one place
 * instead of scattered through the recurrence engine.</p>
 */
@Component
public class RecurringTransactionMapper
        implements ICrudMapper<RecurringTransaction, RecurringTransactionDTO, RecurringTransactionRelatedEntities> {

    @Override
    public RecurringTransactionDTO toDto(RecurringTransaction entity) {
        return new RecurringTransactionDTO(
                entity.getId(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getType().name(),
                entity.getCounterparty(),
                entity.getCategory() != null ? entity.getCategory().getId() : null,
                entity.getCategory() != null ? entity.getCategory().getName() : null,
                entity.getAccount().getId(),
                entity.getAccount().getName(),
                entity.getDestinationAccount() != null ? entity.getDestinationAccount().getId() : null,
                entity.getFrequency().name(),
                entity.getNextRunDate(),
                entity.getEndDate(),
                entity.isActive()
        );
    }

    @Override
    public RecurringTransaction toEntity(RecurringTransactionDTO dto, RecurringTransactionRelatedEntities r) {
        return RecurringTransaction.builder()
                .id(dto.id())
                .description(dto.description())
                .amount(dto.amount())
                .type(TransactionType.fromString(dto.type()))
                .counterparty(dto.counterparty())
                .category(r.category())
                .account(r.account())
                .destinationAccount(r.destinationAccount())
                .frequency(RecurrenceFrequency.fromString(dto.frequency()))
                .nextRunDate(dto.nextRunDate())
                .endDate(dto.endDate())
                .active(dto.active())
                .user(r.user())
                .build();
    }

    /**
     * Builds the payload of the transaction a template produces for one occurrence.
     *
     * <p>The date is serialised in ISO form because {@code TransactionMapper} parses it
     * back with {@link LocalDateTime#parse(CharSequence)}. Occurrences are stamped at the
     * start of their day so a same-day manual entry never sorts above them by accident.</p>
     *
     * @param template  the recurring template being fired
     * @param occurrence the date this occurrence belongs to
     * @return DTO ready for {@code TransactionService.create(...)}
     */
    public TransactionDTO toTransactionDto(RecurringTransaction template, LocalDate occurrence) {
        return new TransactionDTO(
                null,
                LocalDateTime.of(occurrence, LocalTime.MIDNIGHT).toString(),
                template.getAmount(),
                template.getType().name(),
                template.getCounterparty(),
                template.getDescription(),
                template.getCategory() != null ? template.getCategory().getId() : null,
                template.getCategory() != null ? template.getCategory().getName() : null,
                template.getAccount().getId(),
                template.getDestinationAccount() != null ? template.getDestinationAccount().getId() : null,
                template.getAccount().getName(),
                null,
                null
        );
    }

    /**
     * Projects a template as one of its future occurrences.
     *
     * <p>Used by the "upcoming" endpoint: the returned DTO is the template itself with
     * {@code nextRunDate} replaced by the date of that particular occurrence.</p>
     *
     * @param template   the recurring template
     * @param occurrence the date of the projected occurrence
     * @return DTO describing a single upcoming occurrence
     */
    public RecurringTransactionDTO toOccurrenceDto(RecurringTransaction template, LocalDate occurrence) {
        RecurringTransactionDTO dto = toDto(template);
        return new RecurringTransactionDTO(
                dto.id(),
                dto.description(),
                dto.amount(),
                dto.type(),
                dto.counterparty(),
                dto.category(),
                dto.categoryName(),
                dto.account(),
                dto.accountName(),
                dto.destinationAccount(),
                dto.frequency(),
                occurrence,
                dto.endDate(),
                dto.active()
        );
    }
}
