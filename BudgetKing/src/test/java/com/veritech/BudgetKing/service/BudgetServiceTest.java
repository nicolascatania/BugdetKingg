package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.BudgetDTO;
import com.veritech.BudgetKing.dto.BudgetProgressDTO;
import com.veritech.BudgetKing.exception.BudgetRuntimeException;
import com.veritech.BudgetKing.mapper.BudgetMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Budget;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.BudgetRepository;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Budget Service Specification")
class BudgetServiceTest {

    private static final int YEAR = 2026;
    private static final int MONTH = 8;
    private static final String CATEGORY_NAME = "Entertainment";
    private static final String CATEGORY_ICON = "fa-tags";

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private BudgetMapper budgetMapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private BudgetService budgetService;

    private AppUser mockUser;
    private Category mockCategory;
    private UUID categoryId;
    private UUID budgetId;
    private BudgetDTO mockDto;

    @BeforeEach
    void setUpDefaults() {
        categoryId = UUID.randomUUID();
        budgetId = UUID.randomUUID();
        mockUser = new AppUser();

        mockCategory = Category.builder()
                .id(categoryId)
                .name(CATEGORY_NAME)
                .icon(CATEGORY_ICON)
                .user(mockUser)
                .build();

        mockDto = new BudgetDTO(
                budgetId,
                categoryId,
                CATEGORY_NAME,
                CATEGORY_ICON,
                YEAR,
                MONTH,
                new BigDecimal("100.00")
        );
    }

    @Test
    @DisplayName("Should create a budget when the category has no limit for the period")
    void shouldCreateBudgetSuccessfully() {
        Budget budget = budgetOf(new BigDecimal("100.00"));

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryService.getEntityById(categoryId)).thenReturn(mockCategory);
        when(budgetRepository.existsByUserAndCategoryAndYearAndMonth(mockUser, mockCategory, YEAR, MONTH))
                .thenReturn(false);
        when(budgetMapper.toEntity(any(), any())).thenReturn(budget);
        when(budgetRepository.save(budget)).thenReturn(budget);
        when(budgetMapper.toDto(budget)).thenReturn(mockDto);

        BudgetDTO result = budgetService.create(mockDto);

