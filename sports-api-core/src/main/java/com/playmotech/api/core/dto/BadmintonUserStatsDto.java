package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class BadmintonUserStatsDto extends UserStatsBaseDto{
    private List<PointsByShotType> pointsByShotType;

    public record PointsByShotType(String asset, String shotType, Long points) {}
}
