package com.veritech.BudgetKing.security.service;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.AppUserRepository;
import com.veritech.BudgetKing.security.dto.UpdateProfileRequest;
import com.veritech.BudgetKing.security.dto.UserProfileDTO;
import com.veritech.BudgetKing.utils.IsoCountries;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Updates the signed-in user's own profile. Works on a freshly loaded entity
 * rather than the one carried by the security principal: that copy was loaded
 * by the JWT filter and is detached by the time a controller runs, so saving it
 * would merge a stale snapshot.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final AppUserRepository appUserRepository;

    /**
     * Applies {@code request} to the user and returns the profile as stored.
     *
     * @throws IllegalArgumentException when the country is not an ISO 3166-1 code
     *                                  (mapped to 400 by {@code GlobalExceptionHandler})
     */
    @Transactional
    public UserProfileDTO update(UUID userId, UpdateProfileRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (request.country() != null && !IsoCountries.isValid(request.country())) {
            throw new IllegalArgumentException("Unknown country code: " + request.country());
        }
        user.setCountry(request.country());

        return UserProfileDTO.from(appUserRepository.save(user));
    }
}
