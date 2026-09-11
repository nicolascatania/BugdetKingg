package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.AccountDTO;
import com.veritech.BudgetKing.dto.AccountImportPreviewDTO;
import com.veritech.BudgetKing.dto.AccountImportRowDTO;
import com.veritech.BudgetKing.exception.AccountImportRuntimeException;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.AccountRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Import Service Specification")
class AccountImportServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AccountImportService importService;

    private AppUser mockUser;

    private static final String HEADER = "name,description,icon\n";

    @BeforeEach
    void setUpDefaults() {
        mockUser = new AppUser();
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "accounts.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should mark a well-formed row as valid and keep the given icon")
    void shouldPreviewValidRow() {
        when(accountRepository.findByNameAndUser("Cash", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER + "Cash,Wallet money,fa-wallet\n";

        AccountImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.totalRows());
        assertEquals(1, preview.validRows());
        assertEquals(0, preview.duplicateRows());
        assertEquals(0, preview.errorRows());

        AccountImportRowDTO row = preview.rows().get(0);
        assertTrue(row.valid());
        assertNull(row.errorMessage());
        assertEquals("fa-wallet", row.icon());
    }

    @Test
    @DisplayName("Should default an unrecognised icon to the bank icon without invalidating the row")
    void shouldDefaultUnknownIcon() {
        when(accountRepository.findByNameAndUser("Cash", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER + "Cash,Wallet money,fa-not-a-real-icon\n";

        AccountImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.validRows());
        assertEquals(0, preview.errorRows());
        assertEquals(AccountIconCatalog.DEFAULT_ICON, preview.rows().get(0).icon());
    }

    @Test
    @DisplayName("Should default a blank icon to the bank icon without invalidating the row")
    void shouldDefaultBlankIcon() {
        when(accountRepository.findByNameAndUser("Cash", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER + "Cash,Wallet money,\n";

        AccountImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.validRows());
        assertEquals(AccountIconCatalog.DEFAULT_ICON, preview.rows().get(0).icon());
        assertEquals("fa-building-columns", preview.rows().get(0).icon());
    }

    @Test
    @DisplayName("Should flag a row with a blank name")
    void shouldFlagBlankName() {
        String csv = HEADER + ",Wallet money,fa-wallet\n";

        AccountImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("Name is mandatory"));
    }

    @Test
    @DisplayName("Should flag a row with a blank description")
    void shouldFlagBlankDescription() {
        String csv = HEADER + "Cash,,fa-wallet\n";

        AccountImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("Description is mandatory"));
    }

    @Test
    @DisplayName("Should flag a row whose name already exists for the user as a duplicate")
    void shouldFlagDuplicateRow() {
        Account existing = Account.builder().id(UUID.randomUUID()).name("Cash").user(mockUser).build();
        when(accountRepository.findByNameAndUser("Cash", mockUser)).thenReturn(Optional.of(existing));

        String csv = HEADER + "Cash,Wallet money,fa-wallet\n";

        AccountImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(0, preview.validRows());
        assertEquals(1, preview.duplicateRows());
        assertEquals(0, preview.errorRows());
        assertTrue(preview.rows().get(0).duplicate());
    }

    @Test
    @DisplayName("Should throw when the CSV file is empty")
    void shouldThrowOnEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThrows(AccountImportRuntimeException.class, () -> importService.preview(empty));
    }

    @Test
    @DisplayName("Should throw when the CSV header does not match the expected columns")
    void shouldThrowOnInvalidHeader() {
        String csv = "foo,bar,baz\n1,2,3\n";

        assertThrows(AccountImportRuntimeException.class, () -> importService.preview(csvFile(csv)));
    }

    @Test
    @DisplayName("Should throw when the file has a valid header but no data rows")
    void shouldThrowWhenNoDataRows() {
        assertThrows(AccountImportRuntimeException.class, () -> importService.preview(csvFile(HEADER)));
    }

    @Test
    @DisplayName("Should only persist valid, non-duplicate rows on commit, always starting at zero balance")
    void shouldCommitOnlyValidNonDuplicateRows() {
        when(accountRepository.findByNameAndUser("Cash", mockUser)).thenReturn(Optional.empty());
        when(accountRepository.findByNameAndUser("Savings", mockUser))
                .thenReturn(Optional.of(Account.builder().id(UUID.randomUUID()).name("Savings").build()));

        String csv = HEADER
                + "Cash,Wallet money,fa-wallet\n"
                + "Savings,Already exists,fa-piggy-bank\n"
                + ",Blank name,fa-coins\n";

        when(accountService.create(any(AccountDTO.class))).thenReturn(null);

        AccountImportPreviewDTO result = importService.commit(csvFile(csv));

        assertEquals(3, result.totalRows());
        assertEquals(1, result.validRows());
        assertEquals(1, result.duplicateRows());
        assertEquals(1, result.errorRows());

        verify(accountService, times(1)).create(argThat(dto ->
                dto.name().equals("Cash") && dto.balance().signum() == 0));
    }

    @Test
    @DisplayName("Should not abort the whole import when a single physical row is malformed")
    void shouldFlagMalformedRowWithoutAbortingImport() {
        when(accountRepository.findByNameAndUser("Cash", mockUser)).thenReturn(Optional.empty());

        // Second row has fewer columns than the header - commons-csv throws on record.get()
        // for the missing column, which the row parser turns into an invalid row.
        String csv = HEADER
                + "Cash,Wallet money,fa-wallet\n"
                + "Incomplete\n";

        AccountImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(2, preview.totalRows());
        assertEquals(1, preview.validRows());
        assertEquals(1, preview.errorRows());
    }

    @Test
    @DisplayName("Should include the line number pointing to the physical CSV line of each row")
    void shouldTrackLineNumbers() {
        when(accountRepository.findByNameAndUser("Cash", mockUser)).thenReturn(Optional.empty());
        when(accountRepository.findByNameAndUser("Savings", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER
                + "Cash,Wallet money,fa-wallet\n"
                + "Savings,Rainy day fund,fa-piggy-bank\n";

        List<AccountImportRowDTO> rows = importService.preview(csvFile(csv)).rows();

        assertEquals(2, rows.get(0).lineNumber());
        assertEquals(3, rows.get(1).lineNumber());
    }
}
