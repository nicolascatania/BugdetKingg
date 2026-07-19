package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.AppUserDTO;
import com.veritech.BudgetKing.dto.AppUserForListDTO;
import com.veritech.BudgetKing.mapper.AppUserMapper;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Role;
import com.veritech.BudgetKing.repository.AppUserRepository;
import com.veritech.BudgetKing.security.enumerator.Roles;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppUser Service Specification")
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AppUserMapper appUserMapper;

    @InjectMocks
    private AppUserService appUserService;

    private AppUser mockUser;
    private AppUserDTO mockDto;
    private UUID userId;
    private String userEmail;

    @BeforeEach
    void setUpDefaults() {
        userId = UUID.randomUUID();
        userEmail = "nicolas@gmail.com";
        Role roleADMIN = new Role(UUID.randomUUID(), Roles.ROLE_ADMIN.name(), Set.of());

        mockUser = AppUser.builder()
                .id(userId)
                .email(userEmail)
                .name("Nico")
                .lastName("Ar")
                .enabled(true)
                .roles(Set.of(roleADMIN))
                .build();

        mockDto = new AppUserDTO(userId, userEmail, "Nico", "Ar", Collections.singleton(Roles.ROLE_ADMIN.name()), true);
    }

    @Test
    @DisplayName("Should return user DTO when ID exists")
    void shouldReturnUserDtoWhenIdExists() {
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(appUserMapper.toDto(mockUser)).thenReturn(mockDto);

        AppUserDTO result = appUserService.getById(userId);

        assertNotNull(result, () -> "Result should not be null");
        assertEquals(mockDto, result, () -> "DTO mapping mismatch");
        verify(appUserRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when ID does not exist")
    void shouldThrowExceptionWhenIdNotFound() {
        when(appUserRepository.findById(any())).thenThrow(new EntityNotFoundException("User not found"));

        assertThrows(EntityNotFoundException.class, () -> appUserService.getById(null),
                () -> "Should throw EntityNotFoundException for null/non-existent ID");
    }

    @Test
    @DisplayName("Should create user and return mapped DTO")
    void shouldCreateUserSuccessfully() {
        when(appUserMapper.toEntity(mockDto, null)).thenReturn(mockUser);
        when(appUserRepository.save(mockUser)).thenReturn(mockUser);
        when(appUserMapper.toDto(mockUser)).thenReturn(mockDto);

        AppUserDTO result = appUserService.create(mockDto);

        assertNotNull(result, () -> "Created user DTO should not be null");
        assertEquals(mockDto, result, () -> "Result DTO does not match input");
        verify(appUserRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should return paginated list of users for website display")
    void shouldReturnUserListForWebsite() {
        AppUser appUser2 = AppUser.builder().id(UUID.randomUUID()).name("TestName").lastName("TestLastName").build();
        AppUserForListDTO listDto1 = new AppUserForListDTO(userId, "Nico", "Ar", userEmail, true, List.of("ROLE_ADMIN"));
        AppUserForListDTO listDto2 = new AppUserForListDTO(appUser2.getId(), "TestName", "TestLastName", "test@gmail.com", true, List.of("ROLE_ADMIN"));

        Page<AppUser> userPage = new PageImpl<>(List.of(mockUser, appUser2));

        when(appUserRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        when(appUserMapper.toAppUserForListDTO(mockUser)).thenReturn(listDto1);
        when(appUserMapper.toAppUserForListDTO(appUser2)).thenReturn(listDto2);

        Page<AppUserForListDTO> result = appUserService.getListForWebsite(0, 10);

        assertNotNull(result, () -> "Result page should not be null");
        assertEquals(2, result.getTotalElements(), () -> "Total elements count mismatch");
        assertEquals("Nico", result.getContent().get(0).name(), () -> "Name mismatch in list index 0");
        assertEquals("TestLastName", result.getContent().get(1).lastName(), () -> "Last name mismatch in list index 1");

        verify(appUserRepository).findAll(any(Pageable.class));
    }
}