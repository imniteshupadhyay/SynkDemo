package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.MediaType;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.TournamentStatus;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.BadmintonTournament;
import com.playmotech.api.core.dao_postgres.BadmintonTournamentGalleryMedia;
import com.playmotech.api.core.dao_postgres.BadmintonTournamentPlayer;
import com.playmotech.api.core.dao_postgres.Team;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AcademyMinDto;
import com.playmotech.api.core.dto.BadmintonTournamentDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.MediaDto;
import com.playmotech.api.core.dto.PaginatedResponse;
import com.playmotech.api.core.dto.PickleballTournamentDto;
import com.playmotech.api.core.dto.PlayerPerformanceDto;
import com.playmotech.api.core.dto.TournamentPerformanceDto;
import com.playmotech.api.core.dto.TournamentTeamMappingDto;
import com.playmotech.api.core.dto.UpdateBadmintonTournamentDto;
import com.playmotech.api.core.dto.UpdatePickleballTournamentDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.BadmintonTournamentGalleryMediaRepo;
import com.playmotech.api.core.repo.BadmintonTournamentPlayerRepo;
import com.playmotech.api.core.repo.BadmintonTournamentRepo;
import com.playmotech.api.core.repo.TeamRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IBadmintonMatchService;
import com.playmotech.api.core.services.IBadmintonTournamentService;
import com.playmotech.api.core.services.IPickleballTournamentService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.ITeamsService;
import com.playmotech.api.core.specification.BadmintonTournamentSpecification;
import com.playmotech.api.core.utils.DateTimeUtils;
import com.playmotech.api.core.utils.EntityToDtoUtils;
import com.playmotech.api.core.utils.GenericFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BadmintonTournamentService implements IBadmintonTournamentService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final IAcademyService academyService;
    private final ITeamsService teamsService;
    private final BadmintonTournamentRepo badmintonTournamentRepo;
    private final IBadmintonMatchService badmintonMatchService;
    private final IStorageService storageService;
    private final BadmintonTournamentGalleryMediaRepo badmintonTournamentGalleryMediaRepo;
    private final BadmintonTournamentPlayerRepo badmintonTournamentPlayerRepo;
    private final TeamRepo teamRepo;
    private final UserProfileRepo userProfileRepo;
    private final IPushNotificationService pushNotificationService;
    private final IPickleballTournamentService pickleballTournamentService;
    private final EntityToDtoUtils entityToDtoUtilsConverter;

    @Value("${tournaments-media-base-url}")
    private String tournamentsMediaBaseUrl;

    @Value("${storage.tournaments-media-bucket}")
    private String tournamentsMediaBucket;

    public BadmintonTournamentService(final IAcademyService academyService, final ITeamsService teamsService,
            final BadmintonTournamentRepo badmintonTournamentRepo, final IBadmintonMatchService badmintonMatchService,
            final IStorageService storageService,
            final BadmintonTournamentGalleryMediaRepo badmintonTournamentGalleryMediaRepo,
            final BadmintonTournamentPlayerRepo badmintonTournamentPlayerRepo, final TeamRepo teamRepo,
            final UserProfileRepo userProfileRepo, final IPushNotificationService pushNotificationService,
            IPickleballTournamentService pickleballTournamentService, EntityToDtoUtils entityToDtoUtilsConverter) {
        this.academyService = academyService;
        this.teamsService = teamsService;
        this.badmintonTournamentRepo = badmintonTournamentRepo;
        this.badmintonMatchService = badmintonMatchService;
        this.pickleballTournamentService = pickleballTournamentService;
        this.entityToDtoUtilsConverter = entityToDtoUtilsConverter;
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        this.storageService = storageService;
        this.badmintonTournamentGalleryMediaRepo = badmintonTournamentGalleryMediaRepo;
        this.badmintonTournamentPlayerRepo = badmintonTournamentPlayerRepo;
        this.teamRepo = teamRepo;
        this.userProfileRepo = userProfileRepo;
        this.pushNotificationService = pushNotificationService;
    }

    @Override
    public BadmintonTournamentDto createTournament(String userId, String academyId, BadmintonTournamentDto request,
            Sports sport)
            throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballTournamentDto request2 = entityToDtoUtilsConverter
                    .convertBadmintonTournamentDtoToPickleballTournamentDto(request);
            PickleballTournamentDto tournament = pickleballTournamentService.createTournament(userId, academyId,
                    request2, sport);

            return entityToDtoUtilsConverter.convertPickleballTournamentDtoToBadmintonTournamentDto(tournament);
        }

        academyService.getAcademyById(academyId);

        // Validate TEAMS format requirements
        // if (request.getFormat() == GameFormat.TEAMS) {
        // if (CollectionUtils.isEmpty(request.getTeams())) {
        // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
        // "Teams are required when tournament format is TEAMS");
        // }

        // // Validate each team has at least one player
        // for (TeamDto team : request.getTeams()) {
        // if (CollectionUtils.isEmpty(team.getPlayers())) {
        // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
        // "Each team must have at least one player");
        // }
        // }
        // }

        BadmintonTournament badmintonTournament = new BadmintonTournament();
        String tournamentId = UUID.randomUUID().toString();
        badmintonTournament.setId(tournamentId);
        badmintonTournament.setName(request.getName());
        badmintonTournament.setDescription(request.getDescription());
        badmintonTournament.setFormat(request.getFormat());
        badmintonTournament.setType(request.getType());
        badmintonTournament.setStartDate(request.getStartDate());
        badmintonTournament.setEndDate(request.getEndDate());
        badmintonTournament.setAcademy(Academy.builder().id(academyId).build());
        badmintonTournament.setCreatedOn(Timestamp.from(Instant.now()));
        badmintonTournament.setCreatedByUserProfile(UserProfile.builder().id(userId).build());

        badmintonTournament.setScorerUserProfile(
                StringUtils.isEmpty(request.getScorerUserId()) ? null
                        : UserProfile.builder().id(request.getScorerUserId()).build());

        badmintonTournament.setRefereeUserProfile(StringUtils.isEmpty(request.getRefereeUserId()) ? null
                : UserProfile.builder().id(request.getRefereeUserId()).build());

        badmintonTournament.setMatchOfficialUserProfile(StringUtils.isEmpty(request.getMatchOfficialUserId()) ? null
                : UserProfile.builder().id(request.getMatchOfficialUserId()).build());

        BadmintonTournament savedBadmintonTournament = badmintonTournamentRepo.save(badmintonTournament);

        // Process teams if format is TEAMS
        // if (request.getFormat() == GameFormat.TEAMS &&
        // !CollectionUtils.isEmpty(request.getTeams())) {
        // for (TeamDto teamDto : request.getTeams()) {
        // // Set the tournament ID for each team
        // teamDto.setBadmintonTournamentId(savedBadmintonTournament.getId());
        // try {
        // teamsService.addTeam(userId, academyId, teamDto);
        // } catch (ResourceException e) {
        // log.error("Error creating team {} for tournament {}: {}",
        // teamDto.getTeamName(), savedBadmintonTournament.getId(), e.getMessage());
        // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
        // "Failed to create team: " + teamDto.getTeamName());
        // }
        // }
        // }

        if (StringUtils.isNotEmpty(request.getScorerUserId())) {
            CompletableFuture.runAsync(() -> {
                log.info("Sending notification to scorer");
                UserProfileDto userProfileDto = userProfileRepo.findById(request.getScorerUserId())
                        .map(userProfile -> modelMapper.map(userProfile, UserProfileDto.class)).orElse(null);
                sendNotificationToScorer(Arrays.asList(userProfileDto), tournamentId, academyId);
                log.info("Notification sent to scorer");
            });
        }

        return convertToDto(userId, savedBadmintonTournament, sport);
    }

    @Override
    public BadmintonTournamentDto createTournament(String userId, BadmintonTournamentDto request, Sports sport)
            throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballTournamentDto request2 = entityToDtoUtilsConverter
                    .convertBadmintonTournamentDtoToPickleballTournamentDto(request);
            PickleballTournamentDto tournament = pickleballTournamentService.createTournament(userId, request2, sport);

            return entityToDtoUtilsConverter.convertPickleballTournamentDtoToBadmintonTournamentDto(tournament);
        }

        // Validate TEAMS format requirements
        // if (request.getFormat() == GameFormat.TEAMS) {
        // if (CollectionUtils.isEmpty(request.getTeams())) {
        // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
        // "Teams are required when tournament format is TEAMS");
        // }

        // // Validate each team has at least one player
        // for (TeamDto team : request.getTeams()) {
        // if (CollectionUtils.isEmpty(team.getPlayers())) {
        // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
        // "Each team must have at least one player");
        // }
        // }
        // }

        BadmintonTournament badmintonTournament = new BadmintonTournament();
        String tournamentId = UUID.randomUUID().toString();
        badmintonTournament.setId(tournamentId);
        badmintonTournament.setName(request.getName());
        badmintonTournament.setDescription(request.getDescription());
        badmintonTournament.setFormat(request.getFormat());
        badmintonTournament.setType(request.getType());
        badmintonTournament.setStartDate(request.getStartDate());
        badmintonTournament.setEndDate(request.getEndDate());
        badmintonTournament.setCreatedByUserProfile(UserProfile.builder().id(userId).build());
        badmintonTournament.setCreatedOn(Timestamp.from(Instant.now()));

        badmintonTournament.setScorerUserProfile(
                StringUtils.isEmpty(request.getScorerUserId()) ? null
                        : UserProfile.builder().id(request.getScorerUserId()).build());

        badmintonTournament.setRefereeUserProfile(StringUtils.isEmpty(request.getRefereeUserId()) ? null
                : UserProfile.builder().id(request.getRefereeUserId()).build());

        badmintonTournament.setMatchOfficialUserProfile(StringUtils.isEmpty(request.getMatchOfficialUserId()) ? null
                : UserProfile.builder().id(request.getMatchOfficialUserId()).build());

        BadmintonTournament savedBadmintonTournament = badmintonTournamentRepo.save(badmintonTournament);

        // Process teams if format is TEAMS
        // if (request.getFormat() == GameFormat.TEAMS &&
        // !CollectionUtils.isEmpty(request.getTeams())) {
        // for (TeamDto teamDto : request.getTeams()) {
        // // Set the tournament ID for each team
        // teamDto.setBadmintonTournamentId(savedBadmintonTournament.getId());
        // try {
        // // For non-academy tournaments, pass null as academyId
        // teamsService.addTeam(userId, null, teamDto);
        // } catch (ResourceException e) {
        // log.error("Error creating team {} for tournament {}: {}",
        // teamDto.getTeamName(), savedBadmintonTournament.getId(), e.getMessage());
        // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
        // "Failed to create team: " + teamDto.getTeamName());
        // }
        // }
        // }

        if (StringUtils.isNotEmpty(request.getScorerUserId())) {
            CompletableFuture.runAsync(() -> {
                log.info("Sending notification to scorer");
                UserProfileDto userProfileDto = userProfileRepo.findById(request.getScorerUserId())
                        .map(userProfile -> modelMapper.map(userProfile, UserProfileDto.class)).orElse(null);
                sendNotificationToScorer(Arrays.asList(userProfileDto), tournamentId, null);
                log.info("Notification sent to scorer");
            });
        }

        return convertToDto(userId, savedBadmintonTournament, sport);
    }

    @Override
    public PaginatedResponse<BadmintonTournamentDto> getTournamentsPaginated(String userId, GenericFilter filter,
            Sports sport) throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            PaginatedResponse<PickleballTournamentDto> pickleballTournaments = pickleballTournamentService
                    .getTournamentsPaginated(userId, filter);

            List<BadmintonTournamentDto> list = pickleballTournaments.getBody().stream()
                    .map(entityToDtoUtilsConverter::convertPickleballTournamentDtoToBadmintonTournamentDto)
                    .toList();

            return new PaginatedResponse<BadmintonTournamentDto>(
                    list,
                    pickleballTournaments.getTotalElements(),
                    pickleballTournaments.getTotalPages(),
                    pickleballTournaments.getCurrentPage());
        }

        if (StringUtils.isNotEmpty(filter.getAcademyId())) {
            academyService.getAcademyById(filter.getAcademyId());
        }

        BadmintonTournamentSpecification spec = new BadmintonTournamentSpecification(filter,
                filter.getTournamentStatus());

        Pageable pageable = PageRequest.of(filter.getCurrentPage(), filter.getPageSize());

        Page<BadmintonTournament> tournaments = badmintonTournamentRepo.findAll(spec, pageable);

        long totalElements = tournaments.getTotalElements();
        int totalPages = tournaments.getTotalPages();

        if (tournaments.isEmpty()) {
            return new PaginatedResponse<BadmintonTournamentDto>(List.of(), totalElements, totalPages,
                    filter.getCurrentPage());
        }

        List<BadmintonTournamentDto> list = tournaments.getContent()
                .stream()
                .map(t -> {
                    try {
                        return convertToDto(userId, t, sport);
                    } catch (ResourceException e) {
                        log.error("Error while converting tournament to dto", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return new PaginatedResponse<BadmintonTournamentDto>(list, totalElements, totalPages, filter.getCurrentPage());
    }

    @Override
    public List<BadmintonTournamentDto> getAllTournaments(String userId, String academyId, TournamentStatus status,
            String searchTxt, Sports sport) throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            List<PickleballTournamentDto> pickleballTournamentDtoList = pickleballTournamentService
                    .getAllTournaments(userId, academyId, status, searchTxt, sport);

            return pickleballTournamentDtoList.stream()
                    .map(entityToDtoUtilsConverter::convertPickleballTournamentDtoToBadmintonTournamentDto)
                    .toList();
        }

        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }
        List<BadmintonTournament> badmintonTournaments;

        if (status == TournamentStatus.LIVE) {
            if (StringUtils.isNotEmpty(academyId)) {
                badmintonTournaments = badmintonTournamentRepo
                        .findByAcademy_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(academyId,
                                DateTimeUtils.getTodayDate(), DateTimeUtils.getTodayDate())
                        .stream().filter(badmintonTournament -> !badmintonTournament.isInactive()).toList();
            } else {
                badmintonTournaments = badmintonTournamentRepo
                        .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(DateTimeUtils.getTodayDate(),
                                DateTimeUtils.getTodayDate())
                        .stream().filter(badmintonTournament -> !badmintonTournament.isInactive()).toList();
            }
        } else if (status == TournamentStatus.UPCOMING) {
            if (StringUtils.isNotEmpty(academyId)) {
                badmintonTournaments = badmintonTournamentRepo
                        .findByAcademy_IdAndStartDateGreaterThan(academyId, DateTimeUtils.getTodayDate()).stream()
                        .filter(badmintonTournament -> !badmintonTournament.isInactive()).toList();
            } else {
                badmintonTournaments = badmintonTournamentRepo.findByStartDateGreaterThan(DateTimeUtils.getTodayDate())
                        .stream().filter(badmintonTournament -> !badmintonTournament.isInactive()).toList();
            }
        } else if (status == TournamentStatus.COMPLETED) {
            if (StringUtils.isNotEmpty(academyId)) {
                badmintonTournaments = badmintonTournamentRepo
                        .findByAcademy_IdAndEndDateLessThan(academyId, DateTimeUtils.getTodayDate()).stream()
                        .filter(badmintonTournament -> !badmintonTournament.isInactive()).toList();
            } else {
                badmintonTournaments = badmintonTournamentRepo.findByEndDateLessThan(DateTimeUtils.getTodayDate())
                        .stream().filter(badmintonTournament -> !badmintonTournament.isInactive()).toList();
            }

        } else {
            if (StringUtils.isNotEmpty(academyId)) {
                badmintonTournaments = badmintonTournamentRepo.findByAcademy_Id(academyId).stream()
                        .filter(badmintonTournament -> !badmintonTournament.isInactive()).toList();
            } else {
                badmintonTournaments = StreamSupport.stream(badmintonTournamentRepo.findAll().spliterator(), false)
                        .filter(badmintonTournament -> !badmintonTournament.isInactive()).toList();
            }
        }

        if (!CollectionUtils.isEmpty(badmintonTournaments) && StringUtils.isNotEmpty(searchTxt)
                && searchTxt.length() > 2) {
            badmintonTournaments = badmintonTournaments.stream().filter(
                    badmintonTournament -> StringUtils.containsIgnoreCase(badmintonTournament.getName(), searchTxt))
                    .toList();
        }

        if (CollectionUtils.isEmpty(badmintonTournaments)) {
            return List.of();
        }
        // return badmintonTournaments.stream()
        // .sorted(Comparator.comparing(BadmintonTournament::getCreatedOn).reversed())
        // .map(badmintonTournament -> {
        // try {
        // return convertToDto(userId, badmintonTournament);
        // } catch (ResourceException e) {
        // log.error("Error while converting tournament to dto", e);
        // return null;
        // }
        // }).filter(Objects::nonNull).toList();
        return badmintonTournaments.stream()
                .sorted(Comparator.comparing(BadmintonTournament::getCreatedOn).reversed())
                .map(badmintonTournament -> {
                    try {
                        return convertToDto(userId, badmintonTournament, sport);
                    } catch (ResourceException e) {
                        log.error("Error while converting tournament to dto", e);
                        return null;
                    }
                }).filter(Objects::nonNull)
                .toList();
    }

    @Override
    public BadmintonTournamentDto getTournament(String userId, String academyId, String tournamentId, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballTournamentDto pickleballTournamentDto = pickleballTournamentService.getTournament(userId,
                    academyId, tournamentId, sport);

            return entityToDtoUtilsConverter
                    .convertPickleballTournamentDtoToBadmintonTournamentDto(pickleballTournamentDto);
        }

        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }

        Optional<BadmintonTournament> badmintonTournament;
        if (StringUtils.isNotEmpty(academyId)) {
            badmintonTournament = badmintonTournamentRepo.findByAcademyIsNotNullAndId(tournamentId);
        } else {
            badmintonTournament = badmintonTournamentRepo.findByAcademyIsNullAndId(tournamentId);
        }
        return convertToDto(userId, badmintonTournament
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found")),
                sport);
    }

    @Async
    @Override
    public void deleteTournament(String academyId, String tournamentId, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballTournamentService.deleteTournament(academyId, tournamentId, sport);
        } else {
            academyService.getAcademyById(academyId);
            BadmintonTournament badmintonTournament = badmintonTournamentRepo.findById(tournamentId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));
            badmintonTournament.setInactive(true);
            badmintonTournamentRepo.save(badmintonTournament);
        }
    }

    @Override
    public void deleteTournament(String tournamentId, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballTournamentService.deleteTournament(tournamentId, sport);
        } else {
            BadmintonTournament badmintonTournament = badmintonTournamentRepo.findByAcademyIsNullAndId(tournamentId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));
            badmintonTournament.setInactive(true);
            badmintonTournamentRepo.save(badmintonTournament);
        }
    }

    @Override
    public BadmintonTournamentDto updateTournament(String userId, String academyId, String matchId,
            UpdateBadmintonTournamentDto updateRequest, Sports sport) throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            UpdatePickleballTournamentDto request2 = entityToDtoUtilsConverter
                    .convertUpdateBadmintonTournamentDtoToUpdatePickleballTournamentDto(updateRequest);

            PickleballTournamentDto pickleballTournamentDto = pickleballTournamentService.updateTournament(userId,
                    academyId, matchId, request2, sport);

            return entityToDtoUtilsConverter
                    .convertPickleballTournamentDtoToBadmintonTournamentDto(pickleballTournamentDto);
        }

        academyService.getAcademyById(academyId);
        BadmintonTournament badmintonTournament = badmintonTournamentRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        // Handle basic tournament updates
        if (!StringUtils.isEmpty(updateRequest.getName())) {
            badmintonTournament.setName(updateRequest.getName());
        }
        if (!StringUtils.isEmpty(updateRequest.getDescription())) {
            badmintonTournament.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getFormat() != null) {
            badmintonTournament.setFormat(updateRequest.getFormat());
        }
        if (updateRequest.getType() != null) {
            badmintonTournament.setType(updateRequest.getType());
        }
        if (updateRequest.getStartDate() != null) {
            badmintonTournament.setStartDate(updateRequest.getStartDate());
        }
        if (updateRequest.getEndDate() != null) {
            badmintonTournament.setEndDate(updateRequest.getEndDate());
        }

        // Handle teams update for TEAMS format
        // if (updateRequest.getFormat() == GameFormat.TEAMS ||
        // (updateRequest.getFormat() == null && badmintonTournament.getFormat() ==
        // GameFormat.TEAMS)) {

        // if (!CollectionUtils.isEmpty(updateRequest.getTeams())) {
        // // Validate teams
        // for (TeamDto team : updateRequest.getTeams()) {
        // if (CollectionUtils.isEmpty(team.getPlayers())) {
        // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
        // "Each team must have at least one player");
        // }
        // }

        // // Process team updates
        // for (TeamDto teamDto : updateRequest.getTeams()) {
        // teamDto.setBadmintonTournamentId(matchId);
        // try {
        // if (StringUtils.isEmpty(teamDto.getId())) {
        // // Create new team
        // teamsService.addTeam(userId, academyId, teamDto);
        // } else {
        // // Update existing team
        // teamsService.updateTeam(userId, academyId, teamDto.getId(), teamDto);
        // }
        // } catch (ResourceException e) {
        // log.error("Error processing team {} for tournament {}: {}",
        // teamDto.getTeamName(), matchId, e.getMessage());
        // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
        // "Failed to process team: " + teamDto.getTeamName());
        // }
        // }
        // }
        // }

        BadmintonTournament updatedBadmintonTournament = badmintonTournamentRepo.save(badmintonTournament);
        return convertToDto(userId, updatedBadmintonTournament, sport);
    }

    @Override
    public BadmintonTournamentDto updateTournament(String userId, String matchId,
            UpdateBadmintonTournamentDto updateRequest, Sports sport) throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            UpdatePickleballTournamentDto request2 = entityToDtoUtilsConverter
                    .convertUpdateBadmintonTournamentDtoToUpdatePickleballTournamentDto(updateRequest);

            PickleballTournamentDto pickleballTournamentDto = pickleballTournamentService.updateTournament(userId,
                    matchId, request2, sport);

            return entityToDtoUtilsConverter
                    .convertPickleballTournamentDtoToBadmintonTournamentDto(pickleballTournamentDto);
        }

        BadmintonTournament badmintonTournament = badmintonTournamentRepo.findByAcademyIsNullAndId(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        // Handle basic tournament updates
        if (!StringUtils.isEmpty(updateRequest.getName())) {
            badmintonTournament.setName(updateRequest.getName());
        }
        if (!StringUtils.isEmpty(updateRequest.getDescription())) {
            badmintonTournament.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getFormat() != null) {
            badmintonTournament.setFormat(updateRequest.getFormat());
        }
        if (updateRequest.getType() != null) {
            badmintonTournament.setType(updateRequest.getType());
        }
        if (updateRequest.getStartDate() != null) {
            badmintonTournament.setStartDate(updateRequest.getStartDate());
        }
        if (updateRequest.getEndDate() != null) {
            badmintonTournament.setEndDate(updateRequest.getEndDate());
        }

        // Handle teams update for TEAMS format
        // if (updateRequest.getFormat() == GameFormat.TEAMS ||
        // (updateRequest.getFormat() == null && badmintonTournament.getFormat() ==
        // GameFormat.TEAMS)) {

        // if (!CollectionUtils.isEmpty(updateRequest.getTeams())) {
        // // Validate teams
        // for (TeamDto team : updateRequest.getTeams()) {
        // if (CollectionUtils.isEmpty(team.getPlayers())) {
        // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
        // "Each team must have at least one player");
        // }
        // }

        // // Process team updates
        // for (TeamDto teamDto : updateRequest.getTeams()) {
        // teamDto.setBadmintonTournamentId(matchId);
        // try {
        // if (StringUtils.isEmpty(teamDto.getId())) {
        // // Create new team
        // teamsService.addTeam(userId, null, teamDto);
        // } else {
        // // Update existing team
        // teamsService.updateTeam(userId, null, teamDto.getId(), teamDto);
        // }
        // } catch (ResourceException e) {
        // log.error("Error processing team {} for tournament {}: {}",
        // teamDto.getTeamName(), matchId, e.getMessage());
        // throw new ResourceException(ErrorCodes.INVALID_REQUEST,
        // "Failed to process team: " + teamDto.getTeamName());
        // }
        // }
        // }
        // }

        BadmintonTournament updatedBadmintonTournament = badmintonTournamentRepo.save(badmintonTournament);
        return convertToDto(userId, updatedBadmintonTournament, sport);
    }

    @Override
    public BadmintonTournamentDto convertToDto(String userId, BadmintonTournament badmintonTournament, Sports sport)
            throws ResourceException {
        boolean isCreatedUser = StringUtils.isNotEmpty(userId) && badmintonTournament.getCreatedByUserProfile() != null
                && StringUtils.isNotEmpty(badmintonTournament.getCreatedByUserProfile().getId())
                && badmintonTournament.getCreatedByUserProfile().getId().equalsIgnoreCase(userId);
        boolean isScorerUser = StringUtils.isNotEmpty(userId) && badmintonTournament.getScorerUserProfile() != null
                && StringUtils.isNotEmpty(badmintonTournament.getScorerUserProfile().getId())
                && badmintonTournament.getScorerUserProfile().getId().equalsIgnoreCase(userId);
        boolean isRefereeUser = StringUtils.isNotEmpty(userId) && badmintonTournament.getRefereeUserProfile() != null
                && StringUtils.isNotEmpty(badmintonTournament.getRefereeUserProfile().getId())
                && badmintonTournament.getRefereeUserProfile().getId().equalsIgnoreCase(userId);
        boolean isMatchOfficial = StringUtils.isNotEmpty(userId)
                && badmintonTournament.getMatchOfficialUserProfile() != null
                && StringUtils.isNotEmpty(badmintonTournament.getMatchOfficialUserProfile().getId())
                && badmintonTournament.getMatchOfficialUserProfile().getId().equalsIgnoreCase(userId);

        BadmintonTournamentDto badmintonTournamentDto = new BadmintonTournamentDto();
        badmintonTournamentDto.setCreatedByUserId(badmintonTournament.getCreatedByUserProfile() != null
                ? badmintonTournament.getCreatedByUserProfile().getId()
                : null);
        badmintonTournamentDto.setCreatedByUserProfile(badmintonTournament.getCreatedByUserProfile() != null
                ? modelMapper.map(badmintonTournament.getCreatedByUserProfile(), UserProfileMinDto.class)
                : null);
        badmintonTournamentDto.setId(badmintonTournament.getId());
        badmintonTournamentDto.setName(badmintonTournament.getName());
        badmintonTournamentDto.setDescription(badmintonTournament.getDescription());
        badmintonTournamentDto.setFormat(badmintonTournament.getFormat());
        badmintonTournamentDto.setType(badmintonTournament.getType());
        badmintonTournamentDto.setStartDate(badmintonTournament.getStartDate());
        badmintonTournamentDto.setEndDate(badmintonTournament.getEndDate());
        badmintonTournamentDto.setCanManage(isCreatedUser || isScorerUser || isRefereeUser || isMatchOfficial);
        if (badmintonTournament.getAcademy() != null) {
            badmintonTournamentDto.setAcademy(modelMapper.map(badmintonTournament.getAcademy(), AcademyMinDto.class));
        }

        if (!CollectionUtils.isEmpty(badmintonTournament.getTeams())) {
            badmintonTournamentDto.setTeams(teamsService.convertToDto(badmintonTournament.getTeams()));
        }

        if (!CollectionUtils.isEmpty(badmintonTournament.getMatches())) {
            badmintonTournamentDto
                    .setMatches(badmintonMatchService.convertToDto(userId, badmintonTournament.getMatches(), sport));
        }

        if (badmintonTournament.getScorerUserProfile() != null) {
            badmintonTournamentDto
                    .setScorer(modelMapper.map(badmintonTournament.getScorerUserProfile(), UserProfileMinDto.class));
        }

        if (badmintonTournament.getMatchOfficialUserProfile() != null) {
            badmintonTournamentDto.setMatchOfficial(
                    modelMapper.map(badmintonTournament.getMatchOfficialUserProfile(), UserProfileMinDto.class));
        }

        if (badmintonTournament.getRefereeUserProfile() != null) {
            badmintonTournamentDto
                    .setReferee(modelMapper.map(badmintonTournament.getRefereeUserProfile(), UserProfileMinDto.class));
        }

        badmintonTournamentDto.setCreatedOn(badmintonTournament.getCreatedOn());

        if (!CollectionUtils.isEmpty(badmintonTournament.getGalleryMedia())) {
            List<MediaDto> mediaDtos = badmintonTournament.getGalleryMedia().stream()
                    .map(badmintonTournamentGalleryMedia -> {
                        MediaDto mediaDto = new MediaDto();
                        mediaDto.setId(badmintonTournamentGalleryMedia.getId());
                        mediaDto.setUrls(tournamentsMediaBaseUrl + badmintonTournamentGalleryMedia.getMediaPath());
                        mediaDto.setMediaType(badmintonTournamentGalleryMedia.getMediaType());
                        return mediaDto;
                    }).toList();
            badmintonTournamentDto.setGalleryMedia(mediaDtos);
        }

        return badmintonTournamentDto;
    }

    @Override
    public TournamentPerformanceDto getTournamentPerformance(String tournamentId, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            return pickleballTournamentService.getTournamentPerformance(tournamentId);
        }

        Optional<BadmintonTournament> badmintonTournament = badmintonTournamentRepo.findById(tournamentId);
        if (badmintonTournament.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found");
        }

        var countPair = badmintonMatchService.getMatchCounts(tournamentId, sport);

        var playerPerformanceDtos = new ArrayList<PlayerPerformanceDto>();
        playerPerformanceDtos.addAll(badmintonMatchService.getPlayerPerformances(tournamentId, sport));
        playerPerformanceDtos.addAll(badmintonMatchService.getGuestPlayerPerformances(tournamentId, sport));

        var dto = new TournamentPerformanceDto();
        dto.setTournamentId(tournamentId);
        dto.setTournamentName(badmintonTournament.get().getName());
        dto.setTotalMatchesPlayed(countPair.getLeft());
        dto.setTotalMatchesCompleted(countPair.getRight());
        dto.setTeamPerformances(teamsService.getTeamPerformance(tournamentId));
        dto.setPlayerPerformances(playerPerformanceDtos.stream().parallel()
                .sorted(Comparator.comparing(PlayerPerformanceDto::getWinPercentage).reversed())
                .collect(Collectors.toList()));
        return dto;
    }

    @Override
    public List<MediaDto> uploadGalleryMedia(String tournamentId, List<FileObjectDto> fileObjectDtos, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            return pickleballTournamentService.uploadGalleryMedia(tournamentId, fileObjectDtos);
        }
        Optional<BadmintonTournament> badmintonTournament = badmintonTournamentRepo.findById(tournamentId);
        if (badmintonTournament.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found");
        }

        List<MediaDto> mediaDtos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fileObjectDtos)) {
            List<BadmintonTournamentGalleryMedia> badmintonTournamentGalleryMedias = new ArrayList<>();
            for (FileObjectDto fileObjectDto : fileObjectDtos) {
                BadmintonTournamentGalleryMedia badmintonTournamentGalleryMedia = new BadmintonTournamentGalleryMedia();
                badmintonTournamentGalleryMedia.setBadmintonTournament(badmintonTournament.get());
                badmintonTournamentGalleryMedia.setCreatedOn(Timestamp.from(Instant.now()));
                String prefix = "gallery-media/" + tournamentId + "/" + UUID.randomUUID() + "_"
                        + fileObjectDto.getOriginalFilename();
                storageService.upload(tournamentsMediaBucket, prefix, fileObjectDto.getContent(),
                        fileObjectDto.getContentType());
                badmintonTournamentGalleryMedia.setMediaPath(prefix);
                badmintonTournamentGalleryMedia.setMediaType(MediaType.IMAGE);
                badmintonTournamentGalleryMedias.add(badmintonTournamentGalleryMedia);
            }
            Iterable<BadmintonTournamentGalleryMedia> savedRecs = badmintonTournamentGalleryMediaRepo
                    .saveAll(badmintonTournamentGalleryMedias);
            for (BadmintonTournamentGalleryMedia savedRec : savedRecs) {
                MediaDto mediaDto = new MediaDto();
                mediaDto.setId(savedRec.getId());
                mediaDto.setUrls(tournamentsMediaBaseUrl + savedRec.getMediaPath());
                mediaDto.setMediaType(savedRec.getMediaType());
                mediaDtos.add(mediaDto);
            }
        } else {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "FileObjectDto is null");
        }
        return mediaDtos;
    }

    @Transactional
    @Override
    public void deleteGalleryMedia(String tournamentId, Long mediaId, Sports sport) throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballTournamentService.deleteGalleryMedia(tournamentId, mediaId);
        } else {
            badmintonTournamentRepo.findById(tournamentId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

            Optional<BadmintonTournamentGalleryMedia> badmintonTournamentGalleryMedia = badmintonTournamentGalleryMediaRepo
                    .findById(mediaId);
            if (badmintonTournamentGalleryMedia.isEmpty()) {
                throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Media not found");
            }

            log.info(mediaId.toString());
            badmintonTournamentGalleryMediaRepo.deleteByTournamentIdAndId(tournamentId, mediaId);
            storageService.delete(tournamentsMediaBucket, badmintonTournamentGalleryMedia.get().getMediaPath());
        }
    }

    @Transactional
    @Override
    public void deleteAllAutogeneratedMatches(String academyId, String tournamentId, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballTournamentService.deleteAllAutogeneratedMatches(academyId, tournamentId, sport);
        } else {
            if (StringUtils.isNotEmpty(academyId)) {
                academyService.getAcademyById(academyId);
            }
            Optional<BadmintonTournament> badmintonTournament = badmintonTournamentRepo.findById(tournamentId);
            if (badmintonTournament.isEmpty()) {
                throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found");
            }

            badmintonMatchService.deleteAllAutogeneratedMatchesByTournament(academyId, tournamentId, sport);
        }
    }

    @Transactional
    @Override
    public List<UserProfileMinDto> addPlayersToTournament(String tournamentId, List<String> playerIds, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            return pickleballTournamentService.addPlayersToTournament(tournamentId, playerIds);
        }
        Optional<BadmintonTournament> badmintonTournament = badmintonTournamentRepo.findById(tournamentId);
        if (badmintonTournament.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found");
        }

        List<BadmintonTournamentPlayer> badmintonTournamentPlayers = new ArrayList<>();
        for (String playerId : playerIds) {
            BadmintonTournamentPlayer badmintonTournamentPlayer = new BadmintonTournamentPlayer();
            badmintonTournamentPlayer.setBadmintonTournament(badmintonTournament.get());
            badmintonTournamentPlayer.setPlayerUserProfile(UserProfile.builder().id(playerId).build());
            badmintonTournamentPlayer.setCreatedOn(Timestamp.from(Instant.now()));
            badmintonTournamentPlayers.add(badmintonTournamentPlayer);
        }

        Iterable<BadmintonTournamentPlayer> savedRecs = badmintonTournamentPlayerRepo
                .saveAll(badmintonTournamentPlayers);
        return getTournanentPlayers(null, tournamentId, sport);
    }

    @Async
    @Override
    public void deletePlayersFromTournament(String tournamentId, String playerId, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballTournamentService.deletePlayersFromTournament(tournamentId, playerId);
        } else {
            badmintonTournamentPlayerRepo.deletePlayerTournamentMapping(playerId, tournamentId);
        }
    }

    @Override
    public List<UserProfileMinDto> getTournanentPlayers(String searchTxt, String tournamentId, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            return pickleballTournamentService.getTournamentPlayers(searchTxt, tournamentId);
        }
        Optional<BadmintonTournament> badmintonTournament = badmintonTournamentRepo.findById(tournamentId);
        if (badmintonTournament.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found");
        }

        List<BadmintonTournamentPlayer> badmintonTournamentPlayers = badmintonTournamentPlayerRepo
                .findByBadmintonTournament_Id(tournamentId);
        if (CollectionUtils.isEmpty(badmintonTournamentPlayers)) {
            return List.of();
        }

        List<UserProfileMinDto> userProfileMinDtos = badmintonTournamentPlayers.stream()
                .map(badmintonTournamentPlayer -> modelMapper.map(badmintonTournamentPlayer.getPlayerUserProfile(),
                        UserProfileMinDto.class))
                .toList();

        if (StringUtils.isNotEmpty(searchTxt) && searchTxt.length() > 2) {
            userProfileMinDtos = userProfileMinDtos.stream()
                    .filter(userProfileMinDto -> userProfileMinDto.getDisplayName().toLowerCase().contains(
                            searchTxt.toLowerCase()) || userProfileMinDto.getPhoneNumber().contains(searchTxt))
                    .toList();
        }

        return userProfileMinDtos;
    }

    @Transactional
    @Override
    public void mapTeamsToTournament(String userId, String tournamentId,
            TournamentTeamMappingDto tournamentTeamMappingDto, String academyId, Sports sport)
            throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballTournamentService.mapTeamsToTournament(userId, tournamentId, tournamentTeamMappingDto, academyId);
        } else {
            if (academyId != null && !StringUtils.isBlank(academyId)) {
                academyService.getAcademyById(academyId);
            }

            BadmintonTournament tournament = badmintonTournamentRepo.findById(tournamentId)
                    .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

            List<Team> teams = teamRepo.findAllById(tournamentTeamMappingDto.getTeamIds());

            if (CollectionUtils.isEmpty(teams)) {
                throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "No valid teams found to map.");
            }

            for (Team team : teams) {
                team.setBadmintonTournament(tournament);
                if (academyId != null && !StringUtils.isBlank(academyId)) {
                    team.setAcademy(Academy.builder().id(academyId).build());
                }
            }
            teamRepo.saveAll(teams);
        }
    }

    private void sendNotificationToScorer(List<UserProfileDto> scorerUserProfiles, String tournamentId,
            String academyId) {
        if (CollectionUtils.isEmpty(scorerUserProfiles)) {
            log.warn("empty list of scorer user profiles");
            return;
        }

        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("tournamentId", tournamentId);
        extraParams.put("isAssociateWithAcademy", String.valueOf(StringUtils.isNotEmpty(academyId)));
        if (academyId != null && StringUtils.isNotEmpty(academyId)) {
            extraParams.put("academyId", academyId);
        }

        List<String> sentUserIds = new ArrayList<>();

        for (UserProfileDto userProfileDto : scorerUserProfiles) {
            if (StringUtils.isEmpty(userProfileDto.getAndroidFcmPushToken())) {
                continue;
            }

            pushNotificationService.sendMessageToPushToken(userProfileDto.getAndroidFcmPushToken(),
                    NotificationType.LIVE_NOTIFICATION, "You are added as a scorer",
                    "You have been assigned as scorer in a tournament.", "TOURNAMENT_DETAILS", CtaType.SCREEN,
                    extraParams);
            sentUserIds.add(userProfileDto.getId());
        }
        pushNotificationService.addNotification(sentUserIds, "You are added as a scorer", CtaType.SCREEN,
                "TOURAMENT_DETAILS", extraParams);

    }

}
