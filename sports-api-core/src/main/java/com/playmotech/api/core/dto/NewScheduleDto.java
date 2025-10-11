package com.playmotech.api.core.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.playmotech.api.core.constants.ScheduleType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewScheduleDto {
    private Long id;
    private String name;
    private String description;
    private String dayJson;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ScheduleType type;
    private List<DaywiseActivityDto> daywiseActivities = new ArrayList<>();
}
