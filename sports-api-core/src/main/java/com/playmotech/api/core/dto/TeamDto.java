package com.playmotech.api.core.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamDto {
    private String id;
    @NotNull(message = "Team name cannot be null.")
    private String teamName;
    private List<TeamPlayerDto> players; //modelMapper wont work
    private String badmintonTournamentId; // modelMapper won't work
    private UserProfileMinDto createdByUserProfile;
}
