package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class PlayerScoreUpdateDto {
    private String playerId;
    private String guestPlayerName;
    private String teamId;
    private Long score;
    private Boolean isServing;
    private String shotType;
}
