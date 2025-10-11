package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class GuestPlayerPerformanceDto {
	private String guestPlayerName;
	private Integer matchesPlayed;
	private Integer matchesWon;
	private Double winPercentage;
	private Integer totalPoints;
}
