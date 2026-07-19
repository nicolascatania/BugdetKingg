package com.veritech.BudgetKing.repository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;


import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("AppUser Repository Specification")
class AppUserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AppUserRepository repository;

    private String targetEmail;
    private String expectedUserName;

    @BeforeEach
    void setUpDefaults() {
        targetEmail = "nico@budgetking.com";
        expectedUserName = "Nicolas";
    }

    @Test
    @DisplayName("Should retrieve user successfully when email exists")
    void shouldFindUserByEmail() {
        var result = repository.findByEmail(targetEmail);

        assertTrue(result.isPresent(), () -> "User should be found for the provided email");
        result.ifPresent(user -> {
            assertEquals(expectedUserName, user.getName(), () -> "User name mismatch");
            assertEquals(savedUser.getId(), user.getId(), () -> "User ID mismatch");
        });
    }

    @Test
    @DisplayName("Should return true when verifying existence of an existing email")
    void shouldReturnTrueWhenEmailExists() {
        boolean exists = repository.existsByEmail(targetEmail);

        assertTrue(exists, () -> "User should exist for the provided email");
    }
}