package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.AppUserDTO;
import com.veritech.BudgetKing.dto.AppUserForListDTO;
import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.mapper.AppUserMapper;
import com.veritech.BudgetKing.mapper.CategoryMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.Role;
import com.veritech.BudgetKing.repository.AppUserRepository;
import com.veritech.BudgetKing.repository.BaseRepositoryTest;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.security.enumerator.Roles;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
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
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AppUserMapper appUserMapper;

    @InjectMocks
    private AppUserService appUserService;

    private AppUser mockUser;
    ;
    private AppUserDTO mockDto;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        Role roleADMIN = new Role(UUID.randomUUID(), Roles.ROLE_ADMIN.name(), Set.of());
        mockUser = AppUser.builder()
                .id(userId)
                .email("nicolas@gmail.com")
                .name("Nico")
                .lastName("Ar")
                .enabled(true)
                .roles(Set.of(roleADMIN))
                .build();
        mockDto = new AppUserDTO(userId, "nicolas@gmail.com", "Nico", "Ar", Collections.singleton(Roles.ROLE_ADMIN.name()), true);


    }


    @Test
    void getById_success() {
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(appUserMapper.toDto(mockUser)).thenReturn(mockDto);
        AppUserDTO result = appUserService.getById(userId);
        assertNotNull(result);
        assertEquals(mockDto, result);
        verify(appUserRepository).findById(userId);
    }

    @Test
    void getById_failure_throws_EntityNotFoundException() {
        when(appUserRepository.findById(null)).thenThrow(new EntityNotFoundException("User not found"));
        assertThrows(EntityNotFoundException.class, () -> appUserService.getById(null));
    }

    @Test
    @DisplayName("Should create a valid user, Mapper and Repository should be called correctly")
    void create() {

        when(appUserMapper.toEntity(mockDto, null)).thenReturn(mockUser);
        when(appUserRepository.save(mockUser)).thenReturn(mockUser);
        when(appUserMapper.toDto(mockUser)).thenReturn(mockDto);

        AppUserDTO result = appUserService.create(mockDto);

        assertNotNull(result);
        assertEquals(mockDto, result);

        verify(appUserMapper).toEntity(mockDto, null);
        verify(appUserRepository).save(mockUser);
        verify(appUserMapper).toDto(mockUser);
    }

    @Test
    @DisplayName("Return a list of users with certain fields to show on website, sorted by last name")
    void getListForWebsite() {

        Role roleADMIN = new Role(UUID.randomUUID(), Roles.ROLE_ADMIN.name(), Set.of());
        AppUser appUser2 = AppUser.builder().id(UUID.randomUUID()).name("1").lastName("Test").build();

        AppUserForListDTO listDto1 = new AppUserForListDTO(userId, "Nico", "Ar", "nicolas@gmail.com", true, List.of("ROLE_ADMIN"));
        AppUserForListDTO listDto2 = new AppUserForListDTO(appUser2.getId(), "1", "Test", "nico2@gmail.com", true, List.of("ROLE_ADMIN"));

        List<AppUser> users = List.of(mockUser, appUser2);
        Page<AppUser> userPage = new PageImpl<>(users);


        when(appUserRepository.findAll(any(Pageable.class))).thenReturn(userPage);


        when(appUserMapper.toAppUserForListDTO(mockUser)).thenReturn(listDto1);
        when(appUserMapper.toAppUserForListDTO(appUser2)).thenReturn(listDto2);

        // 3. Ejecución
        Page<AppUserForListDTO> result = appUserService.getListForWebsite(0, 10);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Nico", result.getContent().get(0).name());
        assertEquals("Test", result.getContent().get(1).lastName());

        verify(appUserRepository).findAll(any(Pageable.class));
        verify(appUserMapper).toAppUserForListDTO(mockUser);
        verify(appUserMapper).toAppUserForListDTO(appUser2);
    }
}