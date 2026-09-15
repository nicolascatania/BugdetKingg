package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.OptionDTO;
import com.veritech.BudgetKing.dto.SavingsGoalCloseDTO;
import com.veritech.BudgetKing.dto.SavingsGoalContributionDTO;
import com.veritech.BudgetKing.dto.SavingsGoalDTO;
import com.veritech.BudgetKing.dto.SavingsGoalRelatedEntities;
import com.veritech.BudgetKing.dto.SavingsGoalSummaryDTO;
import com.veritech.BudgetKing.enumerator.SavingsGoalCloseOutcome;
import com.veritech.BudgetKing.enumerator.SavingsGoalStatus;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.exception.SavingsGoalRuntimeException;
import com.veritech.BudgetKing.filter.SavingsGoalFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.SavingsGoalMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.SavingsGoal;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.SavingsGoalRepository;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for {@link SavingsGoal}.
 *
 * <p>A goal holds real money: {@link SavingsGoal#getCurrentAmount()} is the running
 * total of the {@code SAVINGS_DEPOSIT} / {@code SAVINGS_WITHDRAWAL} transactions
 * made against it. This service owns the goal-level rules (ownership, status,
 * enough funds) and delegates the actual balance maths to
 * {@link TransactionService#applyBalanceChanges} so there is a single definition
 * of how each transaction type moves money.</p>
 *
 * <p>Nothing happens automatically when {@code targetDate} passes: the goal is
 * reported as {@code OVERDUE} (or {@code ACHIEVED}) and the user decides whether
 * to extend it, keep contributing or {@link #close close} it.</p>
 */
@Service
@RequiredArgsConstructor
public class SavingsGoalService implements ICrudService<SavingsGoalDTO, UUID, SavingsGoalFilter> {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;

    private final SavingsGoalRepository savingsGoalRepository;
    private final SavingsGoalMapper savingsGoalMapper;
    private final SecurityUtils securityUtils;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    @Override
    public SavingsGoalDTO getById(UUID uuid) {
        SavingsGoal found = getEntityById(uuid);
        return enrich(savingsGoalMapper.toDto(found), found);
    }

    @Override
    @Transactional
    public SavingsGoalDTO create(SavingsGoalDTO dto) {
        AppUser user = securityUtils.getCurrentUser();
        validateTargetAmount(dto);
        validateTargetDate(dto.targetDate());

        Account linkedAccount = resolveLinkedAccount(dto.linkedAccountId());
        SavingsGoalRelatedEntities relatedEntities = new SavingsGoalRelatedEntities(user, linkedAccount);

        SavingsGoal goal = savingsGoalMapper.toEntity(dto, relatedEntities);
        refreshAchieved(goal);

        SavingsGoal saved = savingsGoalRepository.save(goal);
        return enrich(savingsGoalMapper.toDto(saved), saved);
    }

    /**
     * Edits the descriptive side of a goal. The target date is only re-validated
     * when it actually changes, so an overdue goal can still be renamed without
     * being forced to pick a new date first.
     */
    @Override
    @Transactional
    public SavingsGoalDTO update(UUID uuid, SavingsGoalDTO dto) {
        SavingsGoal found = getEntityById(uuid);
        assertActive(found);

        validateTargetAmount(dto);
        if (!found.getTargetDate().equals(dto.targetDate())) {
            validateTargetDate(dto.targetDate());
        }

        found.setName(dto.name());
        found.setIcon(dto.icon());
        found.setTargetAmount(dto.targetAmount());
        found.setTargetDate(dto.targetDate());
        found.setLinkedAccount(resolveLinkedAccount(dto.linkedAccountId()));
        refreshAchieved(found);

        SavingsGoal saved = savingsGoalRepository.save(found);
        return enrich(savingsGoalMapper.toDto(saved), saved);
    }

    /**
     * Removes a goal that holds no money. Its past contributions stay in the
     * account history, merely detached from the goal.
     */
    @Override
    @Transactional
    public void deleteById(UUID uuid) {
        SavingsGoal found = getEntityById(uuid);

        if (found.getCurrentAmount().signum() > 0) {
            throw new SavingsGoalRuntimeException("Withdraw or close the goal before deleting it: it still holds money");
        }

        transactionRepository.unlinkSavingsGoal(found);
        savingsGoalRepository.delete(found);
    }

    @Override
    public Page<SavingsGoalDTO> search(SavingsGoalFilter filter) {
        AppUser user = securityUtils.getCurrentUser();

        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by("targetDate").ascending()
        );

        Page<SavingsGoal> page = savingsGoalRepository.findAll(filter.toSpecification(user), pageable);
        return page.map(entity -> enrich(savingsGoalMapper.toDto(entity), entity));
    }

    @Override
    public List<OptionDTO> getOptions() {
        AppUser user = securityUtils.getCurrentUser();

        return savingsGoalRepository.findByUser(user)
                .stream()
                .map(goal -> new OptionDTO(goal.getId().toString(), goal.getName()))
                .toList();
    }

    /**
     * Moves money from one of the user's accounts into the goal.
     *
     * @throws SavingsGoalRuntimeException when the goal is closed, the amount is not
     *                                     positive or the account cannot cover it
     */
    @Transactional
    public SavingsGoalDTO deposit(UUID goalId, SavingsGoalContributionDTO dto) {
        SavingsGoal goal = getEntityById(goalId);
        Account account = accountService.getEntityById(dto.accountId());

        assertActive(goal);
        validateContributionAmount(dto.amount());
        if (account.getBalance().compareTo(dto.amount()) < 0) {
            throw new SavingsGoalRuntimeException("Insufficient funds in account " + account.getName());
        }

        return contribute(goal, account, TransactionType.SAVINGS_DEPOSIT, dto.amount(), dto.date(), dto.note());
    }

    /**
     * Moves money from the goal back into one of the user's accounts.
     *
     * @throws SavingsGoalRuntimeException when the goal is closed, the amount is not
     *                                     positive or the goal does not hold that much
     */
    @Transactional
    public SavingsGoalDTO withdraw(UUID goalId, SavingsGoalContributionDTO dto) {
        SavingsGoal goal = getEntityById(goalId);
        Account account = accountService.getEntityById(dto.accountId());

        assertActive(goal);
        validateContributionAmount(dto.amount());
        if (goal.getCurrentAmount().compareTo(dto.amount()) < 0) {
            throw new SavingsGoalRuntimeException("The goal does not hold enough money for this withdrawal");
        }

        return contribute(goal, account, TransactionType.SAVINGS_WITHDRAWAL, dto.amount(), dto.date(), dto.note());
    }

    /**
     * Ends the goal and makes it read-only. What happens to the money it still
     * holds depends on {@link SavingsGoalCloseDTO#outcome()}:
     * <ul>
     *   <li>{@code RETURN} — the balance goes back to the chosen account.</li>
     *   <li>{@code SPEND} — the balance goes back to the chosen account and an
     *       {@code EXPENSE} of the same amount is recorded against it, so the
     *       account nets to zero and the purchase appears in the expense history
     *       (optionally under a category).</li>
     * </ul>
     * An empty goal needs no destination account and records nothing.
     *
     * @throws SavingsGoalRuntimeException when the goal is already closed or it
     *                                     holds money and no account was given
     */
    @Transactional
    public SavingsGoalDTO close(UUID goalId, SavingsGoalCloseDTO dto) {
        SavingsGoal goal = getEntityById(goalId);
        assertActive(goal);

        BigDecimal remaining = goal.getCurrentAmount();
        if (remaining.signum() > 0) {
            if (dto == null || dto.accountId() == null) {
                throw new SavingsGoalRuntimeException("Choose an account to receive the saved money before closing the goal");
            }
            Account account = accountService.getEntityById(dto.accountId());
            contribute(goal, account, TransactionType.SAVINGS_WITHDRAWAL, remaining, null,
                    "Closed savings goal · " + goal.getName());

            if (dto.resolvedOutcome() == SavingsGoalCloseOutcome.SPEND) {
                recordSpentSavings(goal, account, remaining, dto.categoryId());
            }
        }

        goal.setStatus(SavingsGoalStatus.CLOSED);
        SavingsGoal saved = savingsGoalRepository.save(goal);
        return enrich(savingsGoalMapper.toDto(saved), saved);
    }

    /**
     * Aggregated snapshot across every open goal owned by the current user. Closed
     * goals no longer hold money, so they are left out of every figure.
     */
    public SavingsGoalSummaryDTO getSummary() {
        AppUser user = securityUtils.getCurrentUser();
        List<SavingsGoal> goals = savingsGoalRepository.findByUser(user)
                .stream()
                .filter(SavingsGoal::isActive)
                .toList();

        BigDecimal totalSaved = goals.stream()
                .map(SavingsGoal::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTarget = goals.stream()
                .map(SavingsGoal::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal progressPercentage = totalTarget.compareTo(BigDecimal.ZERO) > 0
                ? totalSaved.multiply(HUNDRED).divide(totalTarget, MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long achievedGoals = goals.stream().filter(SavingsGoal::isAchieved).count();

        return new SavingsGoalSummaryDTO(totalSaved, totalTarget, progressPercentage, goals.size(), achievedGoals);
    }

    /** User-scoped lookup: never resolves a goal by id alone. */
    public SavingsGoal getEntityById(UUID uuid) {
        AppUser user = securityUtils.getCurrentUser();
        return savingsGoalRepository.findByIdAndUser(uuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Savings goal not found"));
    }

    /**
     * Shared tail of deposit, withdraw and close: applies the balance maths through
     * {@link TransactionService}, records the movement as a transaction and returns
     * the refreshed goal. Callers have already validated the goal-level rules.
     */
    private SavingsGoalDTO contribute(
            SavingsGoal goal,
            Account account,
            TransactionType type,
            BigDecimal amount,
            LocalDateTime date,
            String note
    ) {
        transactionService.applyBalanceChanges(type, amount, account, null, goal);

        Transaction movement = Transaction.builder()
                .date(date != null ? date : LocalDateTime.now())
                .amount(amount)
                .type(type)
                .description(note != null && !note.isBlank() ? note.trim() : "Savings · " + goal.getName())
                .counterparty(goal.getName())
                .account(account)
                .savingsGoal(goal)
                .user(goal.getUser())
                .build();
        transactionRepository.save(movement);

        SavingsGoal saved = savingsGoalRepository.save(goal);
        return enrich(savingsGoalMapper.toDto(saved), saved);
    }

    /**
     * Records the money a closed goal was spent on as a regular expense. Runs right
     * after the closing withdrawal, so the account ends where it started and the
     * expense is what remains in the history. The goal is kept on the transaction
     * for traceability; balance-wise it behaves like any other expense.
     */
    private void recordSpentSavings(SavingsGoal goal, Account account, BigDecimal amount, UUID categoryId) {
        Category category = categoryId != null ? categoryService.getEntityById(categoryId) : null;

        transactionService.applyBalanceChanges(TransactionType.EXPENSE, amount, account, null, null);

        Transaction expense = Transaction.builder()
                .date(LocalDateTime.now())
                .amount(amount)
                .type(TransactionType.EXPENSE)
                .description("Spent savings goal · " + goal.getName())
                .counterparty(goal.getName())
                .category(category)
                .account(account)
                .savingsGoal(goal)
                .user(goal.getUser())
                .build();
        transactionRepository.save(expense);
    }

    /** Resolves and validates ownership of the optional default source account. */
    private Account resolveLinkedAccount(UUID linkedAccountId) {
        return linkedAccountId != null ? accountService.getEntityById(linkedAccountId) : null;
    }

    private void assertActive(SavingsGoal goal) {
        if (!goal.isActive()) {
            throw new SavingsGoalRuntimeException("Savings goal is closed");
        }
    }

    private void validateTargetAmount(SavingsGoalDTO dto) {
        if (dto.targetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SavingsGoalRuntimeException("Target amount must be greater than zero");
        }
    }

    private void validateTargetDate(LocalDate targetDate) {
        if (targetDate.isBefore(LocalDate.now())) {
            throw new SavingsGoalRuntimeException("Target date must be a future date");
        }
    }

    private void validateContributionAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new SavingsGoalRuntimeException("Amount must be greater than zero");
        }
    }

    /** Recomputes and caches whether the saved amount already covers the target. */
    private void refreshAchieved(SavingsGoal goal) {
        goal.setAchieved(goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0);
    }

    /** Display state: closed beats achieved, achieved beats overdue. */
    private String deriveState(SavingsGoal goal) {
        if (!goal.isActive()) {
            return SavingsGoalDTO.STATE_CLOSED;
        }
        if (goal.isAchieved()) {
            return SavingsGoalDTO.STATE_ACHIEVED;
        }
        if (goal.getTargetDate().isBefore(LocalDate.now())) {
            return SavingsGoalDTO.STATE_OVERDUE;
        }
        return SavingsGoalDTO.STATE_ACTIVE;
    }

    /**
     * Fills in the derived (read-only) components of {@code dto} using the
     * current state of {@code entity}. The persisted components of {@code dto}
     * are left untouched.
     */
    private SavingsGoalDTO enrich(SavingsGoalDTO dto, SavingsGoal entity) {
        BigDecimal currentAmount = entity.getCurrentAmount();
        BigDecimal targetAmount = entity.getTargetAmount();

        BigDecimal progressPercentage = targetAmount.compareTo(BigDecimal.ZERO) > 0
                ? currentAmount.multiply(HUNDRED).divide(targetAmount, MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal remainingAmount = targetAmount.subtract(currentAmount).max(BigDecimal.ZERO);

        long daysRemaining = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), entity.getTargetDate()));

        // At least one month is always assumed so the split never divides by zero,
        // even when the target date is this month or already overdue.
        long monthsRemaining = Math.max(1, Period.between(LocalDate.now(), entity.getTargetDate()).toTotalMonths());
        BigDecimal monthlyRequired = remainingAmount.compareTo(BigDecimal.ZERO) > 0
                ? remainingAmount.divide(BigDecimal.valueOf(monthsRemaining), MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new SavingsGoalDTO(
                dto.id(),
                dto.name(),
                dto.icon(),
                dto.targetAmount(),
                dto.targetDate(),
                dto.linkedAccountId(),
                dto.linkedAccountName(),
                entity.getStatus(),
                deriveState(entity),
                entity.isAchieved(),
                currentAmount,
                progressPercentage,
                remainingAmount,
                monthlyRequired,
                daysRemaining
        );
    }
}
