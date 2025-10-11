package com.playmotech.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BadmintonPlayerScoreRequestDto {
	private String playerUserId;
	private String guestPlayerName;
	private String teamId;
	private Long score;
}
