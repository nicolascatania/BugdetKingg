package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.dto.CategoryImportPreviewDTO;
import com.veritech.BudgetKing.dto.CategoryImportRowDTO;
import com.veritech.BudgetKing.exception.CategoryImportRuntimeException;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.CategoryRepository;
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
@DisplayName("Category Import Service Specification")
class CategoryImportServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CategoryImportService importService;

    private AppUser mockUser;

    private static final String HEADER = "name,icon\n";

    @BeforeEach
    void setUpDefaults() {
        mockUser = new AppUser();
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "categories.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should mark a well-formed row as valid and keep the given icon")
    void shouldPreviewValidRow() {
        when(categoryRepository.getByNameAndUser("Groceries", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER + "Groceries,fa-basket-shopping\n";

        CategoryImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.totalRows());
        assertEquals(1, preview.validRows());
        assertEquals(0, preview.duplicateRows());
        assertEquals(0, preview.errorRows());

        CategoryImportRowDTO row = preview.rows().get(0);
        assertTrue(row.valid());
        assertNull(row.errorMessage());
        assertEquals("fa-basket-shopping", row.icon());
    }

    @Test
    @DisplayName("Should default an unrecognised icon to fa-tags without invalidating the row")
    void shouldDefaultUnknownIcon() {
        when(categoryRepository.getByNameAndUser("Groceries", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER + "Groceries,fa-not-a-real-icon\n";

        CategoryImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.validRows());
        assertEquals(0, preview.errorRows());
        assertEquals(CategoryIconCatalog.DEFAULT_ICON, preview.rows().get(0).icon());
    }

    @Test
    @DisplayName("Should default a blank icon to fa-tags without invalidating the row")
    void shouldDefaultBlankIcon() {
        when(categoryRepository.getByNameAndUser("Groceries", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER + "Groceries,\n";

        CategoryImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.validRows());
        assertEquals(CategoryIconCatalog.DEFAULT_ICON, preview.rows().get(0).icon());
    }

    @Test
    @DisplayName("Should flag a row with a blank name")
    void shouldFlagBlankName() {
        String csv = HEADER + ",fa-tags\n";

        CategoryImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).errorMessage().contains("Name is mandatory"));
    }

    @Test
    @DisplayName("Should flag a row whose name already exists for the user as a duplicate")
    void shouldFlagDuplicateRow() {
        Category existing = Category.builder().id(UUID.randomUUID()).name("Groceries").user(mockUser).build();
        when(categoryRepository.getByNameAndUser("Groceries", mockUser)).thenReturn(Optional.of(existing));

        String csv = HEADER + "Groceries,fa-basket-shopping\n";

        CategoryImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(0, preview.validRows());
        assertEquals(1, preview.duplicateRows());
        assertEquals(0, preview.errorRows());
        assertTrue(preview.rows().get(0).duplicate());
    }

    @Test
    @DisplayName("Should throw when the CSV file is empty")
    void shouldThrowOnEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThrows(CategoryImportRuntimeException.class, () -> importService.preview(empty));
    }

    @Test
    @DisplayName("Should throw when the CSV header does not match the expected columns")
    void shouldThrowOnInvalidHeader() {
        String csv = "foo,bar\n1,2\n";

        assertThrows(CategoryImportRuntimeException.class, () -> importService.preview(csvFile(csv)));
    }

    @Test
    @DisplayName("Should throw when the file has a valid header but no data rows")
    void shouldThrowWhenNoDataRows() {
        assertThrows(CategoryImportRuntimeException.class, () -> importService.preview(csvFile(HEADER)));
    }

    @Test
    @DisplayName("Should only persist valid, non-duplicate rows on commit")
    void shouldCommitOnlyValidNonDuplicateRows() {
        when(categoryRepository.getByNameAndUser("Groceries", mockUser)).thenReturn(Optional.empty());
        when(categoryRepository.getByNameAndUser("Rent", mockUser))
                .thenReturn(Optional.of(Category.builder().id(UUID.randomUUID()).name("Rent").build()));
        when(categoryRepository.getByNameAndUser("Fun", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER
                + "Groceries,fa-basket-shopping\n"
                + "Rent,fa-house\n"
                + ",fa-tags\n"
                + "Fun,fa-gamepad\n";

        when(categoryService.create(any(CategoryDTO.class))).thenReturn(null);

        CategoryImportPreviewDTO result = importService.commit(csvFile(csv));

        assertEquals(4, result.totalRows());
        assertEquals(2, result.validRows());
        assertEquals(1, result.duplicateRows());
        assertEquals(1, result.errorRows());

        verify(categoryService, times(2)).create(any(CategoryDTO.class));
    }

    @Test
    @DisplayName("Should not abort the whole import when a single physical row is malformed")
    void shouldFlagMalformedRowWithoutAbortingImport() {
        when(categoryRepository.getByNameAndUser("Groceries", mockUser)).thenReturn(Optional.empty());

        // Second row has fewer columns than the header - commons-csv throws on record.get()
        // for the missing column, which the row parser turns into an invalid row.
        String csv = HEADER
                + "Groceries,fa-basket-shopping\n"
                + "Incomplete\n";

        CategoryImportPreviewDTO preview = importService.preview(csvFile(csv));

        assertEquals(2, preview.totalRows());
        assertEquals(1, preview.validRows());
        assertEquals(1, preview.errorRows());
    }

    @Test
    @DisplayName("Should include the line number pointing to the physical CSV line of each row")
    void shouldTrackLineNumbers() {
        when(categoryRepository.getByNameAndUser("Groceries", mockUser)).thenReturn(Optional.empty());
        when(categoryRepository.getByNameAndUser("Rent", mockUser)).thenReturn(Optional.empty());

        String csv = HEADER
                + "Groceries,fa-basket-shopping\n"
                + "Rent,fa-house\n";

        List<CategoryImportRowDTO> rows = importService.preview(csvFile(csv)).rows();

        assertEquals(2, rows.get(0).lineNumber());
        assertEquals(3, rows.get(1).lineNumber());
    }
}
