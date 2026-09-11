package com.veritech.BudgetKing.service;

import java.util.Set;

/**
 * Valid icon identifiers an {@code Account} can use.
 *
 * <p>Mirrors the {@code FINANCIAL_ICONS} array in
 * {@code frontend/src/app/features/icons/interfaces/iconsenum.interace.ts}. There is no
 * mechanism to share this list between the Angular and Spring codebases, so it is duplicated
 * here on purpose; keep both lists in sync when icons are added or removed on the frontend.</p>
 */
public final class AccountIconCatalog {

    public static final String DEFAULT_ICON = "fa-building-columns";

    public static final Set<String> VALID_ICONS = Set.of(
            "fa-wallet", "fa-building-columns", "fa-credit-card", "fa-money-bill-wave",
            "fa-piggy-bank", "fa-coins", "fa-chart-line", "fa-briefcase"
    );

    private AccountIconCatalog() {
    }

    /**
     * Returns the given icon when it is a known, valid icon; otherwise {@link #DEFAULT_ICON}.
     */
    public static String resolveOrDefault(String icon) {
        if (icon == null) {
            return DEFAULT_ICON;
        }
        String trimmed = icon.trim();
        return VALID_ICONS.contains(trimmed) ? trimmed : DEFAULT_ICON;
    }
}
