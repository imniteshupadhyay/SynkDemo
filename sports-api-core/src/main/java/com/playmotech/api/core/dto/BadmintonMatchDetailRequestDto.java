package com.playmotech.api.core.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BadmintonMatchDetailRequestDto {
	private List<BadmintonMatchRoundDetailsRequestDto> rounds;
	private String winningPlayerUserId;
	private String winningGuestPlayerName;
	private String winningTeamId;
	private Boolean isTied;
}
