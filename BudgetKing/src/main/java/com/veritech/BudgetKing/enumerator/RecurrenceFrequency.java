package com.veritech.BudgetKing.enumerator;

import java.time.LocalDate;

/**
 * How often a recurring transaction template fires.
 *
 * <p>Every constant knows how to move a date forward by exactly one period, so the
 * recurrence engine never has to branch on the frequency itself.</p>
 */
public enum RecurrenceFrequency {

    DAILY {
        @Override
        public LocalDate advance(LocalDate from) {
            return from.plusDays(1);
        }
    },

    WEEKLY {
        @Override
        public LocalDate advance(LocalDate from) {
            return from.plusWeeks(1);
        }
    },

    MONTHLY {
        @Override
        public LocalDate advance(LocalDate from) {
            return from.plusMonths(1);
        }
    },

    YEARLY {
        @Override
        public LocalDate advance(LocalDate from) {
            return from.plusYears(1);
        }
    };

    /**
     * Moves a date forward by exactly one period of this frequency.
     *
     * <p>Month and year arithmetic clamps to the last valid day of the target month
     * (31/01 + 1 month = 28/02), which is the behaviour {@link LocalDate} already
     * guarantees. The result is always strictly after the received date.</p>
     *
     * @param from date to move forward, never {@code null}
     * @return the next occurrence date
     */
    public abstract LocalDate advance(LocalDate from);

    /**
     * Parses a frequency name, ignoring case and surrounding blanks.
     *
     * @param value textual frequency coming from a DTO or a filter
     * @return the matching constant
     * @throws IllegalArgumentException when the value maps to no constant
     */
    public static RecurrenceFrequency fromString(String value) {
        try {
            return RecurrenceFrequency.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Recurrence frequency not valid: " + value);
        }
    }
}
