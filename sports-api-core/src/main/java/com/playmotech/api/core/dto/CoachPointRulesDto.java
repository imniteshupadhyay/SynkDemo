package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoachPointRulesDto {
    private Long id;

    private String description;
    private String actionType;

    private Integer pointsAwarded;
}
