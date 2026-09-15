package com.veritech.BudgetKing.security.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Update profile request country format")
class UpdateProfileRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest(name = "accepts {0}")
    @NullSource
    @ValueSource(strings = {"AR", "PL"})
    void acceptsTwoUpperCaseLettersOrNull(String country) {
        assertTrue(validator.validate(new UpdateProfileRequest(country)).isEmpty());
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @ValueSource(strings = {"ar", "ARG", "A", "", "A1"})
    void rejectsAnythingElse(String country) {
        assertFalse(validator.validate(new UpdateProfileRequest(country)).isEmpty());
    }
}
