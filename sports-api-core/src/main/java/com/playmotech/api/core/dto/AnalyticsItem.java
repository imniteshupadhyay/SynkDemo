package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsItem {
    private String timestampOfOutcome;
    private String shotType;
    private String feedback;
}