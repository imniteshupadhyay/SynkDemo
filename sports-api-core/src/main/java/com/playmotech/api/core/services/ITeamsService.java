package com.playmotech.api.core.services;

import java.util.List;
import java.util.Optional;

import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.Team;
import com.playmotech.api.core.dto.TeamDto;
import com.playmotech.api.core.dto.TeamPerformanceDto;
import com.playmotech.api.core.dto.TeamPlayerDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface ITeamsService {
    TeamDto addTeam(String userId, String academyId, TeamDto teamDto, Sports sport) throws ResourceException;

    TeamDto addTeam(String userId, TeamDto teamDto, Sports sport) throws ResourceException;

    TeamDto updateTeam(String userId, String academyId, String teamId, TeamDto teamDto, Sports sport) throws ResourceException;

    TeamDto updateTeam(String userId, String teamId, TeamDto teamDto, Sports sport) throws ResourceException;

    List<TeamDto> getTeams(String userId, String academyId, String badmintonTournamentId, String searchTxt, Sports sport)
            throws ResourceException;

    List<TeamDto> getTeams(String userId, String searchTxt, String badmintonTournamentId, Sports sport) throws ResourceException;

    // List<TeamDto> getTeams(String academyId, List<String> teamId, String
    // badmintonTournamentId) throws ResourceException;
    Optional<TeamDto> getTeam(String academyId, String teamId, Sports sport) throws ResourceException;

    Optional<TeamDto> getTeam(String teamId, Sports sport) throws ResourceException;

    void deleteTeam(String academyId, String teamId, boolean removeBadmintonTournament, String tournamentId, Sports sport)
            throws ResourceException;

    void deleteTeam(String teamId, boolean removeBadmintonTournament, String tournamentId, Sports sport) throws ResourceException;

    List<TeamDto> convertToDto(List<Team> teams);

    TeamDto convertToDto(Team team);

    List<TeamPerformanceDto> getTeamPerformance(String tournamentId);

    TeamDto addPlayersToTeam(String userId, String academyId, String teamId, List<TeamPlayerDto> newPlayers, Sports sport)
            throws ResourceException;

    TeamDto addPlayersToTeam(String userId, String teamId, List<TeamPlayerDto> newPlayers, Sports sport)
            throws ResourceException;

    TeamDto updateTeamWithAddPlayers(String userId, String academyId, String teamId,
                                     TeamDto teamDto, boolean addPlayersMode, Sports sport) throws ResourceException;
}
