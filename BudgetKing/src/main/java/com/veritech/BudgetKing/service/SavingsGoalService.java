package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.OptionDTO;
import com.veritech.BudgetKing.dto.SavingsGoalDTO;
import com.veritech.BudgetKing.dto.SavingsGoalRelatedEntities;
import com.veritech.BudgetKing.dto.SavingsGoalSummaryDTO;
import com.veritech.BudgetKing.exception.SavingsGoalRuntimeException;
import com.veritech.BudgetKing.filter.SavingsGoalFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.SavingsGoalMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.SavingsGoal;
import com.veritech.BudgetKing.repository.SavingsGoalRepository;
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
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for {@link SavingsGoal}.
 *
 * <p>Progress is never persisted: every read recomputes {@code currentAmount}
 * (and everything derived from it) from the balance of the goal's linked
 * account, so figures shown to the user always reflect its current state.
 * Only the {@code achieved} flag is cached on the entity, and it is
 * refreshed here on every create/update so it can be relied upon for
 * reporting (e.g. {@link #getSummary()}) without recomputing balances.</p>
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

    @Override
    public SavingsGoalDTO getById(UUID uuid) {
        AppUser user = securityUtils.getCurrentUser();
        SavingsGoal found = savingsGoalRepository.findByIdAndUser(uuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Savings goal not found"));

        return enrich(savingsGoalMapper.toDto(found), found);
    }

    @Override
    @Transactional
    public SavingsGoalDTO create(SavingsGoalDTO dto) {
        AppUser user = securityUtils.getCurrentUser();
        validateBusinessRules(dto);

        Account linkedAccount = resolveLinkedAccount(dto.linkedAccountId());
        SavingsGoalRelatedEntities relatedEntities = new SavingsGoalRelatedEntities(user, linkedAccount);

        SavingsGoal goal = savingsGoalMapper.toEntity(dto, relatedEntities);
        refreshAchieved(goal);

        SavingsGoal saved = savingsGoalRepository.save(goal);
        return enrich(savingsGoalMapper.toDto(saved), saved);
    }

    @Override
    @Transactional
    public SavingsGoalDTO update(UUID uuid, SavingsGoalDTO dto) {
        AppUser user = securityUtils.getCurrentUser();
        SavingsGoal found = savingsGoalRepository.findByIdAndUser(uuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Savings goal not found"));

        validateBusinessRules(dto);

        found.setName(dto.name());
        found.setIcon(dto.icon());
        found.setTargetAmount(dto.targetAmount());
        found.setTargetDate(dto.targetDate());
        found.setLinkedAccount(resolveLinkedAccount(dto.linkedAccountId()));
        refreshAchieved(found);

        SavingsGoal saved = savingsGoalRepository.save(found);
        return enrich(savingsGoalMapper.toDto(saved), saved);
    }

    @Override
    @Transactional
    public void deleteById(UUID uuid) {
        AppUser user = securityUtils.getCurrentUser();
        SavingsGoal found = savingsGoalRepository.findByIdAndUser(uuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Savings goal not found"));

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
     * Aggregated snapshot across every goal owned by the current user.
     * {@code achievedGoals} relies on the cached {@link SavingsGoal#isAchieved()}
     * flag rather than recomputing it, per the class-level contract.
     */
    public SavingsGoalSummaryDTO getSummary() {
        AppUser user = securityUtils.getCurrentUser();
        List<SavingsGoal> goals = savingsGoalRepository.findByUser(user);

        BigDecimal totalSaved = goals.stream()
                .map(this::getCurrentAmount)
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

    public SavingsGoal getEntityById(UUID uuid) {
        AppUser user = securityUtils.getCurrentUser();
        return savingsGoalRepository.findByIdAndUser(uuid, user)
                .orElseThrow(() -> new EntityNotFoundException("Savings goal not found"));
    }

    /** Resolves and validates ownership of the optional linked account. */
    private Account resolveLinkedAccount(UUID linkedAccountId) {
        return linkedAccountId != null ? accountService.getEntityById(linkedAccountId) : null;
    }

    /** Rejects target amounts that are not positive or target dates already in the past. */
    private void validateBusinessRules(SavingsGoalDTO dto) {
        if (dto.targetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SavingsGoalRuntimeException("Target amount must be greater than zero");
        }
        if (dto.targetDate().isBefore(LocalDate.now())) {
            throw new SavingsGoalRuntimeException("Target date must be a future date");
        }
    }

    /** Recomputes and caches whether the linked balance already covers the target. */
    private void refreshAchieved(SavingsGoal goal) {
        goal.setAchieved(getCurrentAmount(goal).compareTo(goal.getTargetAmount()) >= 0);
    }

    private BigDecimal getCurrentAmount(SavingsGoal goal) {
        Account linkedAccount = goal.getLinkedAccount();
        return linkedAccount != null ? linkedAccount.getBalance() : BigDecimal.ZERO;
    }

    /**
     * Fills in the derived (read-only) components of {@code dto} using the
     * current state of {@code entity}. The persisted components of {@code dto}
     * are left untouched.
     */
    private SavingsGoalDTO enrich(SavingsGoalDTO dto, SavingsGoal entity) {
        BigDecimal currentAmount = getCurrentAmount(entity);
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
                entity.isAchieved(),
                currentAmount,
                progressPercentage,
                remainingAmount,
                monthlyRequired,
                daysRemaining
        );
    }
}
