package com.veritech.BudgetKing.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AppUserRepositoryTest extends BaseRepositoryTest {


    @Autowired
    private AppUserRepository repository;

    @Test
    void findByEmail() {
        var result = repository.findByEmail("nico@budgetking.com");
        assertTrue(result.isPresent(), "User should be found");
        result.ifPresent(user -> {
            assertEquals("Nicolas", user.getName());
            assertEquals(savedUser.getId(), user.getId());
        });
    }

    @Test
    void existsByEmail() {
        boolean result = repository.existsByEmail("nico@budgetking.com");
        assertTrue(result, "User should exist");
    }
}