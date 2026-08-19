package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.ImportPreviewDTO;
import com.veritech.BudgetKing.dto.ImportRowDTO;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.exception.TransactionImportRuntimeException;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Import Service Specification")
class TransactionImportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private TransactionImportService importService;

    private AppUser mockUser;
    private Category mockCategory;
    private Account mockAccount;
    private UUID accountId;

    private static final String HEADER = "date,description,amount,type,category,counterparty\n";

    @BeforeEach
    void setUpDefaults() {
        mockUser = new AppUser();
        accountId = UUID.randomUUID();
        mockAccount = Account.builder().id(accountId).build();

        mockCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Entertainment")
                .user(mockUser)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "transactions.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should mark a well-formed row as valid")
    void shouldPreviewValidRow() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.totalRows());
        assertEquals(1, preview.validRows());
        assertEquals(0, preview.duplicateRows());
        assertEquals(0, preview.errorRows());

        ImportRowDTO row = preview.rows().get(0);
        assertTrue(row.valid());
        assertNull(row.errorMessage());
        assertEquals(new BigDecimal("25.50"), row.amount());
        assertEquals("EXPENSE", row.type());
    }

    @Test
    @DisplayName("Should flag a row referencing a category that does not exist for the user")
    void shouldFlagMissingCategory() {
        when(categoryRepository.getByNameAndUser("Unknown Category", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Unknown Category,Cinema\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(0, preview.validRows());
        assertEquals(1, preview.errorRows());
        assertFalse(preview.rows().get(0).valid());
        assertTrue(preview.rows().get(0).errorMessage().contains("Category not found"));
    }

    @Test
    @DisplayName("Should flag a row with a malformed amount")
    void shouldFlagMalformedAmount() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));

        String csv = HEADER + "2026-01-15,Movie night,not-a-number,EXPENSE,Entertainment,Cinema\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        ImportRowDTO row = preview.rows().get(0);
        assertFalse(row.valid());
        assertNull(row.amount());
        assertTrue(row.errorMessage().contains("Invalid amount format"));
    }

    @Test
    @DisplayName("Should flag a row with a non-positive amount")
    void shouldFlagNonPositiveAmount() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));

        String csv = HEADER + "2026-01-15,Movie night,0,EXPENSE,Entertainment,Cinema\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("Should flag a row with an invalid date")
    void shouldFlagInvalidDate() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));

        String csv = HEADER + "not-a-date,Movie night,25.50,EXPENSE,Entertainment,Cinema\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("Invalid date format"));
    }

    @Test
    @DisplayName("Should reject TRANSFER rows since they cannot be imported")
    void shouldRejectTransferType() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));

        String csv = HEADER + "2026-01-15,Movie night,25.50,TRANSFER,Entertainment,Cinema\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("TRANSFER"));
    }

    @Test
    @DisplayName("Should default a blank counterparty to Unknown")
    void shouldDefaultBlankCounterparty() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals("Unknown", preview.rows().get(0).counterparty());
    }

    @Test
    @DisplayName("Should flag a valid row that already exists as a duplicate, excluded from validRows")
    void shouldFlagDuplicateRow() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(true);

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(0, preview.validRows());
        assertEquals(1, preview.duplicateRows());
        assertEquals(0, preview.errorRows());
        ImportRowDTO row = preview.rows().get(0);
        assertTrue(row.valid());
        assertTrue(row.duplicate());
    }

    @Test
    @DisplayName("Should throw when the CSV file is empty")
    void shouldThrowOnEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThrows(TransactionImportRuntimeException.class, () -> importService.preview(empty));
    }

    @Test
    @DisplayName("Should throw when the CSV header does not match the expected columns")
    void shouldThrowOnInvalidHeader() {
        String csv = "foo,bar,baz\n1,2,3\n";

        assertThrows(TransactionImportRuntimeException.class, () -> importService.preview(csvFile(csv)));
    }

    @Test
    @DisplayName("Should throw when the file has a valid header but no data rows")
    void shouldThrowWhenNoDataRows() {
        assertThrows(TransactionImportRuntimeException.class, () -> importService.preview(csvFile(HEADER)));
    }

    @Test
    @DisplayName("Should not abort the whole import when a single physical row is malformed")
    void shouldFlagMalformedRowWithoutAbortingImport() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);

        // Second row has fewer columns than the header - commons-csv throws on record.get()
        // for the missing column, which the row parser turns into an invalid row.
        String csv = HEADER
                + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema\n"
                + "2026-01-16,Incomplete row\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(2, preview.totalRows());
        assertEquals(1, preview.validRows());
        assertEquals(1, preview.errorRows());
    }

    @Test
    @DisplayName("Should only persist valid, non-duplicate rows on commit")
    void shouldCommitOnlyValidNonDuplicateRows() {
        when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false, true); // first row not duplicate, third row duplicate

        String csv = HEADER
                + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema\n"
                + "2026-01-16,Bad amount,oops,EXPENSE,Entertainment,Cinema\n"
                + "2026-01-17,Old rent,500,EXPENSE,Entertainment,Landlord\n";

        when(transactionService.create(any(TransactionDTO.class))).thenReturn(null);

        ImportPreviewDTO result = importService.commit(csvFile(csv), accountId);

        assertEquals(3, result.totalRows());
        assertEquals(1, result.validRows());
        assertEquals(1, result.duplicateRows());
        assertEquals(1, result.errorRows());

        verify(transactionService, times(1)).create(any(TransactionDTO.class));
    }

    @Test
    @DisplayName("Should not persist anything when every row is invalid")
    void shouldNotPersistWhenAllRowsInvalid() {
        when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
        when(categoryRepository.getByNameAndUser("Ghost", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Ghost,Cinema\n";

        ImportPreviewDTO result = importService.commit(csvFile(csv), accountId);

        assertEquals(1, result.errorRows());
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("Should include the line number pointing to the physical CSV line of each row")
    void shouldTrackLineNumbers() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);

        String csv = HEADER
                + "2026-01-15,First,10,EXPENSE,Entertainment,A\n"
                + "2026-01-16,Second,20,EXPENSE,Entertainment,B\n";

        List<ImportRowDTO> rows = importService.preview(csvFile(csv)).rows();

        assertEquals(2, rows.get(0).lineNumber());
        assertEquals(3, rows.get(1).lineNumber());
    }
}
