package com.veritech.BudgetKing.repository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Category Repository Specification")
class CategoryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private String expectedCategoryName;
    private long expectedTransactionCount;

    @BeforeEach
    void setUpDefaults() {
        expectedCategoryName = "Entertainment";
        expectedTransactionCount = 2L;
    }

    @Test
    @DisplayName("Should retrieve all categories associated with a specific user")
    void shouldRetrieveCategoriesByUser() {
        var results = categoryRepository.findByUser(savedUser);

        assertNotNull(results, () -> "The result list should not be null");
        assertFalse(results.isEmpty(), () -> "The result list should contain categories");
        assertEquals(expectedCategoryName, results.get(0).getName(), () -> "Category name should match expected");
    }

    @Test
    @DisplayName("Should return the correct transaction count for a given category")
    void shouldCountTransactionsByCategory() {
        long count = categoryRepository.countTransactionsByCategory(
                savedCategory.getId(),
                savedUser
        );

        assertEquals(expectedTransactionCount, count, () -> "Transaction count mismatch");
    }

    @Test
    @DisplayName("Should retrieve a specific category by ID and user association")
    void shouldRetrieveCategoryByIdAndUser() {
        var result = categoryRepository.findByIdAndUser(savedCategory.getId(), savedUser);

        assertTrue(result.isPresent(), () -> "Category should be found for the given ID and user");
        assertEquals(savedCategory.getName(), result.get().getName(), () -> "Category name mismatch");
    }

    @Test
    @DisplayName("Should retrieve a category by its name and user association")
    void shouldGetCategoryByNameAndUser() {
        var result = categoryRepository.getByNameAndUser(expectedCategoryName, savedUser);

        assertTrue(result.isPresent(), () -> "Category should be found for the given name and user");
        assertEquals(savedCategory.getId(), result.get().getId(), () -> "Category ID mismatch");
    }
}