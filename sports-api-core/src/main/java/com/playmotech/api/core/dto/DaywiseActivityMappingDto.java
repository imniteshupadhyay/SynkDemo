package com.playmotech.api.core.dto;

import java.time.LocalTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaywiseActivityMappingDto {
    private Long id;
    private List<Long> activityIds;
    private LocalTime startTime;
    private LocalTime endTime;
}