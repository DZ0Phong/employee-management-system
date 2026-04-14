package com.group5.ems.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class WorkingDayUtils {

    private WorkingDayUtils() {
    }

    public static long countWorkingDays(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            return 0;
        }

        long workingDays = 0;
        LocalDate current = from;
        while (!current.isAfter(to)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }
}
