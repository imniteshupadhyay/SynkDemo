package com.playmotech.api.core.services;

import java.util.List;
import java.util.Optional;

import com.playmotech.api.core.dao_postgres.PickleballTeam;
import com.playmotech.api.core.dto.PickleballTeamDto;
import com.playmotech.api.core.dto.TeamPerformanceDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.impl.PickleBallTeamPlayerDto;

public interface IPickleballTeamService {

    PickleballTeamDto addTeam(String userId, String academyId, PickleballTeamDto teamDto) throws ResourceException;

    PickleballTeamDto addTeam(String userId, PickleballTeamDto teamDto) throws ResourceException;

    PickleballTeamDto updateTeam(String userId, String academyId, String teamId, PickleballTeamDto teamDto) throws ResourceException;

    PickleballTeamDto updateTeam(String userId, String teamId, PickleballTeamDto teamDto) throws ResourceException;

    void deleteTeam(String academyId, String teamId, boolean removeBadmintonTournament, String tournamentId) throws ResourceException;

    void deleteTeam(String teamId, boolean removeBamdintonTournament, String tournamentId) throws ResourceException;

    PickleballTeamDto getTeam(String userId, String academyId, String teamId) throws ResourceException;

    List<PickleballTeamDto> getTeams(String userId, String searchTxt, String badmintonTournament) throws ResourceException;

    List<PickleballTeamDto> getTeams(String userId, String academyId, String pickleBallTournamentId, String searchTxt) throws ResourceException;

    List<PickleballTeamDto> searchTeams(String userId, String academyId, String searchTxt) throws ResourceException;

    List<PickleballTeamDto> getTeamsByTournament(String tournamentId) throws ResourceException;

    void removePlayerFromTeam(String userId, String academyId, String teamId, String playerUserId) throws ResourceException;

    List<PickleballTeamDto> convertToDto(List<PickleballTeam> teams);

    PickleballTeamDto convertToDto(PickleballTeam team);

    Optional<PickleballTeamDto> getTeam(String teamId) throws ResourceException;

    Optional<PickleballTeamDto> getTeam(String academyId, String teamId) throws ResourceException;

    PickleballTeamDto addPlayersToTeam(String userId, String academyId, String teamId, List<PickleBallTeamPlayerDto> newPlayers) throws ResourceException;

    PickleballTeamDto addPlayersToTeam(String userId, String teamId, List<PickleBallTeamPlayerDto> newPlayers) throws ResourceException;

    PickleballTeamDto updateTeamWithAddPlayers(String userId, String academyId, String teamId,
                                               PickleballTeamDto teamDto, boolean addPlayersMode) throws ResourceException;

    List<TeamPerformanceDto> getTeamPerformance(String tournamentId);
}
