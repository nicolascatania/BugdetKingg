package com.veritech.BudgetKing.repository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Account Repository Specification")
class AccountRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AccountRepository repository;

    private String expectedAccountName;
    private long expectedTransactionCount;

    @BeforeEach
    void setUpDefaults() {
        expectedAccountName = "Main Bank";
        expectedTransactionCount = 2L;
    }

    @Test
    @DisplayName("Should retrieve all accounts associated with a specific user")
    void shouldRetrieveAccountsByUser() {
        var results = repository.findByUser(savedUser);

        assertNotNull(results, () -> "The result list should not be null");
        assertFalse(results.isEmpty(), () -> "The result list should contain accounts");
        assertEquals(expectedAccountName, results.get(0).getName(), () -> "Account name should match expected");
    }

    @Test
    @DisplayName("Should retrieve a specific account by ID and user association")
    void shouldRetrieveAccountByIdAndUser() {
        var result = repository.findByIdAndUser(savedAccount.getId(), savedUser);

        assertTrue(result.isPresent(), () -> "Account should be found for the given ID and user");
        result.ifPresent(account -> {
            assertEquals(expectedAccountName, account.getName(), () -> "Account name mismatch");
            assertEquals(savedAccount.getId(), account.getId(), () -> "Account ID mismatch");
            assertEquals(savedUser.getId(), account.getUser().getId(), () -> "User ID association mismatch");
        });
    }

    @Test
    @DisplayName("Should return all accounts linked to the user with correct size")
    void shouldFindAllAccountsByUser() {
        var result = repository.findAllByUser(savedUser);

        assertNotNull(result, () -> "Result list should not be null");
        assertFalse(result.isEmpty(), () -> "Result list should not be empty");
        assertEquals(1, result.size(), () -> "Expected exactly 1 account for this user");
    }

    @Test
    @DisplayName("Should return the correct count of transactions for an account")
    void shouldCountTransactionsByAccountAndUser() {
        long count = repository.countTransactionsByAccountAndUser(savedAccount.getId(), savedUser);

        assertEquals(expectedTransactionCount, count, () -> "Transaction count mismatch");
    }
}