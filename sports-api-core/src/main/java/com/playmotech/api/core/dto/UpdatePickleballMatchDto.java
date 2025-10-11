package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.MatchStatus;
import com.playmotech.api.core.constants.StreamingStatus;
import com.playmotech.api.core.services.impl.PickleBallTeamPlayerDto;

import lombok.Data;

@Data
public class UpdatePickleballMatchDto {
    private List<PickleballTeamDto> teamIds; // modelMapper wont work
    private List<PickleBallTeamPlayerDto> players; // modelMapper wont work
    private Integer maxPoints;
    private Integer maxRounds;
    private String scorerUserId;
    private String refereeUserId;
    private String matchOfficialUserId;
    private Boolean isLivestreamed;
    private Boolean isRecordingEnabled;
    private MatchStatus matchStatus;
    private String courtId;
    private String scheduledStartTime;
    private StreamingStatus streamingStatus;

    private List<TeamMatchPlayerDto> teams;

}
