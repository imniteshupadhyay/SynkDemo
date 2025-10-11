package com.playmotech.api.core.dto;

import java.sql.Timestamp;
import java.util.List;

import com.playmotech.api.core.constants.GameFormat;
import com.playmotech.api.core.constants.TournamentType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created By: deep.patel
 **/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class BadmintonTournamentDto {
    private String id;
    @NotNull(message = "Tournament name is required")
    @NotEmpty(message = "Tournament name is required")
    private String name;
    @NotNull(message = "Tournament description is required")
    @NotEmpty(message = "Tournament description is required")
    private String description;
    @NotNull(message = "Tournament start date is required")
    @NotEmpty(message = "Tournament start date is required")
    private String startDate;
    @NotNull(message = "Tournament end date is required")
    @NotEmpty(message = "Tournament end date is required")
    private String endDate;
    @NotNull(message = "Tournament type is required")
    private TournamentType type;
    @NotNull(message = "Tournament format is required")
    private GameFormat format;
    private AcademyMinDto academy;
    private List<TeamDto> teams; // modelMapper wont work
    private List<BadmintonMatchDto> matches; // modelMapper wont work
    private String createdByUserId;
    private UserProfileMinDto createdByUserProfile;
    private Boolean canManage;
    private List<MediaDto> galleryMedia;

    private List<TeamMatchPlayerDto> teamPlayers;

    private String scorerUserId;
    private UserProfileMinDto scorer;

    private String refereeUserId;
    private UserProfileMinDto referee;

    private String matchOfficialUserId;
    private UserProfileMinDto matchOfficial;

    private Timestamp createdOn;

}
