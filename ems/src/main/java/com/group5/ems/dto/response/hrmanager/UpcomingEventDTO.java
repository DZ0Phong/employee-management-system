package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingEventDTO {
    private String title;
    private String monthLabel;
    private String day;
    private String timeLabel;
    private String colorClass;
}