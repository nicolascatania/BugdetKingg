package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.BudgetDTO;
import com.veritech.BudgetKing.dto.BudgetProgressDTO;
import com.veritech.BudgetKing.dto.BudgetRelatedEntities;
import com.veritech.BudgetKing.dto.OptionDTO;
import com.veritech.BudgetKing.exception.BudgetRuntimeException;
import com.veritech.BudgetKing.filter.BudgetFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.BudgetMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Budget;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.dict.Budget_;
import com.veritech.BudgetKing.repository.BudgetRepository;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Monthly spending limits per category, plus the progress reading that compares
 * each limit against the expenses already registered for its period.
 */
@Service
@RequiredArgsConstructor
public class BudgetService implements ICrudService<BudgetDTO, UUID, BudgetFilter> {

    /** Spending is comfortably inside the limit. */
    private static final String STATUS_OK = "OK";
    /** The limit is close to being consumed. */
    private static final String STATUS_WARNING = "WARNING";
    /** The limit has been reached or passed. */
    private static final String STATUS_EXCEEDED = "EXCEEDED";

    private static final double WARNING_THRESHOLD = 80.0;
    private static final double EXCEEDED_THRESHOLD = 100.0;

    /** Page size used when a search arrives without a filter body. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final String DUPLICATED_BUDGET_MESSAGE =
            "A budget already exists for this category in the selected period";

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final BudgetMapper budgetMapper;
    private final SecurityUtils securityUtils;

    /**
     * Reads a single budget owned by the authenticated user.
     *
     * @param id identifier of the budget
     * @return the budget as a DTO
     * @throws EntityNotFoundException when the user has no budget with that identifier
     */
    @Override
    public BudgetDTO getById(UUID id) {
        AppUser user = securityUtils.getCurrentUser();

        return budgetRepository.findByIdAndUser(id, user)
                .map(budgetMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
    }

    /**
     * Creates a budget for the authenticated user.
     *
     * @param dto budget to create; its category must already belong to the user
     * @return the persisted budget
     * @throws BudgetRuntimeException when the category already has a budget for that period
     */
    @Override
    @Transactional
    public BudgetDTO create(BudgetDTO dto) {
        AppUser user = securityUtils.getCurrentUser();
        Category category = categoryService.getEntityById(dto.category());

        assertPeriodIsFree(user, category, dto.year(), dto.month());

        Budget budget = budgetMapper.toEntity(dto, new BudgetRelatedEntities(user, category));

        return budgetMapper.toDto(budgetRepository.save(budget));
    }

    /**
     * Updates an existing budget of the authenticated user.
     *
     * @param id  identifier of the budget to update
     * @param dto new category, period and limit
     * @return the updated budget
     * @throws EntityNotFoundException when the user has no budget with that identifier
     * @throws BudgetRuntimeException  when the new category/period pair is already budgeted
     */
    @Override
    @Transactional
    public BudgetDTO update(UUID id, BudgetDTO dto) {
        AppUser user = securityUtils.getCurrentUser();

        Budget found = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));

        Category category = categoryService.getEntityById(dto.category());

        // Only re-check the uniqueness rule when the budget actually moves to another slot.
        if (movesToAnotherSlot(found, category, dto)) {
            assertPeriodIsFree(user, category, dto.year(), dto.month());
        }

        found.setCategory(category);
        found.setYear(dto.year());
        found.setMonth(dto.month());
        found.setLimitAmount(dto.limitAmount());
        found.setRecurring(dto.recurring());

