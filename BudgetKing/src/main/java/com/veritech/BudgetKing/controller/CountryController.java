package com.veritech.BudgetKing.controller;

import com.veritech.BudgetKing.utils.IsoCountries;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The country codes a user may pick in Settings. Served from the backend so the
 * list the picker shows and the list {@code PATCH /me} validates against are
 * the same one — see {@link IsoCountries}.
 */
@RestController
@RequestMapping("/countries")
public class CountryController {

    /** ISO 3166-1 alpha-2 codes, sorted. Localized names are resolved client-side. */
    @GetMapping
    public List<String> all() {
        return IsoCountries.all();
    }
}
