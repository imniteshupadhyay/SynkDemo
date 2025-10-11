package com.playmotech.api.core.dto;

import java.util.Map;

import com.playmotech.api.core.constants.Sports;

import lombok.Data;

@Data
public class UserStatsDto {
    private Map<Sports, UserStatsBaseDto> stats;
}
