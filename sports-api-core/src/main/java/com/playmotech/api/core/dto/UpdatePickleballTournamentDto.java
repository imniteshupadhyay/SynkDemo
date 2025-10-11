package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.GameFormat;
import com.playmotech.api.core.constants.TournamentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePickleballTournamentDto {
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private TournamentType type;
    private GameFormat format;
}
