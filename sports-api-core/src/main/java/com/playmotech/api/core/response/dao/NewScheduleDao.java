package com.playmotech.api.core.response.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.playmotech.api.core.constants.ScheduleType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NewScheduleDao {
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String dayJson;
    private ScheduleType type;
    private UserProfileDao createdBy;
    private LocalDateTime insertedOn;
    private LocalDateTime updatedOn;
    private List<DaywiseActivityDao> daywiseActivities = new ArrayList<>();
}