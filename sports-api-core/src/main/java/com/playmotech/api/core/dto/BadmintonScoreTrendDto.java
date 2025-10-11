package com.playmotech.api.core.dto;

import java.util.Map;

import lombok.Data;

@Data
public class BadmintonScoreTrendDto {
    private String scoreTime;
    private Map<String, Long> scoreTrend;
}