        assertNotNull(result, () -> "Created budget should not be null");
        assertEquals(MONTH, result.month(), () -> "Budget month mismatch");
        verify(budgetRepository).save(budget);
    }

    @Test
    @DisplayName("Should reject a duplicated budget for the same category and period")
    void shouldRejectDuplicatedBudgetOnCreate() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryService.getEntityById(categoryId)).thenReturn(mockCategory);
        when(budgetRepository.existsByUserAndCategoryAndYearAndMonth(mockUser, mockCategory, YEAR, MONTH))
                .thenReturn(true);

        BudgetRuntimeException exception = assertThrows(BudgetRuntimeException.class,
                () -> budgetService.create(mockDto),
                () -> "Should reject a second budget for the same category and period");

        assertEquals("A budget already exists for this category in the selected period",
                exception.getMessage(), () -> "Conflict message mismatch");
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    @DisplayName("Should reject an update that moves a budget onto an already budgeted period")
    void shouldRejectDuplicatedBudgetOnUpdate() {
        Budget existing = budgetOf(new BigDecimal("100.00"));
        BudgetDTO movedDto = new BudgetDTO(
                budgetId, categoryId, CATEGORY_NAME, CATEGORY_ICON, YEAR, MONTH + 1, new BigDecimal("120.00"));

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(budgetRepository.findByIdAndUser(budgetId, mockUser)).thenReturn(Optional.of(existing));
        when(categoryService.getEntityById(categoryId)).thenReturn(mockCategory);
        when(budgetRepository.existsByUserAndCategoryAndYearAndMonth(mockUser, mockCategory, YEAR, MONTH + 1))
                .thenReturn(true);

        assertThrows(BudgetRuntimeException.class,
                () -> budgetService.update(budgetId, movedDto),
                () -> "Should reject an update landing on an already budgeted period");

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    @DisplayName("Should update a budget in place without re-checking the period")
    void shouldUpdateBudgetSuccessfully() {
        Budget existing = budgetOf(new BigDecimal("100.00"));
        BudgetDTO updateDto = new BudgetDTO(
                budgetId, categoryId, CATEGORY_NAME, CATEGORY_ICON, YEAR, MONTH, new BigDecimal("250.00"));

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(budgetRepository.findByIdAndUser(budgetId, mockUser)).thenReturn(Optional.of(existing));
        when(categoryService.getEntityById(categoryId)).thenReturn(mockCategory);
        when(budgetRepository.save(existing)).thenReturn(existing);
        when(budgetMapper.toDto(existing)).thenReturn(updateDto);

        BudgetDTO result = budgetService.update(budgetId, updateDto);

        assertNotNull(result, () -> "Updated budget should not be null");
        assertEquals(0, new BigDecimal("250.00").compareTo(existing.getLimitAmount()),
                () -> "Limit amount should be updated on the entity");
        verify(budgetRepository, never())
                .existsByUserAndCategoryAndYearAndMonth(any(), any(), anyInt(), anyInt());
        verify(budgetRepository).save(existing);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when the budget does not exist")
    void shouldThrowExceptionWhenBudgetNotFound() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(budgetRepository.findByIdAndUser(budgetId, mockUser)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> budgetService.getById(budgetId),
                () -> "Should throw EntityNotFoundException for a missing budget");

        verify(budgetRepository).findByIdAndUser(budgetId, mockUser);
        verifyNoInteractions(budgetMapper);
    }

    @Test
    @DisplayName("Should compute the consumed percentage and keep the status at OK below 80%")
    void shouldComputePercentageAndReportOk() {
        stubProgress(new BigDecimal("100.00"), new BigDecimal("25.00"));

        BudgetProgressDTO progress = singleProgress();

        assertEquals(25.0, progress.percentage(), 0.0001, () -> "Percentage math mismatch");
        assertEquals(0, new BigDecimal("75.00").compareTo(progress.remainingAmount()),
                () -> "Remaining amount mismatch");
        assertEquals("OK", progress.status(), () -> "Status should stay OK below the warning threshold");
        verify(budgetRepository).findByUserAndYearAndMonth(mockUser, YEAR, MONTH);
    }

    @Test
    @DisplayName("Should report WARNING once spending reaches 80% of the limit")
    void shouldReportWarningAtEightyPercent() {
        stubProgress(new BigDecimal("100.00"), new BigDecimal("80.00"));

        BudgetProgressDTO progress = singleProgress();

        assertEquals(80.0, progress.percentage(), 0.0001, () -> "Percentage math mismatch");
        assertEquals("WARNING", progress.status(), () -> "Status should be WARNING at exactly 80%");
        verify(transactionRepository).getExpensesByCategoryWithIcon(eq(mockUser), any(), any());
    }

    @Test
    @DisplayName("Should report EXCEEDED once spending reaches 100% of the limit")
    void shouldReportExceededAtHundredPercent() {
        stubProgress(new BigDecimal("100.00"), new BigDecimal("120.00"));

        BudgetProgressDTO progress = singleProgress();

        assertEquals(120.0, progress.percentage(), 0.0001, () -> "Percentage math mismatch");
        assertEquals(0, new BigDecimal("-20.00").compareTo(progress.remainingAmount()),
                () -> "Remaining amount should go negative once exceeded");
        assertEquals("EXCEEDED", progress.status(), () -> "Status should be EXCEEDED from 100% upwards");
    }

    @Test
    @DisplayName("Should not divide by zero when the limit amount is zero")
    void shouldNotDivideByZeroOnZeroLimit() {
        stubProgress(BigDecimal.ZERO, new BigDecimal("50.00"));

        BudgetProgressDTO progress = assertDoesNotThrow(this::singleProgress,
                () -> "A zero limit must not blow up the progress calculation");

        assertEquals(0.0, progress.percentage(), 0.0001, () -> "A zero limit carries no ratio");
        assertEquals("OK", progress.status(), () -> "A zero limit should not raise an alert status");
        assertEquals(0, new BigDecimal("-50.00").compareTo(progress.remainingAmount()),
                () -> "Remaining amount should still be reported");
    }

    @Test
    @DisplayName("Should report zero spending for a budgeted category with no expenses")
    void shouldReportZeroSpendingWhenNoExpenses() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.getExpensesByCategoryWithIcon(eq(mockUser), any(), any()))
                .thenReturn(List.of());
        when(budgetRepository.findByUserAndYearAndMonth(mockUser, YEAR, MONTH))
                .thenReturn(List.of(budgetOf(new BigDecimal("100.00"))));

        BudgetProgressDTO progress = singleProgress();

        assertEquals(0, BigDecimal.ZERO.compareTo(progress.spentAmount()),
                () -> "Spent amount should be zero without expenses");
        assertEquals(0.0, progress.percentage(), 0.0001, () -> "Percentage should be zero without expenses");
        assertEquals("OK", progress.status(), () -> "Status should be OK without expenses");
    }

    /** Runs {@code getProgress} for the fixture period and returns its single reading. */
    private BudgetProgressDTO singleProgress() {
        List<BudgetProgressDTO> progress = budgetService.getProgress(YEAR, MONTH);

        assertEquals(1, progress.size(), () -> "Exactly one budget was defined for the period");
        return progress.get(0);
    }

    /** Stubs one budget for the period and one matching expense row. */
    private void stubProgress(BigDecimal limitAmount, BigDecimal spentAmount) {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.getExpensesByCategoryWithIcon(
                eq(mockUser), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(new Object[]{CATEGORY_NAME, CATEGORY_ICON, spentAmount}));
        when(budgetRepository.findByUserAndYearAndMonth(mockUser, YEAR, MONTH))
                .thenReturn(List.of(budgetOf(limitAmount)));
    }

    private Budget budgetOf(BigDecimal limitAmount) {
        return Budget.builder()
                .id(budgetId)
                .category(mockCategory)
                .user(mockUser)
                .year(YEAR)
                .month(MONTH)
                .limitAmount(limitAmount)
                .build();
    }
}
