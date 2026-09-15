package com.veritech.BudgetKing.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ISO country codes")
class IsoCountriesTest {

    @ParameterizedTest(name = "accepts {0}")
    @ValueSource(strings = {"AR", "PL", "DE", "US", "ES"})
    void acceptsKnownCodes(String code) {
        assertTrue(IsoCountries.isValid(code));
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @NullSource
    @ValueSource(strings = {"ar", "ARG", "XX", "", " AR"})
    void rejectsUnknownOrMisformattedCodes(String code) {
        assertFalse(IsoCountries.isValid(code));
    }

    @Test
    @DisplayName("Lists every JDK country exactly once, sorted")
    void listsAllCodesSorted() {
        List<String> all = IsoCountries.all();

        assertEquals(all.stream().distinct().count(), all.size());
        assertEquals(all.stream().sorted().toList(), all);
        assertTrue(all.contains("AR"));
        assertTrue(all.size() > 200, "expected the full ISO 3166-1 list, got " + all.size());
    }
}
