package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class TeamPlayerDto {
	private String playerUserId;
	private UserProfileMinDto userProfile;
	private String guestPlayerName;
}
