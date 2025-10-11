package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.GameFormat;
import com.playmotech.api.core.constants.TournamentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created By: deep.patel
 **/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UpdateBadmintonTournamentDto {
	private String name;
	private String description;
	private String startDate;
	private String endDate;
	private TournamentType type;
	private GameFormat format;

	// New field for GameFormat TEAMS
	private List<TeamDto> teams;
}
