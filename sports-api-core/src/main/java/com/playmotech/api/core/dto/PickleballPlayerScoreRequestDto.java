package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class PickleballPlayerScoreRequestDto {
    private String playerUserId;
    private String guestPlayerName;
    private String teamId;
    private Long score;
}
