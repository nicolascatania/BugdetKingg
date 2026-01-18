package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.dto.IncomeExpenseDTO;
import com.veritech.BudgetKing.dto.MonthlyTransactionReportDTO;
import com.veritech.BudgetKing.enumerator.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest
class TransactionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TransactionRepository repository;

    @Test
    void findByUser() {
        var transactions = repository.findByUser(savedUser);
        assertEquals(2, transactions.size());
    }

    @Test
    void findByUserAndType() {
        var expenses = repository.findByUserAndType(savedUser, TransactionType.EXPENSE);
        assertEquals(1, expenses.size());
        assertEquals("Steam Game", expenses.get(0).getDescription());

        var income = repository.findByUserAndType(savedUser, TransactionType.INCOME);
        assertEquals(1, income.size());
        assertEquals("Netflix", income.get(0).getDescription());
    }

    @Test
    void findByIdAndUser() {
        var transaction = repository.findByIdAndUser(savedNetflixTransaction.getId(), savedUser);
        assertTrue(transaction.isPresent());
        assertEquals(new BigDecimal("50.00"), transaction.get().getAmount());
    }

    @Test
    void findByUserAndDateBetween() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);

        var results = repository.findByUserAndDateBetween(savedUser, start, end);
        assertEquals(2, results.size(), "Should find both transactions in January");
    }

    @Test
    void getMonthlyReport() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 2, 1, 0, 0);

        MonthlyTransactionReportDTO report = repository.getMonthlyReport(savedUser, start, end);

        assertNotNull(report);
        assertEquals(0, new BigDecimal("50.00").compareTo(report.income()));
        assertEquals(0, new BigDecimal("20.00").compareTo(report.outcome()));
    }

    @Test
    void getIncomeAndExpense() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 2, 1, 0, 0);

        IncomeExpenseDTO dto = repository.getIncomeAndExpense(savedUser, start, end);

        assertEquals(0, new BigDecimal("50.00").compareTo(dto.income()));
        assertEquals(0, new BigDecimal("20.00").compareTo(dto.expense()));
    }

    @Test
    void getExpensesByCategory() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 2, 1, 0, 0);

        List<Object[]> result = repository.getExpensesByCategory(savedUser, start, end);

        assertFalse(result.isEmpty());
        assertEquals("Entertainment", result.get(0)[0]);
        assertEquals(0, new BigDecimal("20.00").compareTo((BigDecimal) result.get(0)[1]));
    }

    @Test
    void getIncomeExpenseByMonth() {
        var report = repository.getIncomeExpenseByMonth(savedUser, 2024, null);

        assertFalse(report.isEmpty());
        var januaryReport = report.get(0);
        assertEquals(1, januaryReport.month()); // Enero
        assertEquals(0, new BigDecimal("50.00").compareTo(januaryReport.income()));
        assertEquals(0, new BigDecimal("20.00").compareTo(januaryReport.expense()));
    }
}