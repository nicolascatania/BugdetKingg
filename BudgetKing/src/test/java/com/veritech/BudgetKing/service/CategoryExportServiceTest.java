package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.CategoryRepository;
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
@DisplayName("Category Export Service Specification")
class CategoryExportServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CategoryExportService exportService;

    private AppUser mockUser;

    @BeforeEach
    void setUpDefaults() {
        mockUser = new AppUser();
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    @DisplayName("Should export the user's categories with header and one row per category")
    void shouldExportCategoriesToCsv() {
        Category category = Category.builder().name("Groceries").icon("fa-basket-shopping").build();
        when(categoryRepository.findByUser(mockUser)).thenReturn(List.of(category));

        byte[] result = exportService.exportToCsv();
        String csv = new String(result, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("name,icon"));
        assertTrue(csv.contains("Groceries"));
        assertTrue(csv.contains("fa-basket-shopping"));
    }

    @Test
    @DisplayName("Should produce a header-only file when the user has no categories")
    void shouldExportEmptyList() {
        when(categoryRepository.findByUser(mockUser)).thenReturn(List.of());

        byte[] result = exportService.exportToCsv();
        String csv = new String(result, StandardCharsets.UTF_8);

        assertEquals("name,icon\r\n", csv);
    }
}
