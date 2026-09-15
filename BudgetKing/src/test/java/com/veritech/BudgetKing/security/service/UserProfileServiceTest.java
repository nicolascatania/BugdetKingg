package com.veritech.BudgetKing.security.service;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.AppUserRepository;
import com.veritech.BudgetKing.security.dto.UpdateProfileRequest;
import com.veritech.BudgetKing.security.dto.UserProfileDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User profile service")
class UserProfileServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private UserProfileService service;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.builder()
                .id(UUID.randomUUID())
                .email("ada@mail.com")
                .name("Ada")
                .lastName("Lovelace")
                .picture("https://example.com/ada.jpg")
                .build();
    }

    @Test
    @DisplayName("Stores a valid country and returns the full profile")
    void storesValidCountry() {
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileDTO profile = service.update(user.getId(), new UpdateProfileRequest("AR"));

        assertEquals("AR", user.getCountry());
        assertEquals("AR", profile.country());
        assertEquals("ada@mail.com", profile.email());
        assertEquals("Ada", profile.name());
        assertEquals("Lovelace", profile.lastName());
        assertEquals("https://example.com/ada.jpg", profile.picture());
        verify(appUserRepository).save(user);
    }

    @Test
    @DisplayName("Null country clears a previous choice")
    void nullClearsCountry() {
        user.setCountry("AR");
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileDTO profile = service.update(user.getId(), new UpdateProfileRequest(null));

        assertNull(user.getCountry());
        assertNull(profile.country());
    }

    @Test
    @DisplayName("Rejects a well-formed but unknown code without touching the user")
    void rejectsUnknownCountry() {
        user.setCountry("AR");
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(user.getId(), new UpdateProfileRequest("XX")));

        assertTrue(ex.getMessage().contains("XX"));
        assertEquals("AR", user.getCountry());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fails when the principal's user no longer exists")
    void failsForMissingUser() {
        UUID missing = UUID.randomUUID();
        when(appUserRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.update(missing, new UpdateProfileRequest("AR")));
    }
}
