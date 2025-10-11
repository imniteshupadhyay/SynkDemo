package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.MatchWSMessageType;

import lombok.Data;

@Data
public class BadmintonScoreNotificationDto {
	private MatchWSMessageType type;
	private String matchId;
	private Integer points;
	private String comment;
	private UserProfileMinDto playerUserProfile;
	private String guestPlayerName;
	private String teamId;
	private String scoreTime;
	private String requestId;
	private Boolean isUndo;
	private String metadata;
	private Boolean initial;
}
