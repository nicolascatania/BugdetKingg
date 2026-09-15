package com.veritech.BudgetKing.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Date helpers shared by filters and CSV import/export.
 */
public class DateUtils {

    /** Human-readable date-time written to CSV exports and shown in the UI. */
    public static final DateTimeFormatter CSV_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Human-readable bare date accepted by CSV imports; normalised to midnight. */
    public static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static LocalDateTime parseStart(String date) {
        return LocalDate.parse(date).atStartOfDay();
    }

    public static LocalDateTime parseEnd(String date) {
        return LocalDate.parse(date).plusDays(1).atStartOfDay();
    }

    /**
     * Parses a CSV date cell. Accepts, in this order, {@code dd/MM/yyyy HH:mm},
     * {@code dd/MM/yyyy}, ISO {@code yyyy-MM-ddTHH:mm:ss} and ISO {@code yyyy-MM-dd}
     * (the ISO forms keep files exported before the human-readable format importable).
     * Bare dates are normalised to midnight.
     *
     * @throws DateTimeParseException when the value matches none of the accepted formats
     */
    public static LocalDateTime parseCsvDate(String raw) {
        try {
            return LocalDateTime.parse(raw, CSV_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // fall through to the next format
        }
        try {
            return LocalDate.parse(raw, CSV_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            // fall through to the next format
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // fall through to the next format
        }
        return LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
    }

}
