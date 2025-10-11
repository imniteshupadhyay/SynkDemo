package com.playmotech.api.core.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BadmintonMatchRoundDetailsRequestDto {
	private int round;
	private String roundStartTime;
	private String roundEndTime;
	private List<BadmintonPlayerScoreRequestDto> playerScores;
	private String winningPlayerUserId;
	private String winningGuestPlayerName;
	private String winningTeamId;
}
