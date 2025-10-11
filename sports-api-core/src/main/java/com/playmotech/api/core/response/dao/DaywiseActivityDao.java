package com.playmotech.api.core.response.dao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DaywiseActivityDao {
    private Long id;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate activityDate;
    private List<DaywiseActivityMappingDao> activityMappings = new ArrayList<>();
}