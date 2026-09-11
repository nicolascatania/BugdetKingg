package com.veritech.BudgetKing.service;

import java.util.Set;

/**
 * Valid icon identifiers a {@code Category} can use.
 *
 * <p>Mirrors the {@code FINANCIAL_ICONS}/{@code CATEGORY_ICONS} arrays in
 * {@code frontend/src/app/features/icons/interfaces/iconsenum.interace.ts}. There is no
 * mechanism to share this list between the Angular and Spring codebases, so it is duplicated
 * here on purpose; keep both lists in sync when icons are added or removed on the frontend.</p>
 */
public final class CategoryIconCatalog {

    public static final String DEFAULT_ICON = "fa-tags";

    public static final Set<String> VALID_ICONS = Set.of(
            "fa-wallet", "fa-building-columns", "fa-credit-card", "fa-money-bill-wave",
            "fa-piggy-bank", "fa-coins", "fa-chart-line", "fa-briefcase", "fa-utensils",
            "fa-burger", "fa-basket-shopping", "fa-coffee", "fa-glass-martini-alt", "fa-car",
            "fa-gas-pump", "fa-bus", "fa-plane", "fa-motorcycle", "fa-house", "fa-bolt",
            "fa-faucet", "fa-wifi", "fa-couch", "fa-film", "fa-gamepad", "fa-music",
            "fa-football", "fa-ticket", "fa-shirt", "fa-bag-shopping", "fa-spa", "fa-gift",
            "fa-dumbbell", "fa-heart-pulse", "fa-pills", "fa-user-doctor", "fa-book",
            "fa-graduation-cap", "fa-laptop", "fa-hand-holding-dollar", "fa-money-bill-trend-up",
            "fa-tags", "fa-vr-cardboard", "fa-microchip", "fa-code", "fa-palette", "fa-camera",
            "fa-bicycle", "fa-swimmer", "fa-dog", "fa-baby", "fa-tree", "fa-hammer",
            "fa-scale-balanced", "fa-shield-halved", "fa-people-group", "fa-phone", "fa-tv",
            "fa-calendar-days", "fa-chart-pie", "fa-bitcoin", "fa-landmark", "fa-arrow-trend-up",
            "fa-volleyball", "fa-basketball", "fa-table-tennis-paddle-ball", "fa-person-running",
            "fa-trophy"
    );

    private CategoryIconCatalog() {
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
