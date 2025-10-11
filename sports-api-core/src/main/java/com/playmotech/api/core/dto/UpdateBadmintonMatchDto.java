package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.MatchStatus;
import com.playmotech.api.core.constants.StreamingStatus;

import lombok.Data;

@Data
public class UpdateBadmintonMatchDto {
    //	private List<String> teamIds;
    private List<TeamDto> teamIds; // modelMapper wont work
    private List<TeamPlayerDto> players; // modelMapper wont work
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
