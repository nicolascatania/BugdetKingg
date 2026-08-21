package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.enumerator.RecurrenceFrequency;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.model.RecurringTransaction;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Recurring Transaction Repository Specification")
class RecurringTransactionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RecurringTransactionRepository repository;

    @Autowired
    private TestEntityManager testEntityManager;

    private RecurringTransaction dueTemplate;
    private RecurringTransaction futureTemplate;
    private RecurringTransaction pausedTemplate;
    private RecurringTransaction expiredTemplate;

    private LocalDate today;

    @BeforeEach
    void setUpTemplates() {
        today = LocalDate.now();

        dueTemplate = testEntityManager.persistFlushFind(RecurringTransaction.builder()
                .description("Netflix")
                .amount(new BigDecimal("15.00"))
                .type(TransactionType.EXPENSE)
                .counterparty("Netflix")
                .category(savedCategory)
                .account(savedAccount)
                .frequency(RecurrenceFrequency.MONTHLY)
                .nextRunDate(today.minusDays(1))
                .active(true)
                .user(savedUser)
                .build());

        futureTemplate = testEntityManager.persistFlushFind(RecurringTransaction.builder()
                .description("Gym")
                .amount(new BigDecimal("30.00"))
                .type(TransactionType.EXPENSE)
                .counterparty("Gym")
                .category(savedCategory)
                .account(savedAccount)
                .frequency(RecurrenceFrequency.WEEKLY)
                .nextRunDate(today.plusDays(10))
                .active(true)
                .user(savedUser)
                .build());

        pausedTemplate = testEntityManager.persistFlushFind(RecurringTransaction.builder()
                .description("Paused subscription")
                .amount(new BigDecimal("5.00"))
                .type(TransactionType.EXPENSE)
                .counterparty("Paused")
                .category(savedCategory)
                .account(savedAccount)
                .frequency(RecurrenceFrequency.MONTHLY)
                .nextRunDate(today.minusDays(1))
                .active(false)
                .user(savedUser)
                .build());

        expiredTemplate = testEntityManager.persistFlushFind(RecurringTransaction.builder()
                .description("Expired plan")
                .amount(new BigDecimal("8.00"))
                .type(TransactionType.EXPENSE)
                .counterparty("Expired")
                .category(savedCategory)
                .account(savedAccount)
                .frequency(RecurrenceFrequency.MONTHLY)
                .nextRunDate(today.minusDays(1))
                .endDate(today.minusDays(2))
                .active(true)
                .user(savedUser)
                .build());

        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("Should retrieve all templates for a user")
    void shouldRetrieveAllTemplatesByUser() {
        List<RecurringTransaction> templates = repository.findByUser(savedUser);
        assertEquals(4, templates.size(), () -> "Should find all 4 templates for the user");
    }

    @Test
    @DisplayName("Should retrieve a paginated list of templates for a user")
    void shouldRetrieveAllTemplatesByUserPaged() {
        var page = repository.findAllByUser(savedUser, PageRequest.of(0, 10));
        assertEquals(4, page.getTotalElements(), () -> "Should find all 4 templates for the user");
    }

    @Test
    @DisplayName("Should retrieve a specific template by ID and user")
    void shouldRetrieveTemplateByIdAndUser() {
        Optional<RecurringTransaction> result = repository.findByIdAndUser(dueTemplate.getId(), savedUser);

        assertTrue(result.isPresent(), () -> "Template should be present");
        assertEquals("Netflix", result.get().getDescription(), () -> "Description mismatch");
    }

    @Test
    @DisplayName("Should retrieve only active templates for a user")
    void shouldRetrieveActiveTemplatesByUser() {
        List<RecurringTransaction> active = repository.findByUserAndActive(savedUser, true);
        assertEquals(3, active.size(), () -> "Should find 3 active templates");

        List<RecurringTransaction> paused = repository.findByUserAndActive(savedUser, false);
        assertEquals(1, paused.size(), () -> "Should find 1 paused template");
        assertEquals("Paused subscription", paused.get(0).getDescription(), () -> "Description mismatch");
    }

    @Test
    @DisplayName("Should find due templates: active, not in the future and not past their end date")
    void shouldFindDueTemplates() {
        List<RecurringTransaction> due = repository.findDue(today);

        assertEquals(1, due.size(), () -> "Only the active, due, non-expired template should be returned");
        assertEquals("Netflix", due.get(0).getDescription(), () -> "Description mismatch");
    }

    @Test
    @DisplayName("Should find active templates whose next occurrence falls inside a window")
    void shouldFindActiveTemplatesInWindow() {
        List<RecurringTransaction> inWindow =
                repository.findActiveInWindow(savedUser, today, today.plusDays(30));

        List<String> descriptions = inWindow.stream().map(RecurringTransaction::getDescription).toList();

        assertTrue(descriptions.contains("Netflix"), () -> "Due template should fall inside the window");
        assertTrue(descriptions.contains("Gym"), () -> "Future template within 30 days should be included");
        assertFalse(descriptions.contains("Paused subscription"), () -> "Paused templates must be excluded");
        assertFalse(descriptions.contains("Expired plan"), () -> "Expired templates must be excluded");
    }
}
