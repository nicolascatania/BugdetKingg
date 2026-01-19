package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.dto.OptionDTO;
import com.veritech.BudgetKing.filter.CategoryFilter;
import com.veritech.BudgetKing.mapper.CategoryMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.BaseRepositoryTest;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.times;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest{


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

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        mockUser = new AppUser();

        mockCategory = Category.builder()
                .id(categoryId)
                .name("Entertainment")
                .user(mockUser)
                .build();

        mockDto = new CategoryDTO(categoryId, "Entertainment");
    }


    @Test
    void getById_Success() {
        // Arrange
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.of(mockCategory));
        when(categoryMapper.toDto(mockCategory)).thenReturn(mockDto);

        // Act
        CategoryDTO result = categoryService.getById(categoryId);

        // Assert
        assertNotNull(result);
        assertEquals("Entertainment", result.name());
        verify(categoryRepository).findByIdAndUser(categoryId, mockUser);
    }

    @Test
    void getById_NotFound_ThrowsException() {
        // Arrange
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> categoryService.getById(categoryId));
    }


    @Test
    void create_Success() {
        // Arrange
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryMapper.toEntity(any(), any())).thenReturn(mockCategory);
        when(categoryRepository.save(any(Category.class))).thenReturn(mockCategory);
        when(categoryMapper.toDto(mockCategory)).thenReturn(mockDto);

        // Act
        CategoryDTO result = categoryService.create(mockDto);

        // Assert
        assertNotNull(result);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void update_Success() {
        CategoryDTO updateDto = new CategoryDTO(categoryId, "New Name");
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.of(mockCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(mockCategory);
        when(categoryMapper.toDto(any())).thenReturn(updateDto);

        CategoryDTO result = categoryService.update(categoryId, updateDto);

        assertNotNull(result);
        assertEquals("New Name", result.name());

        verify(categoryRepository).save(mockCategory);
    }

    @Test
    void deleteById_Success() {
        // Arrange
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.of(mockCategory));
        when(categoryRepository.countTransactionsByCategory(categoryId, mockUser)).thenReturn(0L);
        when(categoryRepository.countByUser(mockUser)).thenReturn(5L);

        // Act
        assertDoesNotThrow(() -> categoryService.deleteById(categoryId));

        // Assert
        verify(categoryRepository).delete(mockCategory);
    }

    @Test
    void deleteById_HasTransactions_ThrowsException() {
        // Arrange
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(Optional.of(mockCategory));
        when(categoryRepository.countTransactionsByCategory(categoryId, mockUser)).thenReturn(1L);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> categoryService.deleteById(categoryId));
        verify(categoryRepository, Mockito.never()).delete((Category) any());
    }

    @Test
    void search_Success() {

        CategoryFilter filter = new CategoryFilter("Entertainment", 0, 10);

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);

        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("name").ascending());


        List<Category> categoryList = List.of(mockCategory);
        Page<Category> categoryPage = new PageImpl<>(categoryList, expectedPageable, 1);

        when(categoryRepository.findByUser(eq(mockUser), any(Pageable.class))).thenReturn(categoryPage);

        when(categoryMapper.toDto(mockCategory)).thenReturn(mockDto);

        Page<CategoryDTO> result = categoryService.search(filter);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(mockDto.name(), result.getContent().get(0).name());

        verify(categoryRepository).findByUser(eq(mockUser), any(Pageable.class));
    }

    @Test
    void getOptions_Success() {

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);

        List<Category> categories = List.of(mockCategory);
        when(categoryRepository.findByUser(mockUser)).thenReturn(categories);

        List<OptionDTO> result = categoryService.getOptions();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockCategory.getName(), result.get(0).value());
        assertEquals(mockCategory.getId().toString(), result.get(0).id());


        verify(categoryRepository).findByUser(mockUser);

        verifyNoInteractions(categoryMapper);
    }

}