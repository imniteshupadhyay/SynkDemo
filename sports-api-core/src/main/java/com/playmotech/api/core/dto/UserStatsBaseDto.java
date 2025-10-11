package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class UserStatsBaseDto {
    private long totalMatches;
    private long wins;
    private long losses;
    private long draws;
}