        return budgetMapper.toDto(budgetRepository.save(found));
    }

    /**
     * Deletes a budget owned by the authenticated user.
     *
     * @param id identifier of the budget to delete
     * @throws EntityNotFoundException when the user has no budget with that identifier
     */
    @Override
    @Transactional
    public void deleteById(UUID id) {
        AppUser user = securityUtils.getCurrentUser();

        Budget found = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));

        budgetRepository.delete(found);
    }

    /**
     * Paginated, user-scoped budget search. Most recent periods come first.
     *
     * @param filter search criteria; a null body falls back to the first page
     * @return page of budgets
     */
    @Override
    public Page<BudgetDTO> search(BudgetFilter filter) {
        AppUser user = securityUtils.getCurrentUser();

        if (filter == null) {
            return budgetRepository
                    .findByUser(user, PageRequest.of(0, DEFAULT_PAGE_SIZE, newestPeriodFirst()))
                    .map(budgetMapper::toDto);
        }

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), newestPeriodFirst());

        return budgetRepository.findAll(filter.toSpecification(user), pageable)
                .map(budgetMapper::toDto);
    }

    /**
     * Budget options for selects, labelled with the category and the period.
     *
     * @return one option per budget owned by the authenticated user
     */
    @Override
    public List<OptionDTO> getOptions() {
        AppUser user = securityUtils.getCurrentUser();

        return budgetRepository.findByUser(user)
                .stream()
                .map(budget -> new OptionDTO(
                        budget.getId().toString(),
                        "%s %02d/%d".formatted(budget.getCategory().getName(), budget.getMonth(), budget.getYear())
                ))
                .toList();
    }

    /**
     * Compares every budget that applies to a period against what the user actually
     * spent on each budgeted category during that same period.
     *
     * <p>Which budget applies to a category is resolved as: the budget created for
     * that exact period if there is one, otherwise the recurring budget with the
     * latest start on or before the period. So a recurring limit set once carries
     * over month after month, and a budget created for a specific month overrides
     * it for that month only.</p>
     *
     * @param year  calendar year of the period
     * @param month calendar month of the period, 1 through 12
     * @return one progress reading per category budgeted for the period
     */
    public List<BudgetProgressDTO> getProgress(int year, int month) {
        AppUser user = securityUtils.getCurrentUser();

        YearMonth period = YearMonth.of(year, month);
        LocalDateTime start = period.atDay(1).atStartOfDay();
        LocalDateTime end = period.plusMonths(1).atDay(1).atStartOfDay();

        Map<String, BigDecimal> spentByCategory = expensesByCategoryName(user, start, end);

        return resolveBudgetsFor(user, year, month)
                .stream()
                .map(budget -> toProgress(
                        budget,
                        spentByCategory.getOrDefault(budget.getCategory().getName(), BigDecimal.ZERO)
                ))
                .toList();
    }

    /**
     * One budget per category for the period: exact-period budgets first, then the
     * newest recurring budget for every category not already covered.
     */
    private List<Budget> resolveBudgetsFor(AppUser user, int year, int month) {
        Map<UUID, Budget> byCategory = new LinkedHashMap<>();

        for (Budget exact : budgetRepository.findByUserAndYearAndMonth(user, year, month)) {
            byCategory.put(exact.getCategory().getId(), exact);
        }
        // Sorted newest start first, so putIfAbsent keeps the most recent one per category.
        for (Budget recurring : budgetRepository.findRecurringStartingOnOrBefore(user, year, month)) {
            byCategory.putIfAbsent(recurring.getCategory().getId(), recurring);
        }

        return List.copyOf(byCategory.values());
    }

    /**
     * Totals the period's expenses per category name, reusing the dashboard query.
     * The query groups by name and icon, so rows sharing a name are folded together.
     */
    private Map<String, BigDecimal> expensesByCategoryName(AppUser user, LocalDateTime start, LocalDateTime end) {
        Map<String, BigDecimal> totals = new HashMap<>();

        for (Object[] row : transactionRepository.getExpensesByCategoryWithIcon(user, start, end)) {
            String categoryName = (String) row[0];
            BigDecimal amount = (BigDecimal) row[2];
            totals.merge(categoryName, amount, BigDecimal::add);
        }

        return totals;
    }

    /**
     * Builds the progress reading of a single budget.
     *
     * <p>A limit of zero carries no meaningful ratio, so the percentage stays at
     * zero instead of dividing by it.</p>
     */
    private BudgetProgressDTO toProgress(Budget budget, BigDecimal spentAmount) {
        BigDecimal limitAmount = budget.getLimitAmount() != null ? budget.getLimitAmount() : BigDecimal.ZERO;

        double percentage = limitAmount.compareTo(BigDecimal.ZERO) > 0
                ? spentAmount.divide(limitAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                : 0.0;

        Category category = budget.getCategory();

        return new BudgetProgressDTO(
                budget.getId(),
                category.getId(),
                category.getName(),
                category.getIcon(),
                budget.getYear(),
                budget.getMonth(),
                budget.isRecurring(),
                limitAmount,
                spentAmount,
                limitAmount.subtract(spentAmount),
                percentage,
                resolveStatus(percentage)
        );
    }

    /** Maps a consumed percentage to the traffic-light status shown by the UI. */
    private String resolveStatus(double percentage) {
        if (percentage >= EXCEEDED_THRESHOLD) {
            return STATUS_EXCEEDED;
        }
        if (percentage >= WARNING_THRESHOLD) {
            return STATUS_WARNING;
        }
        return STATUS_OK;
    }

    /** Rejects a second budget for the same category and period. */
    private void assertPeriodIsFree(AppUser user, Category category, int year, int month) {
        if (budgetRepository.existsByUserAndCategoryAndYearAndMonth(user, category, year, month)) {
            throw new BudgetRuntimeException(DUPLICATED_BUDGET_MESSAGE);
        }
    }

    /** True when the update targets a different category or a different period. */
    private boolean movesToAnotherSlot(Budget current, Category newCategory, BudgetDTO dto) {
        return !newCategory.getId().equals(current.getCategory().getId())
                || current.getYear() != dto.year()
                || current.getMonth() != dto.month();
    }

    /** Newest period first, so the most relevant budgets open the list. */
    private Sort newestPeriodFirst() {
        return Sort.by(Budget_.year).descending().and(Sort.by(Budget_.month).descending());
    }
}
