package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.filter.TransactionFilter;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Export Service Specification")
class TransactionExportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private TransactionExportService exportService;

    private AppUser mockUser;
    private Transaction mockTransaction;

    @BeforeEach
    void setUpDefaults() {
        mockUser = new AppUser();

        Category category = Category.builder().name("Groceries").build();
        Account account = Account.builder().name("Main").build();

        mockTransaction = Transaction.builder()
                .date(LocalDateTime.of(2026, 1, 15, 10, 30))
                .amount(new BigDecimal("42.90"))
                .type(TransactionType.EXPENSE)
                .description("Supermarket")
                .counterparty("Local Store")
                .category(category)
                .account(account)
                .user(mockUser)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    @DisplayName("Should export the user's transactions with header and one row per transaction")
    void shouldExportTransactionsToCsv() {
        TransactionFilter filter = new TransactionFilter();
        when(transactionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(mockTransaction));

        byte[] result = exportService.exportToCsv(filter);
        String csv = new String(result, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("date,description,amount,type,category,counterparty,account"));
        assertTrue(csv.contains("2026-01-15T10:30"));
        assertTrue(csv.contains("Supermarket"));
        assertTrue(csv.contains("42.9"));
        assertTrue(csv.contains("EXPENSE"));
        assertTrue(csv.contains("Groceries"));
        assertTrue(csv.contains("Local Store"));
        assertTrue(csv.contains("Main"));
    }

    @Test
    @DisplayName("Should export every transaction of the user when no filter is given")
    void shouldExportAllTransactionsWithoutFilter() {
        when(transactionRepository.findByUser(mockUser)).thenReturn(List.of(mockTransaction));

        byte[] result = exportService.exportToCsv(null);
        String csv = new String(result, StandardCharsets.UTF_8);

        assertTrue(csv.contains("Supermarket"));
        verify(transactionRepository).findByUser(mockUser);
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    @DisplayName("Should produce a header-only file when the user has no transactions")
    void shouldExportEmptyList() {
        TransactionFilter filter = new TransactionFilter();
        when(transactionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        byte[] result = exportService.exportToCsv(filter);
        String csv = new String(result, StandardCharsets.UTF_8);

        assertEquals("date,description,amount,type,category,counterparty,account\r\n", csv);
    }

    @Test
    @DisplayName("Should default category to blank when the transaction has none")
    void shouldExportTransferWithoutCategory() {
        Transaction transfer = Transaction.builder()
                .date(LocalDateTime.of(2026, 2, 1, 0, 0))
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.TRANSFER)
                .description("Move funds")
                .counterparty("Self")
                .account(Account.builder().name("Main").build())
                .user(mockUser)
                .build();

        when(transactionRepository.findByUser(mockUser)).thenReturn(List.of(transfer));

        byte[] result = exportService.exportToCsv(null);
        String csv = new String(result, StandardCharsets.UTF_8);

        assertTrue(csv.contains("Move funds"));
        assertTrue(csv.contains("TRANSFER"));
    }
}
