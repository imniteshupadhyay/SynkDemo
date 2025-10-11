package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.services.impl.PickleBallTeamPlayerDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickleballTeamDto {
    private String id;
    @NotNull(message = "Team name cannot be null")
    private String teamName;
    private List<PickleBallTeamPlayerDto> players; // modelMapper wont work
    private String pickleballTournamentId; // modelMapper wont work
    private UserProfileMinDto createdByUserProfile;
}
