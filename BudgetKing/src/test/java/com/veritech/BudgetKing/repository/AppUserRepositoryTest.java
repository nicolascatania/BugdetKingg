package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AppUserRepositoryTest extends BaseRepositoryTest {


    @Autowired
    private AppUserRepository repository;

    @Test
    @DisplayName("Find user by email")
    void findByEmail() {
        var result = repository.findByEmail("nico@budgetking.com");
        assertTrue(result.isPresent(), "User should be found");
        result.ifPresent(user -> {
            assertEquals("Nicolas", user.getName());
            assertEquals(savedUser.getId(), user.getId());
        });
    }

    @Test
    @DisplayName("Determine if an user exists using email")
    void existsByEmail() {
        boolean result = repository.existsByEmail("nico@budgetking.com");
        assertTrue(result, "User should exist");
    }

}