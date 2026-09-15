package com.veritech.BudgetKing.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * ISO 3166-1 alpha-2 country codes, sourced from the JDK
 * ({@link Locale#getISOCountries()}) so the list is never hand-maintained.
 * The frontend renders the localized names with {@code Intl.DisplayNames};
 * the backend only ever deals in codes.
 */
public final class IsoCountries {

    private static final Set<String> CODES = Set.of(Locale.getISOCountries());

    /** Codes in ascending order; the client sorts by localized name itself. */
    private static final List<String> SORTED = Arrays.stream(Locale.getISOCountries()).sorted().toList();

    private IsoCountries() {
    }

    /** Every known code, sorted. */
    public static List<String> all() {
        return SORTED;
    }

    /** Case-sensitive: codes are stored and compared upper-case only. */
    public static boolean isValid(String code) {
        return code != null && CODES.contains(code);
    }
}
