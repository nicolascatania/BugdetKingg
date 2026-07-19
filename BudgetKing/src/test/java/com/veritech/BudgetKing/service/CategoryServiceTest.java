package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.dto.OptionDTO;
import com.veritech.BudgetKing.exception.CategoryRuntimeException;
import com.veritech.BudgetKing.filter.CategoryFilter;
import com.veritech.BudgetKing.mapper.CategoryMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Category Service Specification")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private AppUser mockUser;
    private Category mockCategory;
    private CategoryDTO mockDto;
    private UUID categoryId;
    private final String categoryName = "Entertainment";

    @BeforeEach
    void setUpDefaults() {
        categoryId = UUID.randomUUID();
        mockUser = new AppUser();

        mockCategory = Category.builder()
                .id(categoryId)
                .name(categoryName)
                .user(mockUser)
                .build();

        mockDto = new CategoryDTO(categoryId, categoryName, "fa fas-circle-info");
    }

    @Test
    @DisplayName("Should retrieve category successfully by ID")
    void shouldGetByIdSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.of(mockCategory));
        when(categoryMapper.toDto(mockCategory)).thenReturn(mockDto);

        CategoryDTO result = categoryService.getById(categoryId);

        assertNotNull(result, () -> "Result should not be null");
        assertEquals(categoryName, result.name(), () -> "Category name mismatch");
        verify(categoryRepository).findByIdAndUser(categoryId, mockUser);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when category does not exist")
    void shouldThrowExceptionWhenNotFound() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> categoryService.getById(categoryId),
                () -> "Should throw EntityNotFoundException for non-existent category");
    }

    @Test
    @DisplayName("Should create category successfully")
    void shouldCreateCategorySuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryMapper.toEntity(any(), any())).thenReturn(mockCategory);
        when(categoryRepository.save(any(Category.class))).thenReturn(mockCategory);
        when(categoryMapper.toDto(mockCategory)).thenReturn(mockDto);

        CategoryDTO result = categoryService.create(mockDto);

        assertNotNull(result, () -> "Result DTO should not be null");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should update category details successfully")
    void shouldUpdateCategorySuccessfully() {
        CategoryDTO updateDto = new CategoryDTO(categoryId, "New Name", "fa fas-circle-info");
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.of(mockCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(mockCategory);
        when(categoryMapper.toDto(any())).thenReturn(updateDto);

        CategoryDTO result = categoryService.update(categoryId, updateDto);

        assertNotNull(result, () -> "Updated result should not be null");
        assertEquals("New Name", result.name(), () -> "Category name should be updated");
        verify(categoryRepository).save(mockCategory);
    }

    @Test
    @DisplayName("Should delete category when no transactions are associated and there are multiple categories")
    void shouldDeleteCategorySuccessfully() {
        // Arrange
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.of(mockCategory));
        when(categoryRepository.countTransactionsByCategory(categoryId, mockUser)).thenReturn(0L);
        // Simulate that there is more than one category
        when(categoryRepository.countByUser(mockUser)).thenReturn(2L);

        // Act & Assert
        assertDoesNotThrow(() -> categoryService.deleteById(categoryId),
                () -> "Deletion should be allowed when there is more than one category");
        verify(categoryRepository).delete(mockCategory);
    }

    @Test
    @DisplayName("Should throw exception when attempting to delete the last remaining category")
    void shouldThrowExceptionWhenDeletingLastCategory() {
        // Arrange
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.of(mockCategory));
        // Simulate that this is the only category left
        when(categoryRepository.countByUser(mockUser)).thenReturn(1L);

        // Act & Assert
        CategoryRuntimeException exception = assertThrows(CategoryRuntimeException.class,
                () -> categoryService.deleteById(categoryId),
                () -> "Should throw exception when deleting the last category");

        assertEquals("Can`t delete this category, you have only one, create new ones and then delete this.",
                exception.getMessage());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting category with existing transactions")
    void shouldThrowExceptionOnDeleteWithTransactions() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.of(mockCategory));
        when(categoryRepository.countTransactionsByCategory(categoryId, mockUser)).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> categoryService.deleteById(categoryId),
                () -> "Should throw exception when category has active transactions");
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Should return paginated search results")
    void shouldSearchSuccessfully() {
        CategoryFilter filter = new CategoryFilter(categoryName, 0, 10);
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        Page<Category> categoryPage = new PageImpl<>(List.of(mockCategory), expectedPageable, 1);

        when(categoryRepository.findByUser(eq(mockUser), any(Pageable.class))).thenReturn(categoryPage);
        when(categoryMapper.toDto(mockCategory)).thenReturn(mockDto);

        Page<CategoryDTO> result = categoryService.search(filter);

        assertNotNull(result, () -> "Page result should not be null");
        assertEquals(1, result.getTotalElements(), () -> "Total elements mismatch");
        verify(categoryRepository).findByUser(eq(mockUser), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return list of category options")
    void shouldGetOptionsSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByUser(mockUser)).thenReturn(List.of(mockCategory));

        List<OptionDTO> result = categoryService.getOptions();

        assertNotNull(result, () -> "Options list should not be null");
        assertEquals(1, result.size(), () -> "Options size mismatch");
        assertEquals(categoryName, result.get(0).value(), () -> "Option value mismatch");
        verify(categoryRepository).findByUser(mockUser);
        verifyNoInteractions(categoryMapper);
    }
}