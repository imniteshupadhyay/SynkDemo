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
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.BadmintonTournament;
import com.playmotech.api.core.dao_postgres.Team;
import com.playmotech.api.core.dao_postgres.TeamPlayerMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.PickleballTeamDto;
import com.playmotech.api.core.dto.TeamDto;
import com.playmotech.api.core.dto.TeamPerformanceDto;
import com.playmotech.api.core.dto.TeamPlayerDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.TeamRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IPickleballTeamService;
import com.playmotech.api.core.services.ITeamsService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.utils.EntityToDtoUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TeamService implements ITeamsService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final TeamRepo teamRepo;
    private final IAcademyService academyService;
    private final IUserProfileService userProfileService;
    private final IPickleballTeamService pickleballTeamService;
    private final EntityToDtoUtils entityToDtoUtilsConverter;

    public TeamService(final TeamRepo teamRepo,
            final IAcademyService academyService,
            final IUserProfileService userProfileService,
            final IPickleballTeamService pickleballTeamService,
            final EntityToDtoUtils entityToDtoUtilsConverter) {
        this.teamRepo = teamRepo;
        this.academyService = academyService;
        this.userProfileService = userProfileService;
        this.pickleballTeamService = pickleballTeamService;
        this.entityToDtoUtilsConverter = entityToDtoUtilsConverter;
    }

    @Override
    public TeamDto addTeam(String userId, String academyId, TeamDto teamDto, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballTeamDto pickleballTeamDto = entityToDtoUtilsConverter.convertTeamDtoToPickleballTeamDto(teamDto);

            PickleballTeamDto addedPickleballTeamDto = pickleballTeamService.addTeam(userId, academyId,
                    pickleballTeamDto);

            return entityToDtoUtilsConverter.convertPickleballTeamDtoToTeamDto(addedPickleballTeamDto);
        } else {
            // Only validate academy if academyId is provided (not null for non-academy
            // tournaments)
            if (academyId != null) {
                academyService.getAcademyById(academyId);
            }

            List<TeamDto> existingTeams = getTeams(userId, academyId, null, null, Sports.BADMINTON);
            if (!existingTeams.isEmpty() && existingTeams.stream()
                    .anyMatch(team -> team.getTeamName().equalsIgnoreCase(teamDto.getTeamName()))) {
                throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Team already exists");
            }
            Team team = new Team();
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
            if (!StringUtils.isEmpty(teamDto.getBadmintonTournamentId())) {
                team.setBadmintonTournament(
                        BadmintonTournament.builder().id(teamDto.getBadmintonTournamentId()).build());
            }

            teamRepo.save(team);

            // Use appropriate getTeam method based on whether academyId is null
            if (academyId != null) {
                return getTeam(academyId, team.getId(), sport)
                        .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
            } else {
                return getTeam(team.getId(), sport)
                        .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
            }
        }
    }

    @Override
    public TeamDto addTeam(String userId, TeamDto teamDto, Sports sport) throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballTeamDto pickleballTeamDto = entityToDtoUtilsConverter.convertTeamDtoToPickleballTeamDto(teamDto);

            PickleballTeamDto addedPickleballTeamDto = pickleballTeamService.addTeam(userId, pickleballTeamDto);

            return entityToDtoUtilsConverter.convertPickleballTeamDtoToTeamDto(addedPickleballTeamDto);
        } else {
            List<TeamDto> existingTeams = getTeams(userId, null, null, sport);
            if (!existingTeams.isEmpty() && existingTeams.stream()
                    .anyMatch(team -> team.getTeamName().equalsIgnoreCase(teamDto.getTeamName()))) {
                throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Team with same name already exist");
            }
            Team team = new Team();
            team.setId(UUID.randomUUID().toString());
            team.setTeamName(teamDto.getTeamName());
            team.setCreatedOn(Timestamp.from(Instant.now()));
            team.setInactive(false);
            team.setPlayers(buildTeamPlayerMapping(team.getId(), teamDto.getPlayers()));
            team.setCreatedByUserProfile(UserProfile.builder().id(userId).build());

            if (!StringUtils.isEmpty(teamDto.getBadmintonTournamentId())) {
                team.setBadmintonTournament(
                        BadmintonTournament.builder().id(teamDto.getBadmintonTournamentId()).build());
            }

            teamRepo.save(team);
            return getTeam(team.getId(), sport)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        }
    }

    @Override
    public List<TeamDto> getTeams(String userId, String academyId, String badminstonTournamentId, String searchTxt,
            Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            List<PickleballTeamDto> teams = pickleballTeamService.getTeams(userId, academyId, badminstonTournamentId,
                    searchTxt);

            return teams.stream()
                    .map(entityToDtoUtilsConverter::convertPickleballTeamDtoToTeamDto)
                    .toList();
        }

        academyService.getAcademyById(academyId);
        List<Team> teams;
        if (!StringUtils.isEmpty(badminstonTournamentId)) {
            teams = teamRepo.findByAcademy_IdAndBadmintonTournament_Id(academyId, badminstonTournamentId).stream()
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
        List<TeamDto> teamDtos = convertToDto(teams);
        return sortTeams(teamDtos);
    }

    @Override
    public List<TeamDto> getTeams(String userId, String searchTxt, String badmintonTournament, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            List<PickleballTeamDto> teams = pickleballTeamService.getTeams(userId, searchTxt, badmintonTournament);

            return teams.stream()
                    .map(entityToDtoUtilsConverter::convertPickleballTeamDtoToTeamDto)
                    .toList();
        }
        List<Team> teams = new ArrayList<>();
        if (StringUtils.isNotEmpty(badmintonTournament)) {
            // teams = teams.stream().filter(team -> team.getBadmintonTournament() != null
            // &&
            // team.getBadmintonTournament().getId().equalsIgnoreCase(badmintonTournament)).toList();

            teams = teamRepo.findTeamsByTournamentId(badmintonTournament);
        } else {
            teams = teamRepo.findTeamsByCreatorOrPlayer(userId);
        }

        // filter out inactive teams
        teams = teams.stream().filter(each -> !each.getInactive()).toList();

        if (!StringUtils.isEmpty(searchTxt) && searchTxt.length() > 2) {
            teams = teams.stream().filter(team -> team.getTeamName().toLowerCase().contains(searchTxt.toLowerCase()))
                    .toList();
        }

        if (teams.isEmpty()) {
            return List.of();
        }

        List<TeamDto> teamDtos = convertToDto(teams);
        return sortTeams(teamDtos);
    }

    @Override
    public Optional<TeamDto> getTeam(String academyId, String teamId, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            Optional<PickleballTeamDto> team = pickleballTeamService.getTeam(academyId, teamId);
            if (!team.isPresent() && team.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(entityToDtoUtilsConverter.convertPickleballTeamDtoToTeamDto(team.get()));
        }

        academyService.getAcademyById(academyId);
        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        if (!team.getInactive()) {
            TeamDto teamDto = modelMapper.map(team, TeamDto.class);
            List<String> userIds = teamDto.getPlayers().stream()
                    .filter(teamPlayerDto -> !StringUtils.isEmpty(teamPlayerDto.getPlayerUserId()))
                    .map(TeamPlayerDto::getPlayerUserId).toList();
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
    public Optional<TeamDto> getTeam(String teamId, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            Optional<PickleballTeamDto> team = pickleballTeamService.getTeam(teamId);

            if (team.isEmpty() || !team.isPresent()) {
                return Optional.empty();
            }
            return Optional.of(entityToDtoUtilsConverter.convertPickleballTeamDtoToTeamDto(team.get()));
        }
        Team team = teamRepo.findByAcademyIsNullAndId(teamId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        if (!team.getInactive()) {
            TeamDto teamDto = modelMapper.map(team, TeamDto.class);
            List<String> userIds = teamDto.getPlayers().stream()
                    .filter(teamPlayerDto -> !StringUtils.isEmpty(teamPlayerDto.getPlayerUserId()))
                    .map(TeamPlayerDto::getPlayerUserId).toList();
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
    public void deleteTeam(String academyId, String teamId, boolean removeBadmintonTournament,
            String badmintonTournamentId, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballTeamService.deleteTeam(academyId, teamId, removeBadmintonTournament, badmintonTournamentId);
        } else {
            academyService.getAcademyById(academyId);
            Team team = teamRepo.findById(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
            if (removeBadmintonTournament) {
                if (team.getBadmintonTournament() != null
                        && team.getBadmintonTournament().getId().equalsIgnoreCase(badmintonTournamentId)) {
                    team.setBadmintonTournament(null);
                }
            } else {
                team.setInactive(true);
            }
            teamRepo.save(team);
        }
    }

    @Override
    public void deleteTeam(String teamId, boolean removeBadmintonTournament, String badmintonTournamentId, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballTeamService.deleteTeam(teamId, removeBadmintonTournament, badmintonTournamentId);
        } else {
            Team team = teamRepo.findByAcademyIsNullAndId(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
            if (removeBadmintonTournament) {
                if (team.getBadmintonTournament() != null
                        && team.getBadmintonTournament().getId().equalsIgnoreCase(badmintonTournamentId)) {
                    team.setBadmintonTournament(null);
                }
            } else {
                team.setInactive(true);
            }
            teamRepo.save(team);
        }
    }

    @Override
    public List<TeamDto> convertToDto(List<Team> teams) {
        return teams.stream().map(this::convertToDto).toList();
    }

    @Override
    public TeamDto convertToDto(Team team) {
        TeamDto teamDto = new TeamDto();
        teamDto.setId(team.getId());
        teamDto.setTeamName(team.getTeamName());
        if (team.getCreatedByUserProfile() != null) {
            teamDto.setCreatedByUserProfile(modelMapper.map(team.getCreatedByUserProfile(), UserProfileMinDto.class));
        }
        if (!CollectionUtils.isEmpty(team.getPlayers())) {
            teamDto.setPlayers(team.getPlayers().stream().map(teamPlayerMapping -> {
                TeamPlayerDto teamPlayerDto = new TeamPlayerDto();
                teamPlayerDto.setGuestPlayerName(teamPlayerMapping.getGuestPlayerName());
                if (teamPlayerMapping.getPlayerUserProfile() != null) {
                    teamPlayerDto.setPlayerUserId(teamPlayerMapping.getPlayerUserProfile().getId());
                    teamPlayerDto.setUserProfile(
                            modelMapper.map(teamPlayerMapping.getPlayerUserProfile(), UserProfileMinDto.class));
                }
                return teamPlayerDto;
            }).toList());
        }
        if (team.getBadmintonTournament() != null) {
            teamDto.setBadmintonTournamentId(team.getBadmintonTournament().getId());
        }
        return teamDto;
    }

    @Override
    public TeamDto updateTeam(String userId, String academyId, String teamId, TeamDto teamDto, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballTeamDto dto = pickleballTeamService.updateTeam(userId, academyId, teamId,
                    entityToDtoUtilsConverter.convertTeamDtoToPickleballTeamDto(teamDto));

            return entityToDtoUtilsConverter.convertPickleballTeamDtoToTeamDto(dto);
        }
        // Only validate academy if academyId is provided
        if (academyId != null) {
            academyService.getAcademyById(academyId);
        }

        Team existingTeam;
        if (academyId != null) {
            existingTeam = teamRepo.findByAcademy_IdAndId(academyId, teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        } else {
            existingTeam = teamRepo.findByAcademyIsNullAndId(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        }

        // Check for duplicate team name (excluding current team)
        List<TeamDto> existingTeams = getTeams(userId, academyId, null, null);
        if (existingTeams.stream().anyMatch(
                team -> !team.getId().equals(teamId) && team.getTeamName().equalsIgnoreCase(teamDto.getTeamName()))) {
            throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Team name already exists");
        }

        // Update team properties
        if (!StringUtils.isEmpty(teamDto.getTeamName())) {
            existingTeam.setTeamName(teamDto.getTeamName());
        }

        // Update players if provided
        if (!CollectionUtils.isEmpty(teamDto.getPlayers())) {
            existingTeam.setPlayers(buildTeamPlayerMapping(teamId, teamDto.getPlayers()));
        }

        // Update tournament association if provided
        if (!StringUtils.isEmpty(teamDto.getBadmintonTournamentId())) {
            existingTeam.setBadmintonTournament(
                    BadmintonTournament.builder().id(teamDto.getBadmintonTournamentId()).build());
        }

        teamRepo.save(existingTeam);

        // Return updated team using appropriate getTeam method
        if (academyId != null) {
            return getTeam(academyId, teamId, sport)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        } else {
            return getTeam(teamId, sport)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        }
    }

    @Override
    public TeamDto updateTeam(String userId, String teamId, TeamDto teamDto, Sports sport) throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballTeamDto dto = pickleballTeamService.updateTeam(userId, teamId,
                    entityToDtoUtilsConverter.convertTeamDtoToPickleballTeamDto(teamDto));

            return entityToDtoUtilsConverter.convertPickleballTeamDtoToTeamDto(dto);
        }

        Team existingTeam = teamRepo.findByAcademyIsNullAndId(teamId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));

        // Check for duplicate team name (excluding current team)
        List<TeamDto> existingTeams = getTeams(userId, null, null, sport);
        if (existingTeams.stream().anyMatch(
                team -> !team.getId().equals(teamId) && team.getTeamName().equalsIgnoreCase(teamDto.getTeamName()))) {
            throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Team name already exists");
        }

        // Update team properties
        if (!StringUtils.isEmpty(teamDto.getTeamName())) {
            existingTeam.setTeamName(teamDto.getTeamName());
        }

        // Update players if provided
        if (!CollectionUtils.isEmpty(teamDto.getPlayers())) {
            existingTeam.setPlayers(buildTeamPlayerMapping(teamId, teamDto.getPlayers()));
        }

        // Update tournament association if provided
        if (!StringUtils.isEmpty(teamDto.getBadmintonTournamentId())) {
            existingTeam.setBadmintonTournament(
                    BadmintonTournament.builder().id(teamDto.getBadmintonTournamentId()).build());
        }

        teamRepo.save(existingTeam);

        return getTeam(teamId, sport)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
    }

    private List<TeamDto> sortTeams(List<TeamDto> teamDtos) {
        return teamDtos.stream().parallel().sorted(Comparator.comparing(TeamDto::getTeamName))
                .collect(Collectors.toList());
    }

    private List<TeamPlayerMapping> buildTeamPlayerMapping(String teamId, List<TeamPlayerDto> playerDtos) {
        return playerDtos.stream().map(playerDto -> {
            TeamPlayerMapping teamPlayerMapping = new TeamPlayerMapping();
            teamPlayerMapping.setTeam(Team.builder().id(teamId).build());
            if (!StringUtils.isEmpty(playerDto.getPlayerUserId())) {
                teamPlayerMapping.setPlayerUserProfile(UserProfile.builder().id(playerDto.getPlayerUserId()).build());
            }
            teamPlayerMapping.setGuestPlayerName(playerDto.getGuestPlayerName());
            return teamPlayerMapping;
        }).toList();
    }

    @Override
    public List<TeamPerformanceDto> getTeamPerformance(String tournamentId) {
        var rawResults = teamRepo.getTeamPerformance(tournamentId);
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

    /**
     * Add new players to an existing team (Academy-based teams)
     *
     * @param userId     User performing the operation
     * @param academyId  Academy ID
     * @param teamId     Team ID
     * @param newPlayers List of new players to add
     * @return Updated team DTO
     * @throws ResourceException if operation fails
     */
    @Override
    public TeamDto addPlayersToTeam(String userId, String academyId, String teamId, List<TeamPlayerDto> newPlayers,
            Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            List<PickleBallTeamPlayerDto> dtos = newPlayers.stream()
                    .map(entityToDtoUtilsConverter::convertTeamPlayerDtoToPickleballTeamPlayerDto)
                    .toList();
            PickleballTeamDto pickleballTeamDto = pickleballTeamService.addPlayersToTeam(userId, academyId, teamId,
                    dtos);

            return entityToDtoUtilsConverter.convertPickleballTeamDtoToTeamDto(pickleballTeamDto);
        }
        // Validate user is part of the team
        validateUserIsTeamMember(userId, teamId, academyId, sport);

        if (academyId != null) {
            academyService.getAcademyById(academyId);
        }

        Team existingTeam;

        if (academyId != null) {
            existingTeam = teamRepo.findByAcademy_IdAndId(academyId, teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found"));
        } else {
            existingTeam = teamRepo.findByAcademyIsNullAndId(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found"));
        }

        // Get current players
        List<TeamPlayerMapping> currentPlayers = existingTeam.getPlayers() != null ? existingTeam.getPlayers()
                : new ArrayList<>();

        // Check for duplicate players before adding
        validateNoDuplicatePlayers(currentPlayers, newPlayers);

        // Add new players to existing list
        List<TeamPlayerMapping> newPlayerMappings = buildTeamPlayerMapping(teamId, newPlayers);
        currentPlayers.addAll(newPlayerMappings);

        // Update the team with combined player list
        existingTeam.setPlayers(currentPlayers);
        teamRepo.save(existingTeam);

        // Return updated team
        if (academyId != null) {
            return getTeam(academyId, teamId, sport)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        } else {
            return getTeam(teamId, sport)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        }

    }

    /**
     * Add new players to an existing team (Non-academy teams)
     *
     * @param userId     User performing the operation
     * @param teamId     Team ID
     * @param newPlayers List of new players to add
     * @return Updated team DTO
     * @throws ResourceException if operation fails
     */
    @Override
    public TeamDto addPlayersToTeam(String userId, String teamId, List<TeamPlayerDto> newPlayers, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            List<PickleBallTeamPlayerDto> dtos = newPlayers.stream()
                    .map(entityToDtoUtilsConverter::convertTeamPlayerDtoToPickleballTeamPlayerDto)
                    .toList();
            PickleballTeamDto pickleballTeamDto = pickleballTeamService.addPlayersToTeam(userId, null, teamId, dtos);

            return entityToDtoUtilsConverter.convertPickleballTeamDtoToTeamDto(pickleballTeamDto);
        }
        return addPlayersToTeam(userId, null, teamId, newPlayers, sport);
    }

    /**
     * Update team with option to add players or update other properties
     * Enhanced version that preserves existing players when adding new ones
     */
    @Override
    public TeamDto updateTeamWithAddPlayers(String userId, String academyId, String teamId,
            TeamDto teamDto, boolean addPlayersMode, Sports sport) throws ResourceException {
        if (Sports.BADMINTON.equals(sport)) {
            PickleballTeamDto dto = pickleballTeamService.updateTeamWithAddPlayers(userId, academyId, teamId,
                    entityToDtoUtilsConverter.convertTeamDtoToPickleballTeamDto(teamDto), addPlayersMode);

            return entityToDtoUtilsConverter.convertPickleballTeamDtoToTeamDto(dto);
        }
        // Validate user is part of the team
        validateUserIsTeamMember(userId, teamId, academyId, sport);

        if (academyId != null) {
            academyService.getAcademyById(academyId);
        }

        Team existingTeam;
        if (academyId != null) {
            existingTeam = teamRepo.findByAcademy_IdAndId(academyId, teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found"));
        } else {
            existingTeam = teamRepo.findByAcademyIsNullAndId(teamId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "Team not found"));
        }

        // Check for duplicate team name (excluding current team)
        if (!StringUtils.isEmpty(teamDto.getTeamName()) &&
                !existingTeam.getTeamName().equalsIgnoreCase(teamDto.getTeamName())) {
            List<TeamDto> existingTeams = getTeams(userId, academyId, null, null);
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
                List<TeamPlayerMapping> currentPlayers = existingTeam.getPlayers() != null
                        ? new ArrayList<>(existingTeam.getPlayers())
                        : new ArrayList<>();

                validateNoDuplicatePlayers(currentPlayers, teamDto.getPlayers());

                List<TeamPlayerMapping> newPlayerMappings = buildTeamPlayerMapping(teamId, teamDto.getPlayers());
                currentPlayers.addAll(newPlayerMappings);
                existingTeam.setPlayers(currentPlayers);
            } else {
                // Replace all players (existing behavior)
                existingTeam.setPlayers(buildTeamPlayerMapping(teamId, teamDto.getPlayers()));
            }
        }

        // Update tournament association if provided
        if (!StringUtils.isEmpty(teamDto.getBadmintonTournamentId())) {
            existingTeam.setBadmintonTournament(
                    BadmintonTournament.builder().id(teamDto.getBadmintonTournamentId()).build());
        }

        teamRepo.save(existingTeam);

        // Return updated team
        if (academyId != null) {
            return getTeam(academyId, teamId, sport)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        } else {
            return getTeam(teamId, sport)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        }

    }

    /**
     * Validate that no duplicate players are being added
     *
     * @param currentPlayers Current team players
     * @param newPlayers     New players to add
     * @throws ResourceException if duplicates found
     */
    private void validateNoDuplicatePlayers(List<TeamPlayerMapping> currentPlayers, List<TeamPlayerDto> newPlayers)
            throws ResourceException {
        Set<String> currentPlayerIds = currentPlayers.stream()
                .filter(player -> player.getPlayerUserProfile() != null)
                .map(player -> player.getPlayerUserProfile().getId())
                .collect(Collectors.toSet());

        Set<String> currentGuestNames = currentPlayers.stream()
                .filter(player -> !StringUtils.isEmpty(player.getGuestPlayerName()))
                .map(player -> player.getGuestPlayerName().toLowerCase())
                .collect(Collectors.toSet());

        // Check for duplicates in new players
        for (TeamPlayerDto newPlayer : newPlayers) {
            if (!StringUtils.isEmpty(newPlayer.getPlayerUserId())
                    && currentPlayerIds.contains(newPlayer.getPlayerUserId())) {
                throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT,
                        "Player is already part of this team");
            }
            if (!StringUtils.isEmpty(newPlayer.getGuestPlayerName()) &&
                    currentGuestNames.contains(newPlayer.getGuestPlayerName().toLowerCase())) {
                throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT,
                        "Guest player with this name already exists in the team");
            }
        }
    }

    /**
     * Validate that the user is a member of the team (can edit it)
     *
     * @param userId    User ID
     * @param teamId    Team ID
     * @param academyId Academy ID (can be null)
     * @throws ResourceException if user is not authorized
     */
    private void validateUserIsTeamMember(String userId, String teamId, String academyId, Sports sport)
            throws ResourceException {
        TeamDto team;
        if (academyId != null) {
            team = getTeam(academyId, teamId, sport)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
        } else {
            team = getTeam(teamId, sport)
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
}
