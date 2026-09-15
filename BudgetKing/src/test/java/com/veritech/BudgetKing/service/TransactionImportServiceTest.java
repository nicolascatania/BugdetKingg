package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.ImportPreviewDTO;
import com.veritech.BudgetKing.dto.ImportRowDTO;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.exception.TransactionImportRuntimeException;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.AccountRepository;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private AccountRepository accountRepository;

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

    private static final String HEADER =
            "date,description,amount,type,category,counterparty,account,destination_account\n";

    @BeforeEach
    void setUpDefaults() {
        mockUser = new AppUser();
        accountId = UUID.randomUUID();
        mockAccount = Account.builder().id(accountId).name("Cash").build();

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

    private void mockValidAccount() {
        when(accountRepository.findByNameAndUser("Cash", mockUser)).thenReturn(Optional.of(mockAccount));
    }

    @Test
    @DisplayName("Should mark a well-formed row as valid")
    void shouldPreviewValidRow() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n";

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
        assertEquals(accountId, row.account());
        assertNull(row.destinationAccount());
    }

    @Test
    @DisplayName("Should keep a row valid when its category does not exist yet and flag it as new")
    void shouldFlagNewCategory() {
        when(categoryRepository.getByNameAndUser("Unknown Category", mockUser)).thenReturn(Optional.empty());
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Unknown Category,Cinema,Cash,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.validRows(), () -> "An unknown category must not invalidate the row");
        assertEquals(0, preview.errorRows());
        assertTrue(preview.rows().get(0).newCategory(), () -> "Row must be flagged as creating a category");
        assertEquals(List.of("Unknown Category"), preview.newCategories(), () -> "Preview must list the categories to create");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Should not flag an existing category as new")
    void shouldNotFlagExistingCategory() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertFalse(preview.rows().get(0).newCategory());
        assertTrue(preview.newCategories().isEmpty());
    }

    @Test
    @DisplayName("Should create a missing category once on commit and reuse it for every row naming it")
    void shouldCreateMissingCategoryOnCommit() {
        when(categoryRepository.getByNameAndUser("Gifts", mockUser)).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(transactionService.create(any(TransactionDTO.class))).thenReturn(null);
        mockValidAccount();

        String csv = HEADER
                + "2026-01-15,Birthday,25.50,EXPENSE,Gifts,Shop,Cash,\n"
                + "2026-01-16,Wedding,80,EXPENSE,Gifts,Shop,Cash,\n";

        ImportPreviewDTO result = importService.commit(csvFile(csv));

        assertEquals(2, result.validRows());
        assertEquals(List.of("Gifts"), result.newCategories());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, times(1)).save(captor.capture());
        assertEquals("Gifts", captor.getValue().getName());
        assertEquals(CategoryIconCatalog.DEFAULT_ICON, captor.getValue().getIcon(), () -> "New categories get the default icon");
        assertSame(mockUser, captor.getValue().getUser());
        verify(transactionService, times(2)).create(argThat(dto -> dto.categoryName().equals("Gifts")));
    }

    @Test
    @DisplayName("Should flag a row referencing an account that does not exist for the user")
    void shouldFlagMissingAccount() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(accountRepository.findByNameAndUser("Ghost Account", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema,Ghost Account,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(0, preview.validRows());
        assertEquals(1, preview.errorRows());
        assertNull(preview.rows().get(0).account());
        assertTrue(preview.rows().get(0).errorMessage().contains("Account not found"));
    }

    @Test
    @DisplayName("Should flag a row with a blank account")
    void shouldFlagBlankAccount() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema,,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("Account is mandatory"));
    }

    @Test
    @DisplayName("Should flag a row with a malformed amount")
    void shouldFlagMalformedAmount() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Movie night,not-a-number,EXPENSE,Entertainment,Cinema,Cash,\n";

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
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Movie night,0,EXPENSE,Entertainment,Cinema,Cash,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("Should flag a savings movement row: those only come from the savings goal endpoints")
    void shouldFlagSavingsType() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Vacation share,25.50,SAVINGS_DEPOSIT,Entertainment,Vacation,Cash,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("Savings transactions cannot be imported"));
    }

    @Test
    @DisplayName("Should flag a row with an invalid date")
    void shouldFlagInvalidDate() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        mockValidAccount();

        String csv = HEADER + "not-a-date,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("Invalid date format"));
    }

    @Test
    @DisplayName("Should accept human-readable dates with and without time, plus legacy ISO ones")
    void shouldAcceptHumanReadableDates() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);
        mockValidAccount();

        String csv = HEADER
                + "15/01/2026 10:30,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n"
                + "15/01/2026,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n"
                + "2026-01-15T10:30:00,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n"
                + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(0, preview.errorRows(), () -> "Every date format must be accepted");
        assertEquals("2026-01-15T10:30:00", preview.rows().get(0).date(), () -> "dd/MM/yyyy HH:mm mismatch");
        assertEquals("2026-01-15T00:00:00", preview.rows().get(1).date(), () -> "Bare dd/MM/yyyy must land on midnight");
        assertEquals("2026-01-15T10:30:00", preview.rows().get(2).date(), () -> "ISO date-time mismatch");
        assertEquals("2026-01-15T00:00:00", preview.rows().get(3).date(), () -> "ISO date must land on midnight");
    }

    @Test
    @DisplayName("Should default a blank counterparty to Unknown")
    void shouldDefaultBlankCounterparty() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,,Cash,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals("Unknown", preview.rows().get(0).counterparty());
    }

    @Test
    @DisplayName("Should flag a valid row that already exists as a duplicate, excluded from validRows")
    void shouldFlagDuplicateRow() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(true);
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n";

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
        mockValidAccount();

        // Second row has fewer columns than the header - commons-csv throws on record.get()
        // for the missing column, which the row parser turns into an invalid row.
        String csv = HEADER
                + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n"
                + "2026-01-16,Incomplete row\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(2, preview.totalRows());
        assertEquals(1, preview.validRows());
        assertEquals(1, preview.errorRows());
    }

    @Test
    @DisplayName("Should only persist valid, non-duplicate rows on commit")
    void shouldCommitOnlyValidNonDuplicateRows() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false, true); // first row not duplicate, third row duplicate
        mockValidAccount();

        String csv = HEADER
                + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n"
                + "2026-01-16,Bad amount,oops,EXPENSE,Entertainment,Cinema,Cash,\n"
                + "2026-01-17,Old rent,500,EXPENSE,Entertainment,Landlord,Cash,\n";

        when(transactionService.create(any(TransactionDTO.class))).thenReturn(null);

        ImportPreviewDTO result = importService.commit(csvFile(csv));

        assertEquals(3, result.totalRows());
        assertEquals(1, result.validRows());
        assertEquals(1, result.duplicateRows());
        assertEquals(1, result.errorRows());

        verify(transactionService, times(1)).create(any(TransactionDTO.class));
    }

    @Test
    @DisplayName("Should not persist anything when every row is invalid")
    void shouldNotPersistWhenAllRowsInvalid() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Movie night,not-a-number,EXPENSE,Entertainment,Cinema,Cash,\n";

        ImportPreviewDTO result = importService.commit(csvFile(csv));

        assertEquals(1, result.errorRows());
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("Should spread transactions across different accounts within the same file")
    void shouldResolveAccountPerRow() {
        Account savings = Account.builder().id(UUID.randomUUID()).name("Savings").build();

        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);
        mockValidAccount();
        when(accountRepository.findByNameAndUser("Savings", mockUser)).thenReturn(Optional.of(savings));
        when(transactionService.create(any(TransactionDTO.class))).thenReturn(null);

        String csv = HEADER
                + "2026-01-15,Movie night,25.50,EXPENSE,Entertainment,Cinema,Cash,\n"
                + "2026-01-16,Interest,10,INCOME,Entertainment,Bank,Savings,\n";

        ImportPreviewDTO result = importService.commit(csvFile(csv));

        assertEquals(accountId, result.rows().get(0).account());
        assertEquals(savings.getId(), result.rows().get(1).account());
        verify(transactionService, times(2)).create(any(TransactionDTO.class));
    }

    @Test
    @DisplayName("Should include the line number pointing to the physical CSV line of each row")
    void shouldTrackLineNumbers() {
        when(categoryRepository.getByNameAndUser("Entertainment", mockUser)).thenReturn(Optional.of(mockCategory));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);
        mockValidAccount();

        String csv = HEADER
                + "2026-01-15,First,10,EXPENSE,Entertainment,A,Cash,\n"
                + "2026-01-16,Second,20,EXPENSE,Entertainment,B,Cash,\n";

        List<ImportRowDTO> rows = importService.preview(csvFile(csv)).rows();

        assertEquals(2, rows.get(0).lineNumber());
        assertEquals(3, rows.get(1).lineNumber());
    }

    // --- TRANSFER-specific behaviour -----------------------------------------------------

    @Test
    @DisplayName("Should import a TRANSFER row with a valid destination account and no category")
    void shouldImportValidTransfer() {
        Account savings = Account.builder().id(UUID.randomUUID()).name("Savings").build();

        mockValidAccount();
        when(accountRepository.findByNameAndUser("Savings", mockUser)).thenReturn(Optional.of(savings));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);

        String csv = HEADER + "2026-01-15,Move to savings,500,TRANSFER,,Self,Cash,Savings\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.validRows());
        assertEquals(0, preview.errorRows());
        ImportRowDTO row = preview.rows().get(0);
        assertEquals(accountId, row.account());
        assertEquals(savings.getId(), row.destinationAccount());
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("Should flag a TRANSFER row with no destination account")
    void shouldFlagTransferWithoutDestination() {
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Move to savings,500,TRANSFER,,Self,Cash,\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("Destination account is mandatory"));
    }

    @Test
    @DisplayName("Should flag a TRANSFER row whose destination account does not exist")
    void shouldFlagTransferWithMissingDestination() {
        mockValidAccount();
        when(accountRepository.findByNameAndUser("Ghost", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER + "2026-01-15,Move to savings,500,TRANSFER,,Self,Cash,Ghost\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("Destination account not found"));
    }

    @Test
    @DisplayName("Should flag a TRANSFER row whose source and destination accounts are the same")
    void shouldFlagTransferWithSameSourceAndDestination() {
        mockValidAccount();

        String csv = HEADER + "2026-01-15,Move to savings,500,TRANSFER,,Self,Cash,Cash\n";

        ImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("must be different"));
    }

    @Test
    @DisplayName("Should persist a TRANSFER row on commit with the resolved destination account")
    void shouldCommitTransferRow() {
        Account savings = Account.builder().id(UUID.randomUUID()).name("Savings").build();

        mockValidAccount();
        when(accountRepository.findByNameAndUser("Savings", mockUser)).thenReturn(Optional.of(savings));
        when(transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(transactionService.create(any(TransactionDTO.class))).thenReturn(null);

        String csv = HEADER + "2026-01-15,Move to savings,500,TRANSFER,,Self,Cash,Savings\n";

        ImportPreviewDTO result = importService.commit(csvFile(csv));

        assertEquals(1, result.validRows());
        verify(transactionService).create(argThat(dto ->
                dto.account().equals(accountId) && dto.destinationAccount().equals(savings.getId())
                        && dto.category() == null));
    }
}
