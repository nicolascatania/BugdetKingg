package com.veritech.BudgetKing.security.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Register request password policy")
class RegisterRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest(name = "accepts \"{0}\"")
    @ValueSource(strings = {"abcdefg1", "Passw0rd!", "1234567a", "contraseña9"})
    void acceptsStrongEnoughPasswords(String password) {
        assertTrue(violationsOn(password).isEmpty());
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @ValueSource(strings = {
            "abc1",          // too short
            "abcdefgh",      // no digit
            "12345678",      // no letter
            "        ",      // blank
    })
    void rejectsWeakPasswords(String password) {
        assertFalse(violationsOn(password).isEmpty());
    }

    @Test
    @DisplayName("rejects passwords longer than 72 characters (BCrypt input limit)")
    void rejectsOverlongPassword() {
        String tooLong = "a1".repeat(37); // 74 chars
        assertFalse(violationsOn(tooLong).isEmpty());
    }

    private static Set<ConstraintViolation<RegisterRequest>> violationsOn(String password) {
        RegisterRequest request = new RegisterRequest("nico@budgetking.com", password, "Nico", "Catania");
        return validator.validate(request);
    }
}
