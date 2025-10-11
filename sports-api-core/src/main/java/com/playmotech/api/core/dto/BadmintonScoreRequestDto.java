package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class BadmintonScoreRequestDto {
	private String matchId;
	private Integer points;
	private String comment;
	private String playerUserId;
	private String guestPlayerName;
	private String teamId;
}
