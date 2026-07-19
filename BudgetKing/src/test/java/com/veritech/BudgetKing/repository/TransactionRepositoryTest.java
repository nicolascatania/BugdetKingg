package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.dto.IncomeExpenseDTO;
import com.veritech.BudgetKing.dto.MonthlyTransactionReportDTO;
import com.veritech.BudgetKing.enumerator.TransactionType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Transaction Repository Specification")
class TransactionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TransactionRepository repository;

    private LocalDateTime start;
    private LocalDateTime end;
    private BigDecimal expectedIncome;
    private BigDecimal expectedExpense;

    @BeforeEach
    void setUpDefaults() {
        start = LocalDateTime.of(2024, 1, 1, 0, 0);
        end = LocalDateTime.of(2024, 2, 1, 0, 0);
        expectedIncome = new BigDecimal("50.00");
        expectedExpense = new BigDecimal("20.00");
    }

    @Test
    @DisplayName("Should retrieve all transactions for a user")
    void shouldRetrieveAllTransactionsByUser() {
        var transactions = repository.findByUser(savedUser);
        assertEquals(2, transactions.size(), () -> "Should find 2 transactions");
    }

    @Test
    @DisplayName("Should retrieve transactions filtered by type")
    void shouldRetrieveTransactionsByUserAndType() {
        var expenses = repository.findByUserAndType(savedUser, TransactionType.EXPENSE);
        assertEquals(1, expenses.size(), () -> "Should find 1 expense");
        assertEquals("Steam Game", expenses.get(0).getDescription(), () -> "Description mismatch");

        var income = repository.findByUserAndType(savedUser, TransactionType.INCOME);
        assertEquals(1, income.size(), () -> "Should find 1 income");
        assertEquals("Netflix", income.get(0).getDescription(), () -> "Description mismatch");
    }

    @Test
    @DisplayName("Should retrieve specific transaction by ID and user")
    void shouldRetrieveTransactionByIdAndUser() {
        var transaction = repository.findByIdAndUser(savedNetflixTransaction.getId(), savedUser);
        assertTrue(transaction.isPresent(), () -> "Transaction should be present");
        assertEquals(0, expectedIncome.compareTo(transaction.get().getAmount()), () -> "Amount mismatch");
    }

    @Test
    @DisplayName("Should retrieve transactions within a specific date range")
    void shouldRetrieveTransactionsByUserAndDateRange() {
        var results = repository.findByUserAndDateBetween(savedUser, start, end.minusMinutes(1));
        assertEquals(2, results.size(), () -> "Should find both transactions in January");
    }

    @Test
    @DisplayName("Should generate accurate monthly report")
    void shouldGetMonthlyReport() {
        MonthlyTransactionReportDTO report = repository.getMonthlyReport(savedUser, start, end);

        assertNotNull(report, () -> "Report should not be null");
        assertEquals(0, expectedIncome.compareTo(report.income()), () -> "Income mismatch");
        assertEquals(0, expectedExpense.compareTo(report.outcome()), () -> "Outcome mismatch");
    }

    @Test
    @DisplayName("Should retrieve income and expense totals")
    void shouldGetIncomeAndExpenseTotals() {
        IncomeExpenseDTO dto = repository.getIncomeAndExpense(savedUser, start, end);

        assertEquals(0, expectedIncome.compareTo(dto.income()), () -> "Income mismatch");
        assertEquals(0, expectedExpense.compareTo(dto.expense()), () -> "Expense mismatch");
    }

    @Test
    @DisplayName("Should group expenses by category")
    void shouldGetExpensesByCategory() {
        List<Object[]> result = repository.getExpensesByCategory(savedUser, start, end);

        assertFalse(result.isEmpty(), () -> "Result list should not be empty");
        assertEquals("Entertainment", result.get(0)[0], () -> "Category name mismatch");
        assertEquals(0, expectedExpense.compareTo((BigDecimal) result.get(0)[1]), () -> "Amount mismatch");
    }

    @Test
    @DisplayName("Should retrieve income and expense by month")
    void shouldGetIncomeExpenseByMonth() {
        var report = repository.getIncomeExpenseByMonth(savedUser, 2024, null);

        assertFalse(report.isEmpty(), () -> "Report list should not be empty");
        var januaryReport = report.get(0);
        assertEquals(1, januaryReport.month(), () -> "Month mismatch");
        assertEquals(0, expectedIncome.compareTo(januaryReport.income()), () -> "Income mismatch");
        assertEquals(0, expectedExpense.compareTo(januaryReport.expense()), () -> "Expense mismatch");
    }
}