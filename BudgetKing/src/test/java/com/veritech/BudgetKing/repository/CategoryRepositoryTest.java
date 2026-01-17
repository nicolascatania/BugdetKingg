package com.veritech.BudgetKing.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;


import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CategoryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;


    @Test
    void findByUser() {
        var results = categoryRepository.findByUser(savedUser);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("Entertainment", results.get(0).getName());
    }

    @Test
    void countTransactionsByCategory() {
        long count = categoryRepository.countTransactionsByCategory(
                savedCategory.getId(),
                savedUser
        );

        assertEquals(2, count, "Should have 2 transactions");
    }

    @Test
    void findByIdAndUser() {
        var result = categoryRepository.findByIdAndUser(savedCategory.getId(), savedUser);

        assertTrue(result.isPresent());
        assertEquals(savedCategory.getName(), result.get().getName());
    }

    @Test
    void getByNameAndUser() {
        var result = categoryRepository.getByNameAndUser("Entertainment", savedUser);

        assertTrue(result.isPresent());
        assertEquals(savedCategory.getId(), result.get().getId());
    }
}