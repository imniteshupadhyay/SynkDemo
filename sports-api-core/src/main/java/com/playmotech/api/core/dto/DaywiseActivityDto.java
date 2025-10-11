package com.playmotech.api.core.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaywiseActivityDto {
    private Long id;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate activityDate;
    private List<DaywiseActivityMappingDto> activityMappings = new ArrayList<>();
}
