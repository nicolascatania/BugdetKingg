package com.veritech.BudgetKing.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AccountRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AccountRepository repository;

    @Test
    void findByUser() {
        var results = repository.findByUser(savedUser);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("Main Bank", results.get(0).getName());

    }

    @Test
    void findByIdAndUser() {
        var result = repository.findByIdAndUser(savedAccount.getId(), savedUser);
        assertTrue(result.isPresent(), "Account should be found");
        result.ifPresent(account -> {
            assertEquals("Main Bank", account.getName());
            assertEquals(savedAccount.getId(), account.getId());
            assertEquals(savedUser.getId(), account.getUser().getId());
        });
    }

    @Test
    void findAllByUser() {
        var result = repository.findAllByUser(savedUser);
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertEquals(1, result.size(), "Should have 1 result");
    }

    @Test
    void countTransactionsByAccountAndUser() {
        long count = repository.countTransactionsByAccountAndUser(savedAccount.getId(), savedUser);
        assertEquals(2, count, "Should have 2 Transactions");
    }
}