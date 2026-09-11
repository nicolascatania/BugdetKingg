package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.AccountRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Export Service Specification")
class AccountExportServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AccountExportService exportService;

    private AppUser mockUser;

    @BeforeEach
    void setUpDefaults() {
        mockUser = new AppUser();
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    @DisplayName("Should export the user's accounts with header and one row per account")
    void shouldExportAccountsToCsv() {
        Account account = Account.builder().name("Cash").description("Wallet money").icon("fa-wallet").build();
        when(accountRepository.findByUser(mockUser)).thenReturn(List.of(account));

        byte[] result = exportService.exportToCsv();
        String csv = new String(result, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("name,description,icon"));
        assertTrue(csv.contains("Cash"));
        assertTrue(csv.contains("Wallet money"));
        assertTrue(csv.contains("fa-wallet"));
    }

    @Test
    @DisplayName("Should produce a header-only file when the user has no accounts")
    void shouldExportEmptyList() {
        when(accountRepository.findByUser(mockUser)).thenReturn(List.of());

        byte[] result = exportService.exportToCsv();
        String csv = new String(result, StandardCharsets.UTF_8);

        assertEquals("name,description,icon\r\n", csv);
    }
}
