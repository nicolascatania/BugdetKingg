package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.enumerator.SavingsGoalStatus;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.filter.SavingsGoalFilter;
import com.veritech.BudgetKing.model.SavingsGoal;
import com.veritech.BudgetKing.model.Transaction;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("SavingsGoal Repository Specification")
class SavingsGoalRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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
    @Test
    @DisplayName("Should persist the money held and the lifecycle status with sensible defaults")
    void shouldPersistCurrentAmountAndStatus() {
        assertEquals(0, BigDecimal.ZERO.compareTo(savedGoal.getCurrentAmount()), () -> "A new goal starts empty");
        assertEquals(SavingsGoalStatus.ACTIVE, savedGoal.getStatus(), () -> "A new goal starts ACTIVE");
    }

    @Test
    @DisplayName("Should filter goals by status")
    void shouldFilterByStatus() {
        SavingsGoal closed = SavingsGoal.builder()
                .name("Old laptop")
                .icon("fa fa-laptop")
                .targetAmount(new BigDecimal("500.00"))
                .targetDate(LocalDate.now().plusMonths(1))
                .status(SavingsGoalStatus.CLOSED)
                .user(savedUser)
                .achieved(true)
                .build();
        entityManager.persistAndFlush(closed);

        SavingsGoalFilter filter = new SavingsGoalFilter();
        filter.setStatus(SavingsGoalStatus.CLOSED);

        List<SavingsGoal> results = savingsGoalRepository.findAll(filter.toSpecification(savedUser));

        assertEquals(1, results.size(), () -> "Only the closed goal should match");
        assertEquals("Old laptop", results.get(0).getName(), () -> "Closed goal name mismatch");
    }

    @Test
    @DisplayName("Should detach contributions from a goal while keeping them in the account history")
    void shouldUnlinkSavingsGoalFromTransactions() {
        Transaction deposit = Transaction.builder()
                .date(LocalDateTime.now())
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.SAVINGS_DEPOSIT)
                .description("Savings · Vacation")
                .counterparty("Vacation")
                .account(savedAccount)
                .savingsGoal(savedGoal)
                .user(savedUser)
                .build();
        Transaction saved = entityManager.persistFlushFind(deposit);
        assertNotNull(saved.getSavingsGoal(), () -> "Deposit should start linked to the goal");

        int touched = transactionRepository.unlinkSavingsGoal(savedGoal);
        entityManager.clear();

        Transaction reloaded = entityManager.find(Transaction.class, saved.getId());
        assertEquals(1, touched, () -> "Exactly one movement should be detached");
        assertNotNull(reloaded, () -> "The movement itself must survive");
        assertNull(reloaded.getSavingsGoal(), () -> "The movement should no longer reference the goal");
        assertEquals(savedAccount.getId(), reloaded.getAccount().getId(), () -> "The account history is preserved");
    }
}
