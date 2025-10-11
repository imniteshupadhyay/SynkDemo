package com.playmotech.api.core.response.dao;

import java.time.LocalTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DaywiseActivityMappingDao {
    private Long id;
    private List<ActivityDao> activities;
    private LocalTime startTime;
    private LocalTime endTime;
}