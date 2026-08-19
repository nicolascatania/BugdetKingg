package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.model.SavingsGoal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("SavingsGoal Repository Specification")
class SavingsGoalRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    private SavingsGoal savedGoal;
    private String expectedGoalName;

    @BeforeEach
    void setUpDefaults() {
        expectedGoalName = "Vacation";

        SavingsGoal goal = SavingsGoal.builder()
                .name(expectedGoalName)
                .icon("fa fa-plane")
                .targetAmount(new BigDecimal("2000.00"))
                .targetDate(LocalDate.now().plusMonths(6))
                .linkedAccount(savedAccount)
                .user(savedUser)
                .achieved(false)
                .build();

        this.savedGoal = entityManager.persistFlushFind(goal);
    }

    @Test
    @DisplayName("Should retrieve all savings goals associated with a specific user")
    void shouldRetrieveGoalsByUser() {
        var results = savingsGoalRepository.findByUser(savedUser);

        assertNotNull(results, () -> "The result list should not be null");
        assertFalse(results.isEmpty(), () -> "The result list should contain savings goals");
        assertEquals(expectedGoalName, results.get(0).getName(), () -> "Savings goal name should match expected");
    }

    @Test
    @DisplayName("Should retrieve a specific savings goal by ID and user association")
    void shouldRetrieveGoalByIdAndUser() {
        Optional<SavingsGoal> result = savingsGoalRepository.findByIdAndUser(savedGoal.getId(), savedUser);

        assertTrue(result.isPresent(), () -> "Savings goal should be found for the given ID and user");
        result.ifPresent(goal -> {
            assertEquals(expectedGoalName, goal.getName(), () -> "Savings goal name mismatch");
            assertEquals(savedAccount.getId(), goal.getLinkedAccount().getId(), () -> "Linked account mismatch");
        });
    }

    @Test
    @DisplayName("Should not retrieve a savings goal that does not belong to the user")
    void shouldNotRetrieveGoalForAnotherUser() {
        Optional<SavingsGoal> result = savingsGoalRepository.findByIdAndUser(savedGoal.getId(), null);

        assertTrue(result.isEmpty(), () -> "Savings goal must not resolve for an unrelated/null user");
    }

    @Test
    @DisplayName("Should paginate savings goals for a user")
    void shouldPaginateGoalsByUser() {
        var page = savingsGoalRepository.findByUser(savedUser,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements(), () -> "Expected exactly 1 savings goal for this user");
    }
}
