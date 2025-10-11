package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.PickleballTeam;
import com.playmotech.api.core.dao_postgres.PickleballTeamPlayerMapping;
import com.playmotech.api.core.dao_postgres.PickleballTournament;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.PickleballTeamDto;
import com.playmotech.api.core.dto.TeamPerformanceDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.PickleballTeamRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IPickleballTeamService;
import com.playmotech.api.core.services.IUserProfileService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PickleballTeamService implements IPickleballTeamService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final PickleballTeamRepo teamRepo;
    private final IAcademyService academyService;
    private final IUserProfileService userProfileService;
    private final PickleballTeamRepo pickleballTeamRepo;

    PickleballTeamService(final PickleballTeamRepo teamRepo, final IUserProfileService userProfileService,
                          final IAcademyService academyService, PickleballTeamRepo pickleballTeamRepo) {
        this.teamRepo = teamRepo;
        this.userProfileService = userProfileService;
        this.academyService = academyService;
        this.pickleballTeamRepo = pickleballTeamRepo;
    }

    @Override
    public PickleballTeamDto addTeam(String userId, String academyId, PickleballTeamDto teamDto)
            throws ResourceException {
        if (academyId != null) {
            academyService.getAcademyById(academyId);
        }

        List<PickleballTeamDto> existingTeams = getTeams(userId, academyId, null, null);
        if (!existingTeams.isEmpty() && existingTeams.stream()
                .anyMatch(team -> team.getTeamName().equalsIgnoreCase(teamDto.getTeamName()))) {
            throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Team already exists");
        }

        PickleballTeam team = new PickleballTeam();
        team.setId(UUID.randomUUID().toString());
        team.setTeamName(teamDto.getTeamName());
        team.setCreatedOn(Timestamp.from(Instant.now()));

        // Only set academy if academyId is provided
        if (academyId != null) {
            team.setAcademy(Academy.builder().id(academyId).build());
        }

        team.setInactive(false);
        team.setPlayers(buildTeamPlayerMapping(team.getId(), teamDto.getPlayers()));
        team.setCreatedByUserProfile(UserProfile.builder().id(userId).build());

        if (StringUtils.isNotEmpty(teamDto.getPickleballTournamentId())) {
            team.setPickleballTournament(
                    PickleballTournament.builder().id(teamDto.getPickleballTournamentId()).build());
        }

        teamRepo.save(team);

        // Use appropriate getTea method based on whether academyId is null
        if (academyId != null) {
            return getTeam(academyId, team.getId())
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        } else {
            return getTeam(team.getId())
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        }
    }

    @Override
    public PickleballTeamDto addTeam(String userId, PickleballTeamDto teamDto) throws ResourceException {
        List<PickleballTeamDto> existingTeams = getTeams(userId, null, null);

        if (!existingTeams.isEmpty() && existingTeams.stream()
                .anyMatch(team -> team.getTeamName().equalsIgnoreCase(teamDto.getTeamName()))) {
            throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Team with same name already exist");
        }

        PickleballTeam team = new PickleballTeam();
        team.setId(UUID.randomUUID().toString());
        team.setTeamName(teamDto.getTeamName());
        team.setCreatedOn(Timestamp.from(Instant.now()));
        team.setInactive(false);
        team.setPlayers(buildTeamPlayerMapping(team.getId(), teamDto.getPlayers()));
        team.setCreatedByUserProfile(UserProfile.builder().id(userId).build());

        if (StringUtils.isNotEmpty(teamDto.getPickleballTournamentId())) {
            team.setPickleballTournament(
                    PickleballTournament.builder().id(teamDto.getPickleballTournamentId()).build());
        }

        teamRepo.save(team);

        return getTeam(team.getId())
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
    }

    @Override
    public List<PickleballTeamDto> getTeams(String userId, String searchTxt, String badmintonTournament)
            throws ResourceException {
        List<PickleballTeam> teams = new ArrayList<>();

        if (StringUtils.isNotEmpty(badmintonTournament)) {
            teams = teamRepo.findTeamsByTournamentId(badmintonTournament);
        } else {
            teams = teamRepo.findTeamsByCreatorOrPlayer(userId);
        }

        // filter out inactive teams
        teams = teams.stream().filter(each -> !each.getInactive()).toList();

        if (StringUtils.isNotEmpty(searchTxt) && searchTxt.length() > 2) {
            teams = teams.stream().filter(team -> team.getTeamName().toLowerCase().contains(searchTxt.toLowerCase()))
                    .toList();
        }

        if (teams.isEmpty()) {
            return List.of();
        }

        List<PickleballTeamDto> teamDtos = convertToDto(teams);
        return sortTeams(teamDtos);
    }

    @Override
    public List<PickleballTeamDto> getTeams(String userId, String academyId, String pickleBallTournamentId,
                                            String searchTxt) throws ResourceException {
        academyService.getAcademyById(academyId);

        List<PickleballTeam> teams;

        if (!StringUtils.isEmpty(pickleBallTournamentId)) {
            teams = teamRepo.findByAcademy_IdAndPickleballTournament_Id(academyId, pickleBallTournamentId).stream()
                    .filter(team -> !team.getInactive()).toList();
        } else {
            teams = teamRepo.findByAcademy_Id(academyId).stream().filter(team -> !team.getInactive()).toList();
        }

        if (!StringUtils.isEmpty(searchTxt) && searchTxt.length() > 2) {
            teams = teams.stream().filter(team -> team.getTeamName().toLowerCase().contains(searchTxt.toLowerCase()))
                    .toList();
        }

        if (teams.isEmpty()) {
            return List.of();
        }

        List<PickleballTeamDto> teamDtos = convertToDto(teams);

        return sortTeams(teamDtos);
    }

    @Override
    public List<PickleballTeamDto> convertToDto(List<PickleballTeam> teams) {
        return teams.stream().map(this::convertToDto).toList();
    }

    @Override
    public PickleballTeamDto updateTeam(String userId, String academyId, String teamId, PickleballTeamDto teamDto)
            throws ResourceException {
        // Only validate academy if academyId is provided
        if (academyId != null) {
            academyService.getAcademyById(academyId);
        }

        PickleballTeam existingTeam;
        if (academyId != null) {
            existingTeam = teamRepo.findByIdAndAcademy_Id(teamId, academyId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found: " + teamId));
        } else {
            existingTeam = teamRepo.findByIdAndAcademyIsNull(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found: " + teamId));
        }

        // Check for duplicate team name (excluding current team)
        List<PickleballTeamDto> existingTeams = getTeams(userId, academyId, null, null);
        if (!existingTeams.isEmpty() && existingTeams.stream()
                .anyMatch(team -> !team.getId().equals(teamId)
                        && team.getTeamName().equalsIgnoreCase(teamDto.getTeamName()))) {
            throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Team name already exists");
        }

        // Update team properties
        if (StringUtils.isNotEmpty(teamDto.getTeamName())) {
            existingTeam.setTeamName(teamDto.getTeamName());
        }

        // Update players if provided
        if (!CollectionUtils.isEmpty(teamDto.getPlayers())) {
            existingTeam.setPlayers(buildTeamPlayerMapping(teamId, teamDto.getPlayers()));
        }

        // Update tournament association if provided
        if (StringUtils.isNotEmpty(teamDto.getPickleballTournamentId())) {
            existingTeam.setPickleballTournament(
                    PickleballTournament.builder().id(teamDto.getPickleballTournamentId()).build());
        }

        teamRepo.save(existingTeam);

        // Return updated team using appropriate getTeam method
        if (academyId != null) {
            return getTeam(academyId, teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        } else {
            return getTeam(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        }
    }

    @Override
    public PickleballTeamDto updateTeam(String userId, String teamId, PickleballTeamDto teamDto)
            throws ResourceException {
        PickleballTeam existingTeam = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        // Check for duplicate team name (excluding current team)
        List<PickleballTeamDto> existingTeams = getTeams(userId, null, null);
        if (existingTeams.stream().anyMatch(
                team -> !team.getId().equals(teamId) && team.getTeamName().equalsIgnoreCase(teamDto.getTeamName()))) {
            throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Team name already exists");
        }

        // Update team properties
        if (StringUtils.isNotEmpty(teamDto.getTeamName())) {
            existingTeam.setTeamName(teamDto.getTeamName());
        }

        // Update players if provided
        if (CollectionUtils.isEmpty(teamDto.getPlayers())) {
            existingTeam.setPlayers(buildTeamPlayerMapping(teamId, teamDto.getPlayers()));
        }

        // Update tournament association if provided
        if (StringUtils.isNotEmpty(teamDto.getPickleballTournamentId())) {
            existingTeam.setPickleballTournament(
                    PickleballTournament.builder().id(teamDto.getPickleballTournamentId()).build());
        }

        teamRepo.save(existingTeam);

        return getTeam(teamId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
    }

    @Override
    public void deleteTeam(String academyId, String teamId, boolean removePickleballTournament, String tournamentId)
            throws ResourceException {
        academyService.getAcademyById(academyId);

        PickleballTeam team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));

        if (removePickleballTournament) {
            if (team.getPickleballTournament() != null
                    && team.getPickleballTournament().getId().equalsIgnoreCase(tournamentId)) {
                team.setPickleballTournament(null);
            }
        } else {
            team.setInactive(true);
        }

        teamRepo.save(team);
    }

    @Override
    public void deleteTeam(String teamId, boolean removePickleballTournament, String tournamentId)
            throws ResourceException {
        PickleballTeam team = teamRepo.findByAcademyIsNullAndId(teamId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));

        if (removePickleballTournament) {
            if (team.getPickleballTournament() != null
                    && team.getPickleballTournament().getId().equalsIgnoreCase(tournamentId)) {
                team.setPickleballTournament(null);
            }
        } else {
            team.setInactive(true);
        }

        teamRepo.save(team);
    }

    @Override
    public PickleballTeamDto getTeam(String userId, String academyId, String teamId) throws ResourceException {
        log.info("Getting Pickleball team: {}", teamId);

        PickleballTeam team;
        if (academyId != null) {
            team = teamRepo.findByIdAndAcademy_Id(teamId, academyId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found: " + teamId));
        } else {
            team = teamRepo.findByIdAndAcademyIsNull(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found: " + teamId));
        }

        return convertToDto(team);
    }

    @Override
    public List<PickleballTeamDto> searchTeams(String userId, String academyId, String searchTxt)
            throws ResourceException {
        log.info("Searching Pickleball teams with query: {}", searchTxt);

        List<PickleballTeam> teams;
        if (academyId != null) {
            if (searchTxt != null && !searchTxt.trim().isEmpty()) {
                teams = teamRepo.findByTeamNameContainingIgnoreCaseAndAcademy_Id(searchTxt, academyId);
            } else {
                teams = teamRepo.findByAcademy_Id(academyId);
            }
        } else {
            if (searchTxt != null && !searchTxt.trim().isEmpty()) {
                teams = teamRepo.findByTeamNameContainingIgnoreCaseAndAcademyIsNull(searchTxt);
            } else {
                teams = teamRepo.findByAcademyIsNull();
            }
        }

        return teams.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PickleballTeamDto> getTeamsByTournament(String tournamentId) throws ResourceException {
        log.info("Getting teams for tournament: {}", tournamentId);

        List<PickleballTeam> teams = teamRepo.findByPickleballTournament_Id(tournamentId);

        return teams.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public PickleballTeamDto addPlayersToTeam(String userId, String academyId, String teamId,
                                              List<PickleBallTeamPlayerDto> newPlayers)
            throws ResourceException {
        // Validate user is part of the team
        validateUserIsTeamMember(userId, teamId, academyId);

        if (academyId != null) {
            academyService.getAcademyById(academyId);
        }

        PickleballTeam existingTeam;
        if (academyId != null) {
            existingTeam = teamRepo.findByIdAndAcademy_Id(teamId, academyId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found: " + teamId));
        } else {
            existingTeam = teamRepo.findByIdAndAcademyIsNull(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found: " + teamId));
        }

        // Get current players
        List<PickleballTeamPlayerMapping> currentPlayers = existingTeam.getPlayers() != null ? existingTeam.getPlayers()
                : new ArrayList<>();

        // Check for duplicate players before adding
        validateNoDuplicatePlayers(currentPlayers, newPlayers);

        List<PickleballTeamPlayerMapping> newPlayerMappings = buildTeamPlayerMapping(teamId, newPlayers);
        currentPlayers.addAll(newPlayerMappings);

        // Update the team with combined player list
        existingTeam.setPlayers(currentPlayers);
        teamRepo.save(existingTeam);

        // Return updated team
        if (academyId != null) {
            return getTeam(academyId, teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        } else {
            return getTeam(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        }
    }

    @Override
    public PickleballTeamDto updateTeamWithAddPlayers(String userId, String academyId, String teamId,
                                                      PickleballTeamDto teamDto, boolean addPlayersMode) throws ResourceException {
        // Validate user is a part of the team
        validateUserIsTeamMember(userId, teamId, academyId);

        if (academyId != null) {
            academyService.getAcademyById(academyId);
        }

        PickleballTeam existingTeam;
        if (academyId != null) {
            existingTeam = teamRepo.findByIdAndAcademy_Id(teamId, academyId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found"));
        } else {
            existingTeam = teamRepo.findByIdAndAcademyIsNull(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found"));
        }

        // Check for duplicate team name (excluding current team)
        if (!StringUtils.isEmpty(teamDto.getTeamName()) &&
                !existingTeam.getTeamName().equalsIgnoreCase(teamDto.getTeamName())) {
            List<PickleballTeamDto> existingTeams = getTeams(userId, academyId, null, null);
            if (existingTeams.stream().anyMatch(
                    team -> !team.getId().equals(teamId) &&
                            team.getTeamName().equalsIgnoreCase(teamDto.getTeamName()))) {
                throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Team name already exists");
            }
            existingTeam.setTeamName(teamDto.getTeamName());
        }

        // Handle players update
        if (!CollectionUtils.isEmpty(teamDto.getPlayers())) {
            if (addPlayersMode) {
                // Add new players to existing ones
                List<PickleballTeamPlayerMapping> currentPlayers = existingTeam.getPlayers() != null
                        ? new ArrayList<>(existingTeam.getPlayers())
                        : new ArrayList<>();

                validateNoDuplicatePlayers(currentPlayers, teamDto.getPlayers());

                List<PickleballTeamPlayerMapping> newPlayerMappings = buildTeamPlayerMapping(teamId,
                        teamDto.getPlayers());
                currentPlayers.addAll(newPlayerMappings);
                existingTeam.setPlayers(currentPlayers);
            } else {
                // Replace all players (existing behavior)
                existingTeam.setPlayers(buildTeamPlayerMapping(teamId, teamDto.getPlayers()));
            }
        }

        // Update tournament association if provided
        if (!StringUtils.isEmpty(teamDto.getPickleballTournamentId())) {
            existingTeam.setPickleballTournament(
                    PickleballTournament.builder().id(teamDto.getPickleballTournamentId()).build());
        }

        teamRepo.save(existingTeam);

        if (academyId != null) {
            return getTeam(academyId, teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        } else {
            return getTeam(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        }
    }

    @Override
    public List<TeamPerformanceDto> getTeamPerformance(String tournamentId) {
        var rawResults = pickleballTeamRepo.getTeamPerformance(tournamentId);
        return rawResults.stream().map(record -> {
            var totalPlayed = ((Number) record[2]).intValue();
            var matchesWon = ((Number) record[3]).intValue();
            var matchesLost = ((Number) record[4]).intValue();
            double winPercentage = Math.round((100.0 * matchesWon / totalPlayed) * 100.0) / 100.0;
            double lossPercentage = Math.round((100.0 * matchesLost / totalPlayed) * 100.0) / 100.0;

            var dto = new TeamPerformanceDto();
            dto.setTeamId((String) record[0]);
            dto.setTeamName((String) record[1]);
            dto.setMatchesPlayed(((Number) record[2]).intValue());
            dto.setMatchesWon(((Number) record[3]).intValue());
            dto.setMatchesTied(((Number) record[4]).intValue());
            dto.setMatchesLost(((Number) record[5]).intValue());
            dto.setWinPercentage(winPercentage);
            dto.setLossPercentage(lossPercentage);
            // dto.setTotalPoints(((Number) record[6]).intValue());
            // TODO: For Smash Premier League, calculating matchesWon * 2
            dto.setTotalPoints(matchesWon >= 0 ? matchesWon * 2 : 0);
            return dto;
        }).toList();
    }

    @Override
    public PickleballTeamDto addPlayersToTeam(String userId, String teamId, List<PickleBallTeamPlayerDto> newPlayers)
            throws ResourceException {
        return addPlayersToTeam(userId, null, teamId, newPlayers);
    }

    @Override
    public void removePlayerFromTeam(String userId, String academyId, String teamId, String playerUserId)
            throws ResourceException {
        log.info("Removing player {} from team: {}", playerUserId, teamId);

        PickleballTeam team;
        if (academyId != null) {
            team = teamRepo.findByIdAndAcademy_Id(teamId, academyId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found: " + teamId));
        } else {
            team = teamRepo.findByIdAndAcademyIsNull(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found: " + teamId));
        }

        team.getPlayers().removeIf(mapping -> mapping.getPlayerUserProfile().getId().equals(playerUserId));

        teamRepo.save(team);
    }

    @Override
    public Optional<PickleballTeamDto> getTeam(String teamId) throws ResourceException {
        PickleballTeam team = teamRepo.findByAcademyIsNullAndId(teamId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));

        if (!team.getInactive()) {
            PickleballTeamDto teamDto = modelMapper.map(team, PickleballTeamDto.class);

            List<String> userIds = teamDto.getPlayers().stream()
                    .filter(teamPlayerDto -> !StringUtils.isEmpty(teamPlayerDto.getPlayerUserId()))
                    .map(PickleBallTeamPlayerDto::getPlayerUserId).toList();
            if (userIds.isEmpty()) {
                return Optional.of(teamDto);
            }

            List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(userIds);
            Map<String, UserProfileDto> userProfileDtoMap = userProfileDtos.stream()
                    .collect(Collectors.toMap(UserProfileDto::getId, userProfileDto -> userProfileDto));
            teamDto.setPlayers(teamDto.getPlayers().stream().peek(teamPlayer -> {
                UserProfileDto userProfileDto = userProfileDtoMap.get(teamPlayer.getPlayerUserId());
                if (userProfileDto != null) {
                    teamPlayer.setUserProfile(modelMapper.map(userProfileDto, UserProfileMinDto.class));
                }
            }).toList());
            return Optional.of(teamDto);
        }
        throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found");
    }

    @Override
    public Optional<PickleballTeamDto> getTeam(String academyId, String teamId) throws ResourceException {
        academyService.getAcademyById(academyId);
        PickleballTeam team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));

        if (!team.getInactive()) {
            PickleballTeamDto teamDto = modelMapper.map(team, PickleballTeamDto.class);
            List<String> userIds = teamDto.getPlayers().stream()
                    .filter(teamPlayerDto -> !StringUtils.isEmpty(teamPlayerDto.getPlayerUserId()))
                    .map(PickleBallTeamPlayerDto::getPlayerUserId).toList();
            if (userIds.isEmpty()) {
                return Optional.of(teamDto);
            }
            List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(userIds);
            Map<String, UserProfileDto> userProfileDtoMap = userProfileDtos.stream()
                    .collect(Collectors.toMap(UserProfileDto::getId, userProfileDto -> userProfileDto));
            teamDto.setPlayers(teamDto.getPlayers().stream().peek(teamPlayer -> {
                UserProfileDto userProfileDto = userProfileDtoMap.get(teamPlayer.getPlayerUserId());
                if (userProfileDto != null) {
                    teamPlayer.setUserProfile(modelMapper.map(userProfileDto, UserProfileMinDto.class));
                }
            }).toList());
            return Optional.of(teamDto);
        }

        throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found");
    }

    @Override
    public PickleballTeamDto convertToDto(PickleballTeam team) {
        PickleballTeamDto teamDto = new PickleballTeamDto();
        teamDto.setId(team.getId());
        teamDto.setTeamName(team.getTeamName());

        if (team.getCreatedByUserProfile() != null) {
            teamDto.setCreatedByUserProfile(modelMapper.map(team.getCreatedByUserProfile(), UserProfileMinDto.class));
        }

        if (!CollectionUtils.isEmpty(team.getPlayers())) {
            teamDto.setPlayers(team.getPlayers()
                    .stream()
                    .map(teamPlayerMapping -> {
                        PickleBallTeamPlayerDto teamPlayerDto = new PickleBallTeamPlayerDto();
                        teamPlayerDto.setGuestPlayerName(teamPlayerMapping.getGuestPlayerName());
                        if (teamPlayerMapping.getPlayerUserProfile() != null) {
                            teamPlayerDto.setPlayerUserId(teamPlayerMapping.getPlayerUserProfile().getId());
                            teamPlayerDto.setUserProfile(
                                    modelMapper.map(teamPlayerMapping.getPlayerUserProfile(), UserProfileMinDto.class));
                        }
                        return teamPlayerDto;
                    }).toList());
        }

        if (team.getPickleballTournament() != null) {
            teamDto.setPickleballTournamentId(team.getPickleballTournament().getId());
        }

        return teamDto;
    }

    private List<PickleballTeamPlayerMapping> buildTeamPlayerMapping(String teamId,
                                                                     List<PickleBallTeamPlayerDto> playerDtos) {
        return playerDtos.stream()
                .map(playerDto -> {
                    PickleballTeamPlayerMapping teamPlayerMapping = new PickleballTeamPlayerMapping();
                    teamPlayerMapping.setTeam(PickleballTeam.builder().id(teamId).build());

                    if (StringUtils.isNotEmpty(playerDto.getPlayerUserId())) {
                        teamPlayerMapping
                                .setPlayerUserProfile(UserProfile.builder().id(playerDto.getPlayerUserId()).build());
                    }
                    teamPlayerMapping.setGuestPlayerName(playerDto.getGuestPlayerName());
                    return teamPlayerMapping;
                })
                .toList();
    }

    private List<PickleballTeamDto> sortTeams(List<PickleballTeamDto> teamDtos) {
        return teamDtos.stream().parallel().sorted(Comparator.comparing(PickleballTeamDto::getTeamName))
                .collect(Collectors.toList());
    }

    /**
     * Validate that the user is a member of the team (can edit it)
     *
     * @param userId    User ID
     * @param teamId    Team ID
     * @param academyId Academy ID (can be null)
     * @throws ResourceException if user is not authorized
     */
    private void validateUserIsTeamMember(String userId, String teamId, String academyId) throws ResourceException {
        PickleballTeamDto team;
        if (academyId != null) {
            team = getTeam(academyId, teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        } else {
            team = getTeam(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        }

        // Check if user is the team creator
        if (team.getCreatedByUserProfile() != null && team.getCreatedByUserProfile().getId().equals(userId)) {
            return;
        }

        // Check if user is a player in the team
        boolean isTeamMember = team.getPlayers().stream().anyMatch(player -> userId.equals(player.getPlayerUserId()));

        if (!isTeamMember) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "User is not authorized to edit this team");
        }
    }

    /**
     * Validate that there are no duplicate players in the team
     *
     * @param currentPlayers List of current players in the team
     * @param newPlayers     List of new players to add
     * @throws ResourceException if there are duplicate players
     */
    private void validateNoDuplicatePlayers(List<PickleballTeamPlayerMapping> currentPlayers,
                                            List<PickleBallTeamPlayerDto> newPlayers) throws ResourceException {
        Set<String> currentPlayerIds = currentPlayers.stream()
                .filter(player -> player.getPlayerUserProfile() != null)
                .map(player -> player.getPlayerUserProfile().getId())
                .collect(Collectors.toSet());

        Set<String> currentGuestNames = currentPlayers.stream()
                .filter(player -> !StringUtils.isEmpty(player.getGuestPlayerName()))
                .map(player -> player.getGuestPlayerName().toLowerCase())
                .collect(Collectors.toSet());

        // Check for duplicate in new players
        for (PickleBallTeamPlayerDto newPlayer : newPlayers) {
            if (!StringUtils.isEmpty(newPlayer.getPlayerUserId())
                    && currentPlayerIds.contains(newPlayer.getPlayerUserId())) {
                throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Player is already part of this team");
            }

            if (!StringUtils.isEmpty(newPlayer.getGuestPlayerName())
                    && currentGuestNames.contains(newPlayer.getGuestPlayerName().toLowerCase())) {
                throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT,
                        "Guest player with this name already exists in the team");
            }
        }
    }
}
