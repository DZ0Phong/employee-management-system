package com.group5.ems.dto.response.hr;

import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Builder
public record HrEventDTO(
    String title,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String color
) {
    public String getMonthLabel() {
        return date.format(DateTimeFormatter.ofPattern("MMM")).toUpperCase();
    }

    public String getDayLabel() {
        return String.valueOf(date.getDayOfMonth());
    }

    public String getTimeLabel() {
        if (startTime != null && endTime != null &&
            startTime.equals(LocalTime.MIN) && endTime.equals(LocalTime.MAX)) {
            return "All Day";
        }
        String s = (startTime != null) ? startTime.toString() : "00:00";
        String e = (endTime != null) ? endTime.toString() : "23:59";
        return s + " - " + e;
    }

    public String getColorClass() {
        if (color == null) return "bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400";
        return switch (color) {
            case "blue" -> "bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400";
            case "purple" -> "bg-purple-50 dark:bg-purple-900/20 text-purple-600 dark:text-purple-400";
            case "emerald" -> "bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400";
            case "amber" -> "bg-amber-50 dark:bg-amber-900/20 text-amber-600 dark:text-amber-400";
            case "rose" -> "bg-rose-50 dark:bg-rose-900/20 text-rose-600 dark:text-rose-400";
            default -> "bg-slate-50 dark:bg-slate-900/20 text-slate-600 dark:text-slate-400";
        };
    }
}
