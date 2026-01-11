package com.veritech.BudgetKing.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DateUtils {

    public static LocalDateTime parseStart(String date) {
        return LocalDate.parse(date).atStartOfDay();
    }

    public static LocalDateTime parseEnd(String date) {
        return LocalDate.parse(date).plusDays(1).atStartOfDay();
    }

}
