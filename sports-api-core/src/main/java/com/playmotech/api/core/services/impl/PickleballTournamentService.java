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
import com.playmotech.api.core.dao_postgres.PickleballTeam;
import com.playmotech.api.core.dao_postgres.PickleballTournament;
import com.playmotech.api.core.dao_postgres.PickleballTournamentGalleryMedia;
import com.playmotech.api.core.dao_postgres.PickleballTournamentPlayer;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AcademyMinDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.MediaDto;
import com.playmotech.api.core.dto.PaginatedResponse;
import com.playmotech.api.core.dto.PickleballTournamentDto;
import com.playmotech.api.core.dto.PlayerPerformanceDto;
import com.playmotech.api.core.dto.TournamentPerformanceDto;
import com.playmotech.api.core.dto.TournamentTeamMappingDto;
import com.playmotech.api.core.dto.UpdatePickleballTournamentDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.PickleTournamentPlayerRepo;
import com.playmotech.api.core.repo.PickleballTeamRepo;
import com.playmotech.api.core.repo.PickleballTournamentGalleryMediaRepo;
import com.playmotech.api.core.repo.PickleballTournamentRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IPickleballMatchService;
import com.playmotech.api.core.services.IPickleballTeamService;
import com.playmotech.api.core.services.IPickleballTournamentService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.specification.PickleballTournamentSpecification;
import com.playmotech.api.core.utils.DateTimeUtils;
import com.playmotech.api.core.utils.GenericFilter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PickleballTournamentService implements IPickleballTournamentService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final IAcademyService academyService;
    private final IPickleballTeamService pickleballTeamService;
    private final PickleballTournamentRepo pickleballTournamentRepo;
    private final IPickleballMatchService pickleballMatchService;
    private final IStorageService storageService;
    private final PickleballTournamentGalleryMediaRepo badmintonTournamentGalleryMediaRepo;
    private final PickleTournamentPlayerRepo pickleballTournamentPlayerRepo;
    private final PickleballTeamRepo pickleballTeamRepo;
    private final UserProfileRepo userProfileRepo;
    private final IPushNotificationService pushNotificationService;
    private final PickleballTournamentGalleryMediaRepo pickleballTournamentGalleryMediaRepo;

    @Value("${tournaments-media-base-url}")
    private String tournamentsMediaBaseUrl;

    @Value("${storage.tournaments-media-bucket}")
    private String tournamentsMediaBucket;

    public PickleballTournamentService(IAcademyService academyService,
            IPickleballTeamService pickleballTeamService,
            PickleballTournamentRepo pickleballTournamentRepo,
            IPickleballMatchService pickleballMatchService,
            IStorageService storageService,
            PickleballTournamentGalleryMediaRepo badmintonTournamentGalleryMediaRepo,
            PickleTournamentPlayerRepo pickleballTournamentPlayerRepo,
            PickleballTeamRepo pickleballTeamRepo,
            UserProfileRepo userProfileRepo,
            IPushNotificationService pushNotificationService,
            PickleballTournamentGalleryMediaRepo pickleballTournamentGalleryMediaRepo) {
        this.academyService = academyService;
        this.pickleballTeamService = pickleballTeamService;
        this.pickleballTournamentRepo = pickleballTournamentRepo;
        this.pickleballMatchService = pickleballMatchService;
        this.storageService = storageService;
        this.badmintonTournamentGalleryMediaRepo = badmintonTournamentGalleryMediaRepo;
        this.pickleballTournamentPlayerRepo = pickleballTournamentPlayerRepo;
        this.pickleballTeamRepo = pickleballTeamRepo;
        this.userProfileRepo = userProfileRepo;
        this.pushNotificationService = pushNotificationService;
        this.pickleballTournamentGalleryMediaRepo = pickleballTournamentGalleryMediaRepo;
    }

    @Override
    @Transactional
    public PickleballTournamentDto createTournament(String userId, String academyId, PickleballTournamentDto request,
            Sports sport)
            throws ResourceException {
        academyService.getAcademyById(academyId);

        PickleballTournament pickleballTournament = new PickleballTournament();
        String tournamentId = UUID.randomUUID().toString();

        pickleballTournament.setId(tournamentId);
        pickleballTournament.setName(request.getName());
        pickleballTournament.setDescription(request.getDescription());
        pickleballTournament.setFormat(request.getFormat());
        pickleballTournament.setType(request.getType());
        pickleballTournament.setStartDate(request.getStartDate());
        pickleballTournament.setEndDate(request.getEndDate());
        pickleballTournament.setAcademy(Academy.builder().id(academyId).build());
        pickleballTournament.setCreatedOn(Timestamp.from(Instant.now()));
        pickleballTournament.setCreatedByUserProfile(UserProfile.builder().id(userId).build());

        pickleballTournament.setScorerUserProfile(StringUtils.isEmpty(request.getScorerUserId()) ? null
                : UserProfile.builder().id(request.getScorerUserId()).build());

        pickleballTournament.setRefereeUserProfile(StringUtils.isEmpty(request.getRefereeUserId()) ? null
                : UserProfile.builder().id(request.getRefereeUserId()).build());

        pickleballTournament.setMatchOfficialUserProfile(StringUtils.isEmpty(request.getMatchOfficialUserId()) ? null
                : UserProfile.builder().id(request.getMatchOfficialUserId()).build());

        PickleballTournament savedTournament = pickleballTournamentRepo.save(pickleballTournament);

        if (StringUtils.isEmpty(request.getScorerUserId())) {
            CompletableFuture.runAsync(() -> {
                log.info("Sending notification to scorer");
                UserProfileDto userProfileDto = userProfileRepo.findById(request.getScorerUserId())
                        .map(userProfile -> modelMapper.map(userProfile, UserProfileDto.class)).orElse(null);
                sendNotificationToScorer(Arrays.asList(userProfileDto), tournamentId, academyId);
                log.info("Notification sent to scorer");
            });
        }

        return convertToDto(userId, savedTournament, sport);
    }

    @Override
    public PickleballTournamentDto createTournament(String userId, PickleballTournamentDto request, Sports sport)
            throws ResourceException {
        PickleballTournament pickleballTournament = new PickleballTournament();
        String tournamentId = UUID.randomUUID().toString();

        pickleballTournament.setId(tournamentId);
        pickleballTournament.setName(request.getName());
        pickleballTournament.setDescription(request.getDescription());
        pickleballTournament.setFormat(request.getFormat());
        pickleballTournament.setType(request.getType());
        pickleballTournament.setStartDate(request.getStartDate());
        pickleballTournament.setEndDate(request.getEndDate());
        pickleballTournament.setCreatedOn(Timestamp.from(Instant.now()));
        pickleballTournament.setCreatedByUserProfile(UserProfile.builder().id(userId).build());

        pickleballTournament.setScorerUserProfile(
                StringUtils.isEmpty(request.getScorerUserId()) ? null
                        : UserProfile.builder().id(request.getScorerUserId()).build());

        pickleballTournament.setRefereeUserProfile(StringUtils.isEmpty(request.getRefereeUserId()) ? null
                : UserProfile.builder().id(request.getRefereeUserId()).build());

        pickleballTournament.setMatchOfficialUserProfile(StringUtils.isEmpty(request.getMatchOfficialUserId()) ? null
                : UserProfile.builder().id(request.getMatchOfficialUserId()).build());

        PickleballTournament savedPickleballTournament = pickleballTournamentRepo.save(pickleballTournament);

        if (StringUtils.isNotEmpty(request.getScorerUserId())) {
            CompletableFuture.runAsync(() -> {
                log.info("Sending notification to scorer");
                UserProfileDto userProfileDto = userProfileRepo.findById(request.getScorerUserId())
                        .map(userProfile -> modelMapper.map(userProfile, UserProfileDto.class)).orElse(null);
                sendNotificationToScorer(Arrays.asList(userProfileDto), tournamentId, null);
                log.info("Notification sent to scorer");
            });
        }

        return convertToDto(userId, savedPickleballTournament, sport);
    }

    @Override
    public PaginatedResponse<PickleballTournamentDto> getTournamentsPaginated(String userId, GenericFilter filter)
            throws ResourceException {

        if (StringUtils.isNotEmpty(filter.getAcademyId())) {
            academyService.getAcademyById(filter.getAcademyId());
        }

        PickleballTournamentSpecification spec = new PickleballTournamentSpecification(filter,
                filter.getTournamentStatus());

        Pageable pageable = PageRequest.of(filter.getCurrentPage(), filter.getPageSize());

        Page<PickleballTournament> tournaments = pickleballTournamentRepo.findAll(spec, pageable);

        long totalElements = tournaments.getTotalElements();
        int totalPages = tournaments.getTotalPages();

        if (tournaments.isEmpty()) {
            return new PaginatedResponse<PickleballTournamentDto>(List.of(), totalElements, totalPages,
                    filter.getCurrentPage());
        }

        List<PickleballTournamentDto> list = tournaments.getContent()
                .stream()
                .map(t -> {
                    try {
                        return convertToDto(userId, t, Sports.PICKLEBALL);
                    } catch (ResourceException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return new PaginatedResponse<PickleballTournamentDto>(list, totalElements, totalPages, filter.getCurrentPage());
    }

    @Override
    public List<PickleballTournamentDto> getAllTournaments(String userId, String academyId, TournamentStatus status,
            String searchText, Sports sport) throws ResourceException {

        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }

        List<PickleballTournament> badmintonTournaments = new ArrayList<>();

        if (status == TournamentStatus.LIVE) {
            if (StringUtils.isNotEmpty(academyId)) {
                badmintonTournaments = pickleballTournamentRepo
                        .findByAcademy_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(academyId,
                                DateTimeUtils.getTodayDate(), DateTimeUtils.getTodayDate())
                        .stream().filter(t -> !t.isInactive()).toList();
            } else {
                badmintonTournaments = pickleballTournamentRepo
                        .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(DateTimeUtils.getTodayDate(),
                                DateTimeUtils.getTodayDate())
                        .stream().filter(t -> !t.isInactive()).toList();
            }
        } else if (status == TournamentStatus.UPCOMING) {
            if (StringUtils.isNotEmpty(academyId)) {
                badmintonTournaments = pickleballTournamentRepo
                        .findByAcademy_IdAndStartDateGreaterThan(academyId, DateTimeUtils.getTodayDate()).stream()
                        .filter(t -> !t.isInactive()).toList();
            } else {
                badmintonTournaments = pickleballTournamentRepo.findByStartDateGreaterThan(DateTimeUtils.getTodayDate())
                        .stream().filter(t -> !t.isInactive()).toList();
            }
        } else if (status == TournamentStatus.COMPLETED) {
            if (StringUtils.isNotEmpty(academyId)) {
                badmintonTournaments = pickleballTournamentRepo
                        .findByAcademy_IdAndEndDateLessThan(academyId, DateTimeUtils.getTodayDate()).stream()
                        .filter(t -> !t.isInactive()).toList();
            } else {
                badmintonTournaments = pickleballTournamentRepo.findByEndDateLessThan(DateTimeUtils.getTodayDate())
                        .stream().filter(t -> !t.isInactive()).toList();
            }
        } else {
            if (StringUtils.isNotEmpty(academyId)) {
                badmintonTournaments = pickleballTournamentRepo.findByAcademy_Id(academyId).stream()
                        .filter(t -> !t.isInactive())
                        .toList();
            } else {
                badmintonTournaments = StreamSupport.stream(pickleballTournamentRepo.findAll().spliterator(), false)
                        .filter(t -> !t.isInactive()).toList();
            }
        }

        if (!CollectionUtils.isEmpty(badmintonTournaments) && StringUtils.isNotEmpty(searchText)
                && searchText.length() > 2) {
            badmintonTournaments = badmintonTournaments.stream().filter(
                    badmintonTournament -> StringUtils.containsIgnoreCase(badmintonTournament.getName(), searchText))
                    .toList();
        }

        if (CollectionUtils.isEmpty(badmintonTournaments)) {
            return List.of();
        }

        return badmintonTournaments.stream()
                .sorted(Comparator.comparing(PickleballTournament::getCreatedOn).reversed())
                .map(t -> {
                    try {
                        return convertToDto(userId, t, sport);
                    } catch (ResourceException e) {
                        log.error("Error while converting tournament to dto: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public PickleballTournamentDto getTournament(String userId, String academyId, String tournamentId, Sports sport)
            throws ResourceException {
        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }

        Optional<PickleballTournament> pickleballTournament;

        if (StringUtils.isNotEmpty(academyId)) {
            pickleballTournament = pickleballTournamentRepo.findByAcademyIsNotNullAndId(tournamentId);
        } else {
            pickleballTournament = pickleballTournamentRepo.findByAcademyIsNullAndId(tournamentId);
        }

        return convertToDto(userId, pickleballTournament
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found")),
                sport);
    }

    @Async
    @Override
    public void deleteTournament(String academyId, String tournamentId, Sports sport) throws ResourceException {
        academyService.getAcademyById(academyId);

        PickleballTournament pickleballTournament = pickleballTournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));
        pickleballTournament.setInactive(true);
        pickleballTournamentRepo.save(pickleballTournament);
    }

    @Override
    public void deleteTournament(String tournamentId, Sports sport) throws ResourceException {
        PickleballTournament pickleballTournament = pickleballTournamentRepo.findByAcademyIsNullAndId(tournamentId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));
        pickleballTournament.setInactive(true);
        pickleballTournamentRepo.save(pickleballTournament);
    }

    @Override
    @Transactional
    public PickleballTournamentDto updateTournament(String userId, String academyId, String matchId,
            UpdatePickleballTournamentDto updateRequest, Sports sport) throws ResourceException {

        academyService.getAcademyById(academyId);

        PickleballTournament pickleballTournament = pickleballTournamentRepo.findByAcademyIsNullAndId(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        // Handle basic tournament updates
        if (!StringUtils.isEmpty(updateRequest.getName())) {
            pickleballTournament.setName(updateRequest.getName());
        }
        if (!StringUtils.isEmpty(updateRequest.getDescription())) {
            pickleballTournament.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getFormat() != null) {
            pickleballTournament.setFormat(updateRequest.getFormat());
        }
        if (updateRequest.getType() != null) {
            pickleballTournament.setType(updateRequest.getType());
        }
        if (updateRequest.getStartDate() != null) {
            pickleballTournament.setStartDate(updateRequest.getStartDate());
        }
        if (updateRequest.getEndDate() != null) {
            pickleballTournament.setEndDate(updateRequest.getEndDate());
        }

        PickleballTournament updatedTournament = pickleballTournamentRepo.save(pickleballTournament);
        return convertToDto(userId, updatedTournament, sport);
    }

    @Override
    public PickleballTournamentDto updateTournament(String userId, String matchId,
            UpdatePickleballTournamentDto updateRequest, Sports sport) throws ResourceException {
        PickleballTournament pickleballTournament = pickleballTournamentRepo.findByAcademyIsNullAndId(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        // Handle basic tournament updates
        if (!StringUtils.isEmpty(updateRequest.getName())) {
            pickleballTournament.setName(updateRequest.getName());
        }
        if (!StringUtils.isEmpty(updateRequest.getDescription())) {
            pickleballTournament.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getFormat() != null) {
            pickleballTournament.setFormat(updateRequest.getFormat());
        }
        if (updateRequest.getType() != null) {
            pickleballTournament.setType(updateRequest.getType());
        }
        if (updateRequest.getStartDate() != null) {
            pickleballTournament.setStartDate(updateRequest.getStartDate());
        }
        if (updateRequest.getEndDate() != null) {
            pickleballTournament.setEndDate(updateRequest.getEndDate());
        }

        PickleballTournament updatedTournament = pickleballTournamentRepo.save(pickleballTournament);

        return convertToDto(userId, updatedTournament, sport);
    }

    @Override
    public List<MediaDto> uploadGalleryMedia(String tournamentId, List<FileObjectDto> fileObjectDtos)
            throws ResourceException {
        PickleballTournament pickleballTournament = pickleballTournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        List<MediaDto> mediaDtos = new ArrayList<>();

        if (!CollectionUtils.isEmpty(fileObjectDtos)) {
            List<PickleballTournamentGalleryMedia> tournamentGalleryMedias = new ArrayList<>();
            for (FileObjectDto fileObjectDto : fileObjectDtos) {
                PickleballTournamentGalleryMedia media = new PickleballTournamentGalleryMedia();
                media.setPickleballTournament(pickleballTournament);
                media.setCreatedOn(Timestamp.from(Instant.now()));
                String prefix = "gallery-media/" + tournamentId + "/" + UUID.randomUUID() + "_"
                        + fileObjectDto.getOriginalFilename();
                storageService.upload(tournamentsMediaBucket, prefix, fileObjectDto.getContent(),
                        fileObjectDto.getContentType());

                media.setMediaPath(prefix);
                media.setMediaType(MediaType.IMAGE);
                tournamentGalleryMedias.add(media);
            }
            List<PickleballTournamentGalleryMedia> savedRecords = pickleballTournamentGalleryMediaRepo
                    .saveAll(tournamentGalleryMedias);

            for (PickleballTournamentGalleryMedia savedRecord : savedRecords) {
                MediaDto mediaDto = new MediaDto();
                mediaDto.setId(savedRecord.getId());
                mediaDto.setUrls(tournamentsMediaBaseUrl + savedRecord.getMediaPath());
                mediaDto.setMediaType(savedRecord.getMediaType());
                mediaDtos.add(mediaDto);
            }
        } else {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "FileObjectDto is null");
        }
        return mediaDtos;
    }

    // @Transactional
    @Override
    public void deleteGalleryMedia(String tournamentId, Long mediaId) throws ResourceException {
        pickleballTournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        Optional<PickleballTournamentGalleryMedia> tournamentGalleryMedia = badmintonTournamentGalleryMediaRepo
                .findById(mediaId);

        if (tournamentGalleryMedia.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Media not found");
        }

        pickleballTournamentGalleryMediaRepo.deleteByTournamentIdAndId(tournamentId, mediaId);
        storageService.delete(tournamentsMediaBucket, tournamentGalleryMedia.get().getMediaPath());
    }

    @Override
    public TournamentPerformanceDto getTournamentPerformance(String tournamentId) throws ResourceException {
        PickleballTournament tournament = pickleballTournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        var countPair = pickleballMatchService.getMatchCounts(tournamentId);

        var playerPerformanceDtos = new ArrayList<PlayerPerformanceDto>();

        playerPerformanceDtos.addAll(pickleballMatchService.getPlayerPerformances(tournamentId));
        playerPerformanceDtos.addAll(pickleballMatchService.getGuestPlayerPerformances(tournamentId));

        var dto = new TournamentPerformanceDto();
        dto.setTournamentId(tournamentId);
        dto.setTournamentName(tournament.getName());
        dto.setTotalMatchesPlayed(countPair.getLeft());
        dto.setTotalMatchesCompleted(countPair.getRight());
        dto.setTeamPerformances(pickleballTeamService.getTeamPerformance(tournamentId));
        dto.setPlayerPerformances(playerPerformanceDtos.stream().parallel()
                .sorted(Comparator.comparing(PlayerPerformanceDto::getWinPercentage).reversed())
                .collect(Collectors.toList()));
        return dto;
    }

    @Override
    public void deleteAllAutogeneratedMatches(String academyId, String tournamentId, Sports sport)
            throws ResourceException {
        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }
        pickleballTournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        pickleballMatchService.deleteAllAutogeneratedMatchesByTournament(academyId, tournamentId, sport);
    }

    @Transactional
    @Override
    public List<UserProfileMinDto> addPlayersToTournament(String tournamentId, List<String> playerIds)
            throws ResourceException {
        PickleballTournament tournament = pickleballTournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        List<PickleballTournamentPlayer> pickleballTournamentPlayers = new ArrayList<>();

        for (String playerId : playerIds) {
            PickleballTournamentPlayer pickleballTournamentPlayer = new PickleballTournamentPlayer();

            pickleballTournamentPlayer.setPickleballTournament(tournament);
            pickleballTournamentPlayer.setPlayerUserProfile(UserProfile.builder().id(playerId).build());
            pickleballTournamentPlayer.setCreatedOn(Timestamp.from(Instant.now()));

            pickleballTournamentPlayers.add(pickleballTournamentPlayer);
        }

        List<PickleballTournamentPlayer> savedRecs = pickleballTournamentPlayerRepo
                .saveAll(pickleballTournamentPlayers);

        return getTournamentPlayers(null, tournamentId);
    }

    @Async
    @Override
    public void deletePlayersFromTournament(String tournamentId, String playerUserId) throws ResourceException {
        pickleballTournamentPlayerRepo.deletePlayerTournamentMapping(playerUserId, tournamentId);
    }

    @Override
    public List<UserProfileMinDto> getTournamentPlayers(String searchTxt, String tournamentId)
            throws ResourceException {
        pickleballTournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        List<PickleballTournamentPlayer> pickleballTournamentPlayers = pickleballTournamentPlayerRepo
                .findByPickleballTournament_Id(tournamentId);

        if (CollectionUtils.isEmpty(pickleballTournamentPlayers)) {
            return List.of();
        }

        List<UserProfileMinDto> userProfileMinDtos = pickleballTournamentPlayers.stream()
                .map(t -> modelMapper.map(t.getPlayerUserProfile(), UserProfileMinDto.class))
                .toList();

        if (StringUtils.isNotEmpty(searchTxt) && searchTxt.length() > 2) {
            userProfileMinDtos = userProfileMinDtos.stream()
                    .filter(userProfileMinDto -> userProfileMinDto.getDisplayName().toLowerCase().contains(
                            searchTxt.toLowerCase()) || userProfileMinDto.getPhoneNumber().contains(searchTxt))
                    .toList();
        }

        return userProfileMinDtos;
    }

    @Override
    public void mapTeamsToTournament(String userId, String tournamentId,
            TournamentTeamMappingDto tournamentTeamMappingDto, String academyId) throws ResourceException {
        if (academyId != null && !StringUtils.isBlank(academyId)) {
            academyService.getAcademyById(academyId);
        }

        PickleballTournament tournament = pickleballTournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Tournament not found"));

        List<PickleballTeam> teams = pickleballTeamRepo.findAllById(tournamentTeamMappingDto.getTeamIds());

        if (CollectionUtils.isEmpty(teams)) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "No valid teams found to map.");
        }

        for (PickleballTeam team : teams) {
            team.setPickleballTournament(tournament);
            if (academyId != null && !StringUtils.isBlank(academyId)) {
                team.setAcademy(Academy.builder().id(academyId).build());
            }
        }
        pickleballTeamRepo.saveAll(teams);
    }

    @Override
    public PickleballTournamentDto convertToDto(String userId, PickleballTournament tournament, Sports sport)
            throws ResourceException {
        boolean isCreatedUser = StringUtils.isNotEmpty(userId) && tournament.getCreatedByUserProfile() != null
                && StringUtils.isNotEmpty(tournament.getCreatedByUserProfile().getId())
                && tournament.getCreatedByUserProfile().getId().equalsIgnoreCase(userId);
        boolean isScorerUser = StringUtils.isNotEmpty(userId) && tournament.getScorerUserProfile() != null
                && StringUtils.isNotEmpty(tournament.getScorerUserProfile().getId())
                && tournament.getScorerUserProfile().getId().equalsIgnoreCase(userId);
        boolean isRefereeUser = StringUtils.isNotEmpty(userId) && tournament.getRefereeUserProfile() != null
                && StringUtils.isNotEmpty(tournament.getRefereeUserProfile().getId())
                && tournament.getRefereeUserProfile().getId().equalsIgnoreCase(userId);
        boolean isMatchOfficial = StringUtils.isNotEmpty(userId)
                && tournament.getMatchOfficialUserProfile() != null
                && StringUtils.isNotEmpty(tournament.getMatchOfficialUserProfile().getId())
                && tournament.getMatchOfficialUserProfile().getId().equalsIgnoreCase(userId);

        PickleballTournamentDto pickleballTournamentDto = new PickleballTournamentDto();

        pickleballTournamentDto.setCreatedByUserId(tournament.getCreatedByUserProfile() != null
                ? tournament.getCreatedByUserProfile().getId()
                : null);
        pickleballTournamentDto.setCreatedByUserProfile(tournament.getCreatedByUserProfile() != null
                ? modelMapper.map(tournament.getCreatedByUserProfile(), UserProfileMinDto.class)
                : null);

        pickleballTournamentDto.setId(tournament.getId());
        pickleballTournamentDto.setName(tournament.getName());
        pickleballTournamentDto.setDescription(tournament.getDescription());
        pickleballTournamentDto.setFormat(tournament.getFormat());
        pickleballTournamentDto.setType(tournament.getType());
        pickleballTournamentDto.setStartDate(tournament.getStartDate());
        pickleballTournamentDto.setEndDate(tournament.getEndDate());
        pickleballTournamentDto.setCanManage(isCreatedUser || isScorerUser || isRefereeUser || isMatchOfficial);

        if (tournament.getAcademy() != null) {
            pickleballTournamentDto.setAcademy(modelMapper.map(tournament.getAcademy(), AcademyMinDto.class));
        }

        if (!CollectionUtils.isEmpty(tournament.getMatches())) {
            pickleballTournamentDto
                    .setMatches(pickleballMatchService.convertToDto(userId, tournament.getMatches(), sport));
        }

        if (tournament.getScorerUserProfile() != null) {
            pickleballTournamentDto
                    .setScorer(modelMapper.map(tournament.getScorerUserProfile(), UserProfileMinDto.class));
        }

        if (tournament.getMatchOfficialUserProfile() != null) {
            pickleballTournamentDto.setMatchOfficial(
                    modelMapper.map(tournament.getMatchOfficialUserProfile(), UserProfileMinDto.class));
        }

        if (tournament.getRefereeUserProfile() != null) {
            pickleballTournamentDto
                    .setReferee(modelMapper.map(tournament.getRefereeUserProfile(), UserProfileMinDto.class));
        }

        if (!CollectionUtils.isEmpty(tournament.getGalleryMedia())) {
            List<MediaDto> mediaDtos = tournament.getGalleryMedia().stream()
                    .map(badmintonTournamentGalleryMedia -> {
                        MediaDto mediaDto = new MediaDto();
                        mediaDto.setId(badmintonTournamentGalleryMedia.getId());
                        mediaDto.setUrls(tournamentsMediaBaseUrl + badmintonTournamentGalleryMedia.getMediaPath());
                        mediaDto.setMediaType(badmintonTournamentGalleryMedia.getMediaType());
                        return mediaDto;
                    }).toList();
            pickleballTournamentDto.setGalleryMedia(mediaDtos);
        }

        return pickleballTournamentDto;
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
