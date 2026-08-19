package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.OptionDTO;
import com.veritech.BudgetKing.dto.RecurringTransactionDTO;
import com.veritech.BudgetKing.dto.RecurringTransactionRelatedEntities;
import com.veritech.BudgetKing.enumerator.RecurrenceFrequency;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.exception.RecurringTransactionRuntimeException;
import com.veritech.BudgetKing.filter.RecurringTransactionFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.RecurringTransactionMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.RecurringTransaction;
import com.veritech.BudgetKing.repository.RecurringTransactionRepository;
import com.veritech.BudgetKing.security.UserDetailsImpl;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * CRUD for recurring transaction templates plus the engine that turns them into real
 * transactions.
 *
 * <p>The engine never touches account balances itself: every occurrence is delegated to
 * {@link TransactionService#create(com.veritech.BudgetKing.dto.TransactionDTO)}, the one
 * path that already knows how to move money. {@code nextRunDate} is only moved forward
 * <em>after</em> a successful create, which is what makes a second run on the same day a
 * no-op instead of a duplicate.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionService
        implements ICrudService<RecurringTransactionDTO, UUID, RecurringTransactionFilter> {

    /**
     * Ceiling on how many occurrences a single template may generate in one run.
     * A template that fell further behind than this is simply picked up again by the
     * next run, so nothing is lost and no run can ever grow unbounded.
     */
    static final int MAX_CATCH_UP_OCCURRENCES = 60;

    /**
     * Hard cap on the date-advance loop. It exists so a frequency that somehow failed to
     * move a date forward can never spin forever.
     */
    static final int MAX_ADVANCE_ITERATIONS = 512;

    /** How far ahead the "upcoming" projection looks. */
    static final int UPCOMING_HORIZON_DAYS = 30;

    /** Ceiling on how many occurrences a single template contributes to the projection. */
    static final int MAX_UPCOMING_OCCURRENCES = 60;

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final RecurringTransactionMapper mapper;
    private final SecurityUtils securityUtils;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    /* ----------------------------------- CRUD ---------------------------------------- */

    @Override
    public RecurringTransactionDTO getById(UUID id) {
        return mapper.toDto(getEntityById(id));
    }

    /**
     * Stores a new template. Nothing is generated here — the first occurrence is produced
     * by the engine once {@code nextRunDate} arrives, or immediately via "run now".
     *
     * @param dto template to store
     * @return the stored template
     */
    @Override
    @Transactional
    public RecurringTransactionDTO create(RecurringTransactionDTO dto) {
        AppUser user = securityUtils.getCurrentUser();

        Account sourceAccount = accountService.getEntityById(dto.account());
        Account destinationAccount = resolveDestinationAccount(dto);
        Category category = resolveCategory(dto);

        validateTemplate(dto, sourceAccount, destinationAccount);

        RecurringTransaction template = mapper.toEntity(
                dto,
                new RecurringTransactionRelatedEntities(user, sourceAccount, destinationAccount, category)
        );
        template.setId(null); // creation never reuses a client supplied identifier

        return mapper.toDto(recurringTransactionRepository.save(template));
    }

    /**
     * Replaces the editable part of an existing template. The owner is never reassigned.
     *
     * @param id  identifier of the template to update
     * @param dto new values
     * @return the updated template
     */
    @Override
    @Transactional
    public RecurringTransactionDTO update(UUID id, RecurringTransactionDTO dto) {
        RecurringTransaction existing = getEntityById(id);

        Account sourceAccount = accountService.getEntityById(dto.account());
        Account destinationAccount = resolveDestinationAccount(dto);
        Category category = resolveCategory(dto);

        validateTemplate(dto, sourceAccount, destinationAccount);

        existing.setDescription(dto.description());
        existing.setAmount(dto.amount());
        existing.setType(TransactionType.fromString(dto.type()));
        existing.setCounterparty(dto.counterparty());
        existing.setCategory(category);
        existing.setAccount(sourceAccount);
        existing.setDestinationAccount(destinationAccount);
        existing.setFrequency(RecurrenceFrequency.fromString(dto.frequency()));
        existing.setNextRunDate(dto.nextRunDate());
        existing.setEndDate(dto.endDate());
        existing.setActive(dto.active());

        return mapper.toDto(recurringTransactionRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        recurringTransactionRepository.delete(getEntityById(id));
    }

    @Override
    public Page<RecurringTransactionDTO> search(RecurringTransactionFilter filter) {
        AppUser user = securityUtils.getCurrentUser();

        Pageable pageRequest = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.ASC, "nextRunDate")
        );

        Page<RecurringTransaction> templates =
                recurringTransactionRepository.findAll(filter.toSpecification(user), pageRequest);

        return templates.map(mapper::toDto);
    }

    @Override
    public List<OptionDTO> getOptions() {
        AppUser user = securityUtils.getCurrentUser();
        return recurringTransactionRepository.findByUser(user)
                .stream()
                .map(t -> new OptionDTO(t.getId().toString(), t.getDescription()))
                .toList();
    }

    /**
     * Loads a template owned by the authenticated user.
     *
     * @param id identifier of the template
     * @return the entity
     * @throws EntityNotFoundException when it does not exist or belongs to somebody else
     */
    public RecurringTransaction getEntityById(UUID id) {
        AppUser user = securityUtils.getCurrentUser();
        return recurringTransactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Recurring transaction not found"));
    }

    /* ---------------------------------- engine --------------------------------------- */

    /**
     * Fires every template that is due today.
     *
     * <p>A template qualifies when it is active, its {@code nextRunDate} is not in the
     * future and its {@code endDate} has not passed. Each occurrence is created through
     * {@link TransactionService#create}; the cursor advances only after that succeeds, and
     * a template whose schedule is exhausted is deactivated.</p>
     *
     * <p>Templates are processed independently: a failing one is logged and skipped so it
     * cannot block the rest of the batch.</p>
     *
     * @return how many transactions were generated
     */
    public int runDue() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> dueTemplates = recurringTransactionRepository.findDue(today);

        int created = 0;
        for (RecurringTransaction template : dueTemplates) {
            try {
                created += runAsOwner(template, today);
            } catch (RuntimeException ex) {
                log.error("Recurring transaction {} could not be processed: {}",
                        template.getId(), ex.getMessage(), ex);
            }
        }

        if (created > 0) {
            log.info("Recurrence engine generated {} transaction(s) from {} due template(s)",
                    created, dueTemplates.size());
        }
        return created;
    }

    /**
     * Generates one occurrence of a template right away, whether or not it is due yet.
     *
     * <p>The occurrence is dated today and the cursor is pushed strictly past today, so a
     * scheduled run later the same day cannot produce a duplicate.</p>
     *
     * @param id identifier of the template to fire
     * @return the template with its cursor already advanced
     * @throws RecurringTransactionRuntimeException when the template is paused or expired
     */
    @Transactional
    public RecurringTransactionDTO runNow(UUID id) {
        RecurringTransaction template = getEntityById(id);
        LocalDate today = LocalDate.now();

        if (!template.isActive()) {
            throw new RecurringTransactionRuntimeException(
                    "This recurring transaction is paused, activate it before running it.");
        }
        if (template.getEndDate() != null && template.getEndDate().isBefore(today)) {
            throw new RecurringTransactionRuntimeException(
                    "This recurring transaction already reached its end date.");
        }

        transactionService.create(mapper.toTransactionDto(template, today));

        template.setNextRunDate(advanceBeyond(template.getFrequency(), template.getNextRunDate(), today));
        deactivateIfExhausted(template);

        return mapper.toDto(recurringTransactionRepository.save(template));
    }

    /**
     * Projects the occurrences the authenticated user can expect over the next 30 days.
     *
     * <p>Read-only: no template is modified and no transaction is generated. Each element
     * is the template DTO with {@code nextRunDate} set to that particular occurrence.</p>
     *
     * @return upcoming occurrences ordered by date
     */
    public List<RecurringTransactionDTO> getUpcoming() {
        AppUser user = securityUtils.getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(UPCOMING_HORIZON_DAYS);

        return recurringTransactionRepository.findActiveInWindow(user, today, horizon)
                .stream()
                .flatMap(template -> occurrencesWithin(template, today, horizon)
                        .stream()
                        .map(date -> mapper.toOccurrenceDto(template, date)))
                .sorted(Comparator.comparing(RecurringTransactionDTO::nextRunDate))
                .toList();
    }

    /* ------------------------------- engine internals -------------------------------- */

    /**
     * Runs one template under its owner's security context.
     *
     * <p>The engine is triggered by the scheduler, where no user is authenticated, yet
     * every collaborator down the chain ({@code SecurityUtils}, the account lookup, the
     * auditing callbacks) is user-scoped. The previous context is always restored.</p>
     *
     * @param template template to fire
     * @param today    date the run is executed for
     * @return how many transactions this template generated
     */
    private int runAsOwner(RecurringTransaction template, LocalDate today) {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(contextFor(template.getUser()));
            int created = fireDueOccurrences(template, today);
            recurringTransactionRepository.save(template);
            return created;
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
    }

    /**
     * Builds a security context impersonating the owner of a template.
     *
     * <p>No authority is granted: the engine only needs the principal so the user-scoped
     * lookups and the audit columns resolve correctly.</p>
     */
    private SecurityContext contextFor(AppUser owner) {
        UserDetailsImpl principal = new UserDetailsImpl(owner);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        return context;
    }

    /**
     * Creates every occurrence a template still owes up to {@code today}.
     *
     * <p>The loop catches up period by period so a template that fell several periods
     * behind does not silently lose occurrences, and stops at
     * {@link #MAX_CATCH_UP_OCCURRENCES} so a corrupt date can never produce an unbounded
     * run. The cursor is advanced only after the create succeeded — if it throws, the
     * cursor stays put and the occurrence is retried on the next run.</p>
     *
     * @param template template to fire, mutated in place
     * @param today    date the run is executed for
     * @return how many transactions were generated
     */
    int fireDueOccurrences(RecurringTransaction template, LocalDate today) {
        int created = 0;

        while (created < MAX_CATCH_UP_OCCURRENCES) {
            LocalDate occurrence = template.getNextRunDate();

            boolean paused = !template.isActive();
            boolean notDueYet = occurrence == null || occurrence.isAfter(today);
            boolean pastEnd = template.getEndDate() != null && occurrence != null
                    && occurrence.isAfter(template.getEndDate());

            if (paused || notDueYet || pastEnd) {
                break;
            }

            transactionService.create(mapper.toTransactionDto(template, occurrence));
            created++;

            template.setNextRunDate(nextAfter(template.getFrequency(), occurrence));
        }

        deactivateIfExhausted(template);
        return created;
    }

    /**
     * Turns a template off once its cursor moved past the end of the series, so the engine
     * stops considering it on every subsequent run.
     */
    private void deactivateIfExhausted(RecurringTransaction template) {
        if (template.getEndDate() != null
                && template.getNextRunDate() != null
                && template.getNextRunDate().isAfter(template.getEndDate())) {
            template.setActive(false);
        }
    }

    /**
     * Moves a cursor forward until it lands strictly after {@code cutOff}.
     *
     * <p>Bounded by {@link #MAX_ADVANCE_ITERATIONS}; a template left behind for longer than
     * that is re-anchored on the cut-off date so the cursor can never stall in the past.</p>
     */
    private LocalDate advanceBeyond(RecurrenceFrequency frequency, LocalDate cursor, LocalDate cutOff) {
        LocalDate result = cursor;
        for (int i = 0; i < MAX_ADVANCE_ITERATIONS && !result.isAfter(cutOff); i++) {
            result = nextAfter(frequency, result);
        }
        return result.isAfter(cutOff) ? result : nextAfter(frequency, cutOff);
    }

    /**
     * One step of the recurrence, guarding the invariant the loops rely on: a period must
     * always move the date strictly forward.
     */
    private LocalDate nextAfter(RecurrenceFrequency frequency, LocalDate from) {
        LocalDate next = frequency.advance(from);
        if (next == null || !next.isAfter(from)) {
            throw new RecurringTransactionRuntimeException(
                    "Frequency " + frequency + " did not move the schedule forward from " + from);
        }
        return next;
    }

    /**
     * Lists the dates a template will fire on inside a window, without mutating it.
     *
     * <p>An overdue template is projected from the start of the window because the engine
     * will pick its pending occurrence up on its very next run.</p>
     */
    private List<LocalDate> occurrencesWithin(RecurringTransaction template, LocalDate from, LocalDate to) {
        List<LocalDate> occurrences = new ArrayList<>();

        LocalDate last = (template.getEndDate() != null && template.getEndDate().isBefore(to))
                ? template.getEndDate()
                : to;

        LocalDate cursor = template.getNextRunDate();
        if (cursor == null) {
            return occurrences;
        }
        if (cursor.isBefore(from)) {
            cursor = from;
        }

        for (int i = 0; i < MAX_UPCOMING_OCCURRENCES && !cursor.isAfter(last); i++) {
            occurrences.add(cursor);
            cursor = nextAfter(template.getFrequency(), cursor);
        }

        return occurrences;
    }

    /* -------------------------------- validation ------------------------------------- */

    /**
     * Applies the rules a template must satisfy on top of the bean validation already run
     * on the DTO, i.e. the ones that need the resolved entities.
     */
    private void validateTemplate(RecurringTransactionDTO dto, Account source, Account destination) {
        TransactionType type = TransactionType.fromString(dto.type());

        if (type == TransactionType.TRANSFER) {
            if (destination == null) {
                throw new IllegalArgumentException("Destination account is required for TRANSFER");
            }
            if (source.getId().equals(destination.getId())) {
                throw new IllegalArgumentException("Source and destination accounts must be different");
            }
        }

        if (dto.amount() == null || dto.amount().signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (dto.endDate() != null && dto.endDate().isBefore(dto.nextRunDate())) {
            throw new IllegalArgumentException("End date cannot be earlier than the next run date");
        }
    }

    private Account resolveDestinationAccount(RecurringTransactionDTO dto) {
        if (dto.destinationAccount() == null) {
            return null;
        }
        return accountService.getEntityById(dto.destinationAccount());
    }

    private Category resolveCategory(RecurringTransactionDTO dto) {
        if (dto.category() == null) {
            return null;
        }
        return categoryService.getEntityById(dto.category());
    }
}
