package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.model.Budget;
import com.veritech.BudgetKing.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Budget Repository Specification")
class BudgetRepositoryTest extends BaseRepositoryTest {

    private static final int YEAR = 2026;
    private static final int MONTH = 8;

    @Autowired
    private BudgetRepository budgetRepository;

    /** Budget of the inherited "Entertainment" category for the reference period. */
    private Budget savedEntertainmentBudget;

    /** Budget of a second category, same period, to prove the period query returns both. */
    private Budget savedGroceriesBudget;

    /** Same category as the first one but the following month, so it must not leak into the period query. */
    private Budget savedNextMonthBudget;

    private Category savedGroceriesCategory;

    @BeforeEach
    void setUpDefaults() {
        savedGroceriesCategory = entityManager.persistFlushFind(Category.builder()
                .name("Groceries")
                .icon("fa-cart-shopping")
                .user(savedUser)
                .build());

        savedEntertainmentBudget = entityManager.persistFlushFind(Budget.builder()
                .category(savedCategory)
                .user(savedUser)
                .year(YEAR)
                .month(MONTH)
                .limitAmount(new BigDecimal("150.00"))
                .build());

        savedGroceriesBudget = entityManager.persistFlushFind(Budget.builder()
                .category(savedGroceriesCategory)
                .user(savedUser)
                .year(YEAR)
                .month(MONTH)
                .limitAmount(new BigDecimal("300.00"))
                .build());

        savedNextMonthBudget = entityManager.persistFlushFind(Budget.builder()
                .category(savedCategory)
                .user(savedUser)
                .year(YEAR)
                .month(MONTH + 1)
                .limitAmount(new BigDecimal("175.00"))
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should retrieve a budget by its ID and user association")
    void shouldRetrieveBudgetByIdAndUser() {
        var result = budgetRepository.findByIdAndUser(savedEntertainmentBudget.getId(), savedUser);

        assertTrue(result.isPresent(), () -> "Budget should be found for the given ID and user");
        assertEquals(0, new BigDecimal("150.00").compareTo(result.get().getLimitAmount()),
                () -> "Limit amount mismatch");
        assertEquals(savedCategory.getId(), result.get().getCategory().getId(),
                () -> "Budget should point to the expected category");
    }

    @Test
    @DisplayName("Should not retrieve a budget with an unknown ID")
    void shouldNotRetrieveUnknownBudget() {
        var result = budgetRepository.findByIdAndUser(UUID.randomUUID(), savedUser);

        assertTrue(result.isEmpty(), () -> "No budget should be found for an unknown ID");
    }

    @Test
    @DisplayName("Should retrieve only the budgets defined for a given period")
    void shouldRetrieveBudgetsByPeriod() {
        List<Budget> results = budgetRepository.findByUserAndYearAndMonth(savedUser, YEAR, MONTH);

        assertEquals(2, results.size(), () -> "Only the two budgets of the period should be returned");
        assertTrue(results.stream().noneMatch(b -> b.getId().equals(savedNextMonthBudget.getId())),
                () -> "The next month's budget must not leak into the period query");
    }

    @Test
    @DisplayName("Should confirm a category is already budgeted for a period")
    void shouldDetectExistingBudgetForPeriod() {
        boolean exists = budgetRepository
                .existsByUserAndCategoryAndYearAndMonth(savedUser, savedCategory, YEAR, MONTH);

        assertTrue(exists, () -> "The category is already budgeted for the period");
    }

    @Test
    @DisplayName("Should report no budget for a period the category has not been budgeted in")
    void shouldDetectMissingBudgetForPeriod() {
        boolean exists = budgetRepository
                .existsByUserAndCategoryAndYearAndMonth(savedUser, savedGroceriesCategory, YEAR, MONTH + 1);

        assertFalse(exists, () -> "That category has no budget for the following month");
    }

    @Test
    @DisplayName("Should page through the budgets of a user")
    void shouldPageBudgetsByUser() {
        Page<Budget> firstPage = budgetRepository.findByUser(savedUser, PageRequest.of(0, 2));

        assertEquals(3, firstPage.getTotalElements(), () -> "The user owns three budgets");
        assertEquals(2, firstPage.getContent().size(), () -> "The first page should hold two budgets");
        assertEquals(2, firstPage.getTotalPages(), () -> "Three budgets over a page size of two");
        assertNotNull(savedGroceriesBudget.getId(), () -> "Fixtures should have been persisted with an ID");
    }
}
