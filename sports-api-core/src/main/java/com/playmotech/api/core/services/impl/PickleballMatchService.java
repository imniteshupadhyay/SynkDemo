package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import com.playmotech.api.core.agora.RtcTokenProvider;
import com.playmotech.api.core.constants.AppConstants;
import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.GameFormat;
import com.playmotech.api.core.constants.MatchStatus;
import com.playmotech.api.core.constants.MatchWSMessageType;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.StreamingStatus;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.BadmintonLiveScore;
import com.playmotech.api.core.dao_postgres.Court;
import com.playmotech.api.core.dao_postgres.PickleballMatch;
import com.playmotech.api.core.dao_postgres.PickleballMatchPlayDetail;
import com.playmotech.api.core.dao_postgres.PickleballMatchRound;
import com.playmotech.api.core.dao_postgres.PickleballMatchRoundsPlayDetail;
import com.playmotech.api.core.dao_postgres.PickleballMatchTeamPlayerMapping;
import com.playmotech.api.core.dao_postgres.PickleballSinglesPlayerMapping;
import com.playmotech.api.core.dao_postgres.PickleballTeam;
import com.playmotech.api.core.dao_postgres.PickleballTeamMapping;
import com.playmotech.api.core.dao_postgres.PickleballTournament;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.BadmintonScoreNotificationDto;
import com.playmotech.api.core.dto.BadmintonScoreTrendDto;
import com.playmotech.api.core.dto.CourtDto;
import com.playmotech.api.core.dto.CreatePickleballMatchDto;
import com.playmotech.api.core.dto.PaginatedResponse;
import com.playmotech.api.core.dto.PickleballMatchDetailDto;
import com.playmotech.api.core.dto.PickleballMatchDetailRequestDto;
import com.playmotech.api.core.dto.PickleballMatchDto;
import com.playmotech.api.core.dto.PickleballMatchRoundDetailsDto;
import com.playmotech.api.core.dto.PickleballMatchRoundDetailsRequestDto;
import com.playmotech.api.core.dto.PickleballPlayerScoreDto;
import com.playmotech.api.core.dto.PickleballPlayerScoreRequestDto;
import com.playmotech.api.core.dto.PickleballTeamDto;
import com.playmotech.api.core.dto.PlayerPerformanceDto;
import com.playmotech.api.core.dto.RealTimeScoreUpdateRequestDto;
import com.playmotech.api.core.dto.TeamMatchPlayerDto;
import com.playmotech.api.core.dto.UpdatePickleballMatchDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.BadmintonLiveScoreRepository;
import com.playmotech.api.core.repo.PickleballMatchPlayDetailRepo;
import com.playmotech.api.core.repo.PickleballMatchRepo;
import com.playmotech.api.core.repo.PickleballMatchTeamPlayerMappingRepo;
import com.playmotech.api.core.repo.PickleballSinglesPlayerMappingRepo;
import com.playmotech.api.core.repo.PickleballTeamMappingRepo;
import com.playmotech.api.core.repo.PickleballTeamRepo;
import com.playmotech.api.core.repo.PickleballTournamentRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.ICoachService;
import com.playmotech.api.core.services.IPickleballMatchService;
import com.playmotech.api.core.services.IPickleballTeamService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.specification.PickleballMatchSpecification;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PickleballMatchService implements IPickleballMatchService {
    private final static Integer NO_UID = 0;
    private final ModelMapper modelMapper = new ModelMapper();
    private final PickleballMatchRepo pickleballMatchRepo;
    private final IAcademyService academyService;
    private final PickleballTeamMappingRepo pickleballTeamMappingRepo;
    private final PickleballSinglesPlayerMappingRepo pickleballSinglesPlayerMappingRepo;
    private final RtcTokenProvider rtcTokenProvider;
    private final PickleballMatchPlayDetailRepo pickleballMatchPlayDetailRepo;
    private final ICoachService coachService;
    private final UserProfileRepo userProfileRepo;
    private final IPickleballTeamService pickleballTeamService;
    private final Map<String, List<WebSocketSession>> pickleballMatchSubscribedSessions;
    private final IPushNotificationService pushNotificationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic scoreUpdatesTopic;
    private final BadmintonLiveScoreRepository liveScoreRepo;

    // No live scoring for now
    // private final SimpMessagingTemplate messagingTemplate;
    // private final SubscribeMatchUpdatesWebSocketHandler webSocketHandler;
    // private final CommentaryService commentaryService;
    private final WebSocketBrokerService webSocketBrokerService;

    private final PickleballMatchTeamPlayerMappingRepo pickleballMatchTeamPlayerMappingRepo;
    private final PickleballTournamentRepo pickleballTournamentRepo;
    private final PickleballTeamRepo pickleballTeamRepo;

    public PickleballMatchService(PickleballMatchRepo pickleballMatchRepo,
            IAcademyService academyService,
            PickleballTeamMappingRepo pickleballTeamMappingRepo,
            PickleballSinglesPlayerMappingRepo pickleballSinglesPlayerMappingRepo,
            RtcTokenProvider rtcTokenProvider,
            PickleballMatchPlayDetailRepo pickleballMatchPlayDetailRepo,
            ICoachService coachService,
            UserProfileRepo userProfileRepo, PickleballTeamRepo teamRepo,
            IPickleballTeamService pickleballTeamService,
            Map<String, List<WebSocketSession>> pickleballMatchSubscribedSessions,
            IPushNotificationService pushNotificationService,
            RedisTemplate<String, Object> redisTemplate,
            ChannelTopic scoreUpdatesTopic, BadmintonLiveScoreRepository liveScoreRepo,
            WebSocketBrokerService webSocketBrokerService,
            PickleballMatchTeamPlayerMappingRepo pickleballMatchTeamPlayerMappingRepo,
            PickleballTournamentRepo pickleballTournamentRepo,
            PickleballTeamRepo pickleballTeamRepo) {
        this.pickleballTeamMappingRepo = pickleballTeamMappingRepo;
        this.liveScoreRepo = liveScoreRepo;
        this.webSocketBrokerService = webSocketBrokerService;
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        this.pickleballMatchRepo = pickleballMatchRepo;
        this.academyService = academyService;
        this.pickleballSinglesPlayerMappingRepo = pickleballSinglesPlayerMappingRepo;
        this.rtcTokenProvider = rtcTokenProvider;
        this.pickleballMatchPlayDetailRepo = pickleballMatchPlayDetailRepo;
        this.coachService = coachService;
        this.userProfileRepo = userProfileRepo;
        this.pickleballTeamService = pickleballTeamService;
        this.pickleballMatchSubscribedSessions = pickleballMatchSubscribedSessions;
        this.pushNotificationService = pushNotificationService;
        this.redisTemplate = redisTemplate;
        this.scoreUpdatesTopic = scoreUpdatesTopic;
        this.pickleballMatchTeamPlayerMappingRepo = pickleballMatchTeamPlayerMappingRepo;
        this.pickleballTournamentRepo = pickleballTournamentRepo;
        this.pickleballTeamRepo = pickleballTeamRepo;
    }

    @Override
    public PickleballMatchDto createMatch(String userId, String academyId, CreatePickleballMatchDto request,
            Sports sport) throws ResourceException {
        if (request.getGameFormat() == GameFormat.SINGLES && CollectionUtils.isEmpty(request.getPlayers())) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Player are required for singles match");
        } else if (request.getGameFormat() == GameFormat.DOUBLES && CollectionUtils.isEmpty(request.getTeamIds())) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Teams are required for doubles match");
        }

        if (request.getMatchStatus() == MatchStatus.SCHEDULED && StringUtils.isEmpty(request.getScheduledStartTime())) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                    "scheduledStartTime is required for scheduled match");
        }

        academyService.getAcademyById(academyId);

        List<AcademyDto> academyDtos = coachService.getAcademiesByCoachUserId(userId);

        if (CollectionUtils.isEmpty(academyDtos)
                || academyDtos.stream().noneMatch(academyDto -> academyDto.getId().equals(academyId))) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Coach is not associated with academy");
        }

        PickleballMatch badmintonMatch = modelMapper.map(request, PickleballMatch.class);
        String matchId = UUID.randomUUID().toString();
        badmintonMatch.setId(matchId);
        badmintonMatch.setCreatedByUserProfile(UserProfile.builder().id(userId).build());
        badmintonMatch.setAcademy(Academy.builder().id(academyId).build());
        badmintonMatch.setCreatedAtTimestampUtc(Timestamp.from(Instant.now()));
        badmintonMatch.setUpdatedAtTimestampUtc(Timestamp.from(Instant.now()));
        badmintonMatch.setIsAutogenerated(request.getIsAutogenerated() != null && request.getIsAutogenerated());

        if (!StringUtils.isEmpty(request.getTournamentId())) {
            badmintonMatch.setTournament(PickleballTournament.builder().id(request.getTournamentId()).build());
        }
        badmintonMatch.setInactive(false);
        if (request.getIsLivestreamed() == null) {
            badmintonMatch.setIsLivestreamed(false);
        }
        if (request.getIsRecordingEnabled() == null) {
            badmintonMatch.setIsRecordingEnabled(false);
        }
        if (request.getMatchStatus() == MatchStatus.IN_PROGRESS) {
            badmintonMatch.setStartTime(Instant.now().toString());
        }

        PickleballTournament tournament = null;

        if (StringUtils.isNotEmpty(request.getTournamentId())) {
            tournament = pickleballTournamentRepo.findById(request.getTournamentId())
                    .orElseThrow(() -> new ResourceException(ErrorCodes.INVALID_REQUEST, "Tournament not found"));
        }

        // Set scorer either from tournament or from the request body
        badmintonMatch.setScorerUserProfile(getUserProfileWithFallback(
                tournament != null ? tournament.getScorerUserProfile() : null, request.getScorerUserId()));

        // Set referee either from tournament or from the request body
        badmintonMatch.setRefereeUserProfile(getUserProfileWithFallback(
                tournament != null ? tournament.getRefereeUserProfile() : null, request.getRefereeUserId()));

        // Set matchOfficial either from tournament or from the request body
        badmintonMatch.setMatchOfficialUserProfile(
                getUserProfileWithFallback(tournament != null ? tournament.getMatchOfficialUserProfile() : null,
                        request.getMatchOfficialUserId()));

        badmintonMatch.setCourt(
                StringUtils.isEmpty(request.getCourtId()) ? null : Court.builder().id(request.getCourtId()).build());

        List<String> teamIds = request.getTeamIds() != null
                ? request.getTeamIds().stream().map(PickleballTeamDto::getId).collect(Collectors.toList())
                : new ArrayList<>();

        badmintonMatch.setTeams(
                request.getGameFormat() == GameFormat.DOUBLES ? buildPickleballTeamMapping(matchId, teamIds)
                        : null);
        badmintonMatch.setSinglePlayers(request.getGameFormat() == GameFormat.SINGLES
                ? buildPickleballSinglesPlayerMapping(matchId, request.getPlayers())
                : null);

        // for game format TEAMS
        badmintonMatch.setPickleballMatchTeamPlayers(request.getTeamIds() != null
                ? buildPickleballTeamMatchPlayerMapping(matchId, request.getTeamIds())
                : null);

        if (StringUtils.isNotEmpty(request.getScorerUserId())) {
            CompletableFuture.runAsync(() -> {
                log.info("Sending notification to scorer");
                UserProfileDto userProfileDto = userProfileRepo.findById(request.getScorerUserId())
                        .map(userProfile -> modelMapper.map(userProfile, UserProfileDto.class)).orElse(null);
                sendNotificationToScorer(Arrays.asList(userProfileDto), matchId, academyId);
                log.info("Notification sent to scorer");
            });
        }

        pickleballMatchRepo.save(badmintonMatch);

        return getMatch(userId, academyId, matchId, sport);
    }

    @Override
    public PickleballMatchDto createMatch(String userId, CreatePickleballMatchDto request, Sports sport)
            throws ResourceException {
        if (request.getGameFormat() == GameFormat.SINGLES && CollectionUtils.isEmpty(request.getPlayers())) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Player are required for singles match");
        } else if (request.getGameFormat() == GameFormat.DOUBLES && CollectionUtils.isEmpty(request.getTeamIds())) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Teams are required for doubles match");
        }

        if (request.getMatchStatus() == MatchStatus.SCHEDULED && StringUtils.isEmpty(request.getScheduledStartTime())) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                    "scheduledStartTime is required for scheduled match");
        }

        PickleballMatch badmintonMatch = modelMapper.map(request, PickleballMatch.class);
        String matchId = UUID.randomUUID().toString();
        badmintonMatch.setId(matchId);
        badmintonMatch.setCreatedByUserProfile(UserProfile.builder().id(userId).build());
        badmintonMatch.setCreatedAtTimestampUtc(Timestamp.from(Instant.now()));
        badmintonMatch.setUpdatedAtTimestampUtc(Timestamp.from(Instant.now()));
        badmintonMatch.setInactive(false);

        if (!StringUtils.isEmpty(request.getTournamentId())) {
            badmintonMatch.setTournament(PickleballTournament.builder().id(request.getTournamentId()).build());
        }

        if (request.getIsLivestreamed() == null) {
            badmintonMatch.setIsLivestreamed(false);
        }
        if (request.getIsRecordingEnabled() == null) {
            badmintonMatch.setIsRecordingEnabled(false);
        }
        if (request.getMatchStatus() == MatchStatus.IN_PROGRESS) {
            badmintonMatch.setStartTime(Instant.now().toString());
        }

        PickleballTournament tournament = null;
        if (StringUtils.isNotEmpty(request.getTournamentId())) {
            tournament = pickleballTournamentRepo.findById(request.getTournamentId())
                    .orElseThrow(() -> new ResourceException(ErrorCodes.INVALID_REQUEST, "Tournament not found"));
        }

        // Set scorer either from tournament or from the request body
        badmintonMatch.setScorerUserProfile(getUserProfileWithFallback(
                tournament != null ? tournament.getScorerUserProfile() : null, request.getScorerUserId()));

        // Set referee either from tournament or from the request body
        badmintonMatch.setRefereeUserProfile(getUserProfileWithFallback(
                tournament != null ? tournament.getRefereeUserProfile() : null, request.getRefereeUserId()));

        // Set matchOfficial either from tournament or from the request body
        badmintonMatch.setMatchOfficialUserProfile(
                getUserProfileWithFallback(tournament != null ? tournament.getMatchOfficialUserProfile() : null,
                        request.getMatchOfficialUserId()));

        badmintonMatch.setCourt(
                StringUtils.isEmpty(request.getCourtId()) ? null : Court.builder().id(request.getCourtId()).build());

        List<String> teamIds = request.getTeamIds() != null
                ? request.getTeamIds().stream().map(PickleballTeamDto::getId).collect(Collectors.toList())
                : new ArrayList<>();

        badmintonMatch.setTeams(
                request.getGameFormat() == GameFormat.DOUBLES ? buildPickleballTeamMapping(matchId, teamIds) : null);
        badmintonMatch.setSinglePlayers(request.getGameFormat() == GameFormat.SINGLES
                ? buildPickleballSinglesPlayerMapping(matchId, request.getPlayers())
                : null);

        // for game format TEAMS
        badmintonMatch.setPickleballMatchTeamPlayers(
                request.getTeamIds() != null ? buildPickleballTeamMatchPlayerMapping(matchId, request.getTeamIds())
                        : null);

        if (StringUtils.isNotEmpty(request.getScorerUserId())) {
            CompletableFuture.runAsync(() -> {
                log.info("Sending notification to scorer");
                UserProfileDto userProfileDto = userProfileRepo.findById(request.getScorerUserId())
                        .map(userProfile -> modelMapper.map(userProfile, UserProfileDto.class)).orElse(null);
                sendNotificationToScorer(Arrays.asList(userProfileDto), matchId, null);
                log.info("Notification sent to scorer");
            });
        }

        pickleballMatchRepo.save(badmintonMatch);

        return getMatch(userId, matchId, sport);
    }

    @Override
    public PaginatedResponse<PickleballMatchDto> getMatchesByStatusSpecification(String userId, GenericFilter filter)
            throws ResourceException {
        if (StringUtils.isNotEmpty(filter.getAcademyId())) {
            academyService.getAcademyById(filter.getAcademyId());
        }

        PickleballMatchSpecification spec = new PickleballMatchSpecification(filter, filter.getMatchStatus());

        Pageable pageable = PageRequest.of(filter.getCurrentPage(),
                filter.getPageSize());
        Page<PickleballMatch> matches = pickleballMatchRepo.findAll(spec, pageable);

        long totalElements = matches.getTotalElements();
        int totalPages = matches.getTotalPages();

        if (matches.isEmpty()) {
            return new PaginatedResponse<PickleballMatchDto>(List.of(), totalElements,
                    totalPages, filter.getCurrentPage());
        }

        List<PickleballMatchDto> list = matches.getContent()
                .stream()
                .map(match -> {
                    try {
                        return convertToDto(userId, match, Sports.PICKLEBALL);
                    } catch (ResourceException e) {
                        log.error("Error while converting match to dto", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return new PaginatedResponse<PickleballMatchDto>(list, totalElements,
                totalPages, filter.getCurrentPage());
    }

    @Override
    public List<PickleballMatchDto> getMatchesByStatus(String userId, String academyId, MatchStatus matchStatus,
            String tournamentId, String searchText, Sports sport) throws ResourceException {
        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }
        List<PickleballMatch> matches;

        if (StringUtils.isEmpty(tournamentId)) {
            if (StringUtils.isEmpty(academyId)) {
                matches = pickleballMatchRepo.findByMatchStatusIn(List.of(matchStatus)).stream()
                        .filter(match -> !match.getInactive()).toList();
            } else {
                matches = pickleballMatchRepo.findByMatchStatusAndAcademy_Id(matchStatus, academyId).stream()
                        .filter(match -> !match.getInactive()).toList();
            }
        } else {
            if (StringUtils.isEmpty(academyId)) {
                matches = pickleballMatchRepo.findByMatchStatusAndTournament_Id(matchStatus, tournamentId).stream()
                        .filter(match -> !match.getInactive()).toList();
            } else {
                matches = pickleballMatchRepo
                        .findByMatchStatusAndAcademy_IdAndTournament_Id(matchStatus, academyId, tournamentId).stream()
                        .filter(match -> !match.getInactive()).toList();
            }
        }

        if (CollectionUtils.isEmpty(matches)) {
            return List.of();
        }

        if (!CollectionUtils.isEmpty(matches) && StringUtils.isNotEmpty(searchText) && searchText.length() > 2) {
            matches = matches.stream().filter(
                    match -> {
                        // Search for court as well
                        boolean foundAnyCourts = match.getCourt() != null
                                && StringUtils.containsIgnoreCase(match.getCourt().getCourtName(), searchText);

                        // Search in single players (for SINGLES)
                        boolean foundInSinglePlayers = match.getSinglePlayers() != null &&
                                match.getSinglePlayers().stream()
                                        .anyMatch(player -> (player.getPlayerUserProfile() != null &&
                                                StringUtils.containsIgnoreCase(
                                                        player.getPlayerUserProfile().getDisplayName(), searchText))
                                                ||
                                                StringUtils.containsIgnoreCase(player.getGuestName(), searchText));

                        // Search in team players (for DOUBLES)
                        boolean foundInTeamPlayers = match.getPickleballMatchTeamPlayers() != null &&
                                match.getPickleballMatchTeamPlayers().stream()
                                        .anyMatch(teamPlayer -> (teamPlayer.getPlayerUserProfile() != null &&
                                                StringUtils.containsIgnoreCase(
                                                        teamPlayer.getPlayerUserProfile().getDisplayName(), searchText))
                                                ||
                                                StringUtils.containsIgnoreCase(teamPlayer.getGuestName(), searchText));

                        // Search in team names (if teams mapping exists)
                        boolean foundInTeamNames = match.getTeams() != null &&
                                match.getTeams().stream().anyMatch(team -> team.getTeam() != null &&
                                        StringUtils.containsIgnoreCase(team.getTeam().getTeamName(), searchText));
                        return foundInSinglePlayers || foundInTeamPlayers || foundInTeamNames || foundAnyCourts;
                    }).toList();
        }

        return sort(matches).stream()
                .map(m -> {
                    try {
                        return convertToDto(userId, m, sport);
                    } catch (ResourceException e) {
                        log.error("Error while converting match to dto", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<PickleballMatchDto> getAllMatches(String userId, String academyId, String tournamentId,
            String searchText, Sports sport) throws ResourceException {
        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }
        List<PickleballMatch> matches;

        if (StringUtils.isEmpty(tournamentId)) {
            if (StringUtils.isNotEmpty(academyId)) {
                matches = StreamSupport.stream(pickleballMatchRepo.findAll().spliterator(), false)
                        .filter(match -> !match.getInactive()).toList();
            } else {
                matches = pickleballMatchRepo.findByAcademy_Id(academyId).stream().filter(match -> !match.getInactive())
                        .toList();
            }
        } else {
            if (StringUtils.isNotEmpty(academyId)) {
                matches = pickleballMatchRepo.findByTournament_Id(tournamentId).stream()
                        .filter(match -> !match.getInactive()).toList();
            } else {
                matches = pickleballMatchRepo.findByAcademy_IdAndTournament_Id(academyId, tournamentId).stream()
                        .filter(match -> !match.getInactive()).toList();
            }
        }

        if (CollectionUtils.isEmpty(matches)) {
            return List.of();
        }

        if (!CollectionUtils.isEmpty(matches) && StringUtils.isNotEmpty(searchText) && searchText.length() > 2) {
            matches = matches.stream().filter(
                    match -> {
                        // Search for court as well
                        boolean foundAnyCourts = match.getCourt() != null
                                && StringUtils.containsIgnoreCase(match.getCourt().getCourtName(), searchText);

                        // Search in single players (for SINGLES)
                        boolean foundInSinglePlayers = match.getSinglePlayers() != null &&
                                match.getSinglePlayers().stream()
                                        .anyMatch(player -> (player.getPlayerUserProfile() != null &&
                                                StringUtils.containsIgnoreCase(
                                                        player.getPlayerUserProfile().getDisplayName(), searchText))
                                                ||
                                                StringUtils.containsIgnoreCase(player.getGuestName(), searchText));

                        // Search in team players (for DOUBLES)
                        boolean foundInTeamPlayers = match.getPickleballMatchTeamPlayers() != null &&
                                match.getPickleballMatchTeamPlayers().stream()
                                        .anyMatch(teamPlayer -> (teamPlayer.getPlayerUserProfile() != null &&
                                                StringUtils.containsIgnoreCase(
                                                        teamPlayer.getPlayerUserProfile().getDisplayName(), searchText))
                                                ||
                                                StringUtils.containsIgnoreCase(teamPlayer.getGuestName(), searchText));

                        // Search in team names (if teams mapping exists)
                        boolean foundInTeamNames = match.getTeams() != null &&
                                match.getTeams().stream().anyMatch(team -> team.getTeam() != null &&
                                        StringUtils.containsIgnoreCase(team.getTeam().getTeamName(), searchText));
                        return foundInSinglePlayers || foundInTeamPlayers || foundInTeamNames || foundAnyCourts;
                    }).toList();
        }

        return sort(matches).stream()
                .map(m -> {
                    try {
                        return convertToDto(userId, m, sport);
                    } catch (ResourceException e) {
                        log.error("Error while converting match to dto", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public PickleballMatchDto getMatch(String userId, String academyId, String matchId, Sports sport)
            throws ResourceException {
        academyService.getAcademyById(academyId);
        PickleballMatch match = pickleballMatchRepo.findByAcademy_IdAndId(academyId, matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));
        return convertToDto(userId, match, sport);
    }

    @Override
    public PickleballMatchDto getMatch(String userId, String matchId, Sports sport) throws ResourceException {
        PickleballMatch match = pickleballMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));
        return convertToDto(userId, match, sport);
    }

    @Override
    public void updateMatchStatus(String userId, String matchId, MatchStatus matchStatus, Sports sport)
            throws ResourceException {
        PickleballMatch match = pickleballMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));

        boolean isCreatedUser = match.getCreatedByUserProfile() != null
                && StringUtils.isNotEmpty(match.getCreatedByUserProfile().getId())
                && match.getCreatedByUserProfile().getId().equalsIgnoreCase(userId);
        boolean isScorerUser = match.getScorerUserProfile() != null
                && StringUtils.isNotEmpty(match.getScorerUserProfile().getId())
                && match.getScorerUserProfile().getId().equalsIgnoreCase(userId);
        boolean isRefereeUser = match.getRefereeUserProfile() != null
                && StringUtils.isNotEmpty(match.getRefereeUserProfile().getId())
                && match.getRefereeUserProfile().getId().equalsIgnoreCase(userId);
        boolean isMatchOfficialUser = match.getMatchOfficialUserProfile() != null
                && StringUtils.isNotEmpty(match.getMatchOfficialUserProfile().getId())
                && match.getMatchOfficialUserProfile().getId().equalsIgnoreCase(userId);

        boolean isAuthorized = StringUtils.isNotEmpty(userId) && (isCreatedUser || isScorerUser || isRefereeUser
                || isMatchOfficialUser);

        if (isAuthorized) {
            match.setMatchStatus(matchStatus);
            match.setUpdatedAtTimestampUtc(Timestamp.from(Instant.now()));
            if (matchStatus == MatchStatus.ENDED) {
                match.setEndTime(Instant.now().toString());
            } else if (matchStatus == MatchStatus.IN_PROGRESS) {
                match.setStartTime(Instant.now().toString());
            }
            pickleballMatchRepo.save(match);
        } else {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "User is not authorized to update match status");
        }

    }

    @Override
    public void deleteMatch(String academyId, String matchId, Sports sport) throws ResourceException {
        academyService.getAcademyById(academyId);

        PickleballMatch match = pickleballMatchRepo.findByAcademy_IdAndId(academyId, matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));

        match.setInactive(true);

        pickleballMatchRepo.save(match);
    }

    @Override
    public void deleteMatch(String matchId, Sports sport) throws ResourceException {
        PickleballMatch match = pickleballMatchRepo.findByAcademyIsNullAndId(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));

        match.setInactive(true);
        pickleballMatchRepo.save(match);
    }

    @Override
    public PickleballMatchDto updateMatch(String userId, String academyId, String matchId,
            UpdatePickleballMatchDto request, Sports sport) throws ResourceException {
        academyService.getAcademyById(academyId);

        Optional<PickleballMatch> match = pickleballMatchRepo.findByAcademy_IdAndId(academyId, matchId);

        if (match.isEmpty() || match.get().getInactive()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
        }

        if (match.get().getGameFormat() == GameFormat.SINGLES) {
            updateSinglePlayers(match.get(), request.getPlayers(), sport);
        } else if (match.get().getGameFormat() == GameFormat.DOUBLES) {
            if (!CollectionUtils.isEmpty(request.getTeamIds())) {
                List<String> teamIds = request.getTeamIds() != null
                        ? request.getTeamIds().stream().map(PickleballTeamDto::getId).collect(Collectors.toList())
                        : new ArrayList<>();
                updateTeams(match.get(), teamIds, sport);
            } else if (!CollectionUtils.isEmpty(request.getTeams())) {
                updateTeamPlayers(match.get(), request.getTeams(), sport);
            }
        }

        if (StringUtils.isEmpty(request.getCourtId())) {
            match.get().setCourt(null);
        } else {
            match.get().setCourt(Court.builder().id(request.getCourtId()).build());
        }

        if (StringUtils.isEmpty(request.getRefereeUserId())) {
            match.get().setRefereeUserProfile(null);
        } else {
            match.get().setRefereeUserProfile(UserProfile.builder().id(request.getRefereeUserId()).build());
        }

        if (StringUtils.isEmpty(request.getScorerUserId())) {
            match.get().setScorerUserProfile(null);
        } else {
            match.get().setScorerUserProfile(UserProfile.builder().id(request.getScorerUserId()).build());
        }

        if (StringUtils.isEmpty(request.getMatchOfficialUserId())) {
            match.get().setMatchOfficialUserProfile(null);
        } else {
            match.get().setMatchOfficialUserProfile(UserProfile.builder().id(request.getMatchOfficialUserId()).build());
        }

        if (StringUtils.isNotEmpty(request.getScorerUserId())) {
            CompletableFuture.runAsync(() -> {
                log.info("Sending notification to scorer");
                UserProfileDto userProfileDto = userProfileRepo.findById(request.getScorerUserId())
                        .map(userProfile -> modelMapper.map(userProfile, UserProfileDto.class)).orElse(null);
                sendNotificationToScorer(Arrays.asList(userProfileDto), matchId, academyId);
                log.info("Notification sent to scorer");
            });
        }
        match.get().setScheduledStartTime(request.getScheduledStartTime());
        match.get().setMatchStatus(request.getMatchStatus());
        match.get().setMaxPoints(request.getMaxPoints());
        match.get().setMaxRounds(request.getMaxRounds());

        if (request.getIsLivestreamed() == null) {
            match.get().setIsLivestreamed(false);
        }
        if (request.getIsRecordingEnabled() == null) {
            match.get().setIsRecordingEnabled(false);
        }

        match.get().setUpdatedAtTimestampUtc(Timestamp.from(Instant.now()));
        pickleballMatchRepo.save(match.get());

        return getMatch(academyId, matchId, sport);
    }

    @Override
    public PickleballMatchDto updateMatch(String userId, String matchId, UpdatePickleballMatchDto request, Sports sport)
            throws ResourceException {
        Optional<PickleballMatch> match = pickleballMatchRepo.findByAcademyIsNullAndId(matchId);
        if (match.isEmpty() || match.get().getInactive()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
        }

        if (match.get().getGameFormat() == GameFormat.SINGLES) {
            updateSinglePlayers(match.get(), request.getPlayers(), sport);
        } else if (match.get().getGameFormat() == GameFormat.DOUBLES) {
            if (!CollectionUtils.isEmpty(request.getTeamIds())) {
                List<String> teamIds = request.getTeamIds() != null
                        ? request.getTeamIds().stream().map(PickleballTeamDto::getId).collect(Collectors.toList())
                        : new ArrayList<>();
                updateTeams(match.get(), teamIds, sport);
            } else if (!CollectionUtils.isEmpty(request.getTeams())) {
                updateTeamPlayers(match.get(), request.getTeams(), sport);
            }
        }

        if (StringUtils.isEmpty(request.getCourtId())) {
            match.get().setCourt(null);
        } else {
            match.get().setCourt(Court.builder().id(request.getCourtId()).build());
        }

        if (StringUtils.isEmpty(request.getRefereeUserId())) {
            match.get().setRefereeUserProfile(null);
        } else {
            match.get().setRefereeUserProfile(UserProfile.builder().id(request.getRefereeUserId()).build());
        }

        if (StringUtils.isEmpty(request.getScorerUserId())) {
            match.get().setScorerUserProfile(null);
        } else {
            match.get().setScorerUserProfile(UserProfile.builder().id(request.getScorerUserId()).build());
        }

        if (StringUtils.isEmpty(request.getMatchOfficialUserId())) {
            match.get().setMatchOfficialUserProfile(null);
        } else {
            match.get().setMatchOfficialUserProfile(UserProfile.builder().id(request.getMatchOfficialUserId()).build());
        }

        if (StringUtils.isNotEmpty(request.getScorerUserId())) {
            CompletableFuture.runAsync(() -> {
                log.info("Sending notification to scorer");
                UserProfileDto userProfileDto = userProfileRepo.findById(request.getScorerUserId())
                        .map(userProfile -> modelMapper.map(userProfile, UserProfileDto.class)).orElse(null);
                sendNotificationToScorer(Arrays.asList(userProfileDto), matchId, null);
                log.info("Notification sent to scorer");
            });
        }
        match.get().setScheduledStartTime(request.getScheduledStartTime());
        match.get().setMatchStatus(request.getMatchStatus());
        match.get().setMaxPoints(request.getMaxPoints());
        match.get().setMaxRounds(request.getMaxRounds());

        if (request.getIsLivestreamed() == null) {
            match.get().setIsLivestreamed(false);
        }
        if (request.getIsRecordingEnabled() == null) {
            match.get().setIsRecordingEnabled(false);
        }

        match.get().setUpdatedAtTimestampUtc(Timestamp.from(Instant.now()));
        pickleballMatchRepo.save(match.get());

        return getMatch(userId, matchId, sport);
    }

    // Helper method to update single players
    private void updateSinglePlayers(PickleballMatch match, List<PickleBallTeamPlayerDto> players, Sports sport) {
        pickleballSinglesPlayerMappingRepo.deleteAll(match.getSinglePlayers());

        List<PickleballSinglesPlayerMapping> mappings = players.stream()
                .map(player -> {
                    PickleballSinglesPlayerMapping mapping = new PickleballSinglesPlayerMapping();

                    if (StringUtils.isNotEmpty(player.getPlayerUserId())) {
                        UserProfile userProfile = null;
                        try {
                            userProfile = userProfileRepo.findById(player.getPlayerUserId()).orElseThrow(
                                    () -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Player not found"));
                        } catch (ResourceException e) {
                            log.error("Error while updating single players", e);
                        }
                        mapping.setPickleballMatch(match);
                        mapping.setPlayerUserProfile(userProfile);
                    } else {
                        mapping.setPickleballMatch(match);
                        mapping.setGuestName(player.getGuestPlayerName());
                        mapping.setPlayerUserProfile(null);
                    }
                    return mapping;
                })
                .toList();
        match.setSinglePlayers(new ArrayList<>(mappings));

    }

    // Helper method to update teams
    private void updateTeams(PickleballMatch match, List<String> teamIds, Sports sport) {
        pickleballTeamMappingRepo.deleteAll(match.getTeams());

        List<PickleballTeamMapping> mappings = teamIds.stream().map(teamId -> {
            PickleballTeam team = null;
            try {
                team = pickleballTeamRepo.findById(teamId)
                        .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
            } catch (ResourceException e) {
                log.error("Error while updating teams", e);
            }

            PickleballTeamMapping mapping = new PickleballTeamMapping();
            mapping.setTeam(team);
            mapping.setPickleballMatch(match);
            return mapping;
        }).toList();

        match.setTeams(new ArrayList<>(mappings));
    }

    // Helper method to update teams when game format is TEAMS
    private void updateTeamPlayers(PickleballMatch match, List<TeamMatchPlayerDto> teams, Sports sport) {
        pickleballMatchTeamPlayerMappingRepo.deleteAll(match.getPickleballMatchTeamPlayers());

        List<PickleballMatchTeamPlayerMapping> mappings = new ArrayList<>();

        for (TeamMatchPlayerDto teamDto : teams) {
            String teamId = teamDto.getTeamId();

            PickleballTeam team = null;

            try {
                team = pickleballTeamRepo.findById(teamId)
                        .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
            } catch (ResourceException e) {
                log.error("Error while updating team players", e);
                continue; // Skip this team
            }

            // Handle registered players
            if (teamDto.getPlayers() != null) {
                for (String playerId : teamDto.getPlayers()) {
                    if (playerId == null || StringUtils.isBlank(playerId))
                        continue;

                    try {
                        UserProfile player = userProfileRepo.findById(playerId)
                                .orElseThrow(
                                        () -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Player not found"));

                        PickleballMatchTeamPlayerMapping mapping = new PickleballMatchTeamPlayerMapping();
                        mapping.setPickleballMatch(match);
                        mapping.setTeam(team);
                        mapping.setPlayerUserProfile(player);
                        mappings.add(mapping);
                    } catch (ResourceException e) {
                        log.error("Error while fetching player with ID: {}", playerId, e);
                    }
                }
            }

            // Handle guest players
            if (teamDto.getGuestPlayers() != null) {
                for (String guestName : teamDto.getGuestPlayers()) {
                    if (guestName == null || StringUtils.isBlank(guestName))
                        continue;

                    PickleballMatchTeamPlayerMapping mapping = new PickleballMatchTeamPlayerMapping();
                    mapping.setPickleballMatch(match);
                    mapping.setTeam(team);
                    mapping.setGuestName(guestName);
                    mappings.add(mapping);
                }
            }
        }
        match.setPickleballMatchTeamPlayers(new ArrayList<>(mappings));
    }

    @Override
    public PickleballMatchDto convertToDto(String userId, PickleballMatch match, Sports sport)
            throws ResourceException {
        PickleballMatchDto badmintonMatchDto = modelMapper.map(match, PickleballMatchDto.class);
        boolean isCreatedUser = StringUtils.isNotEmpty(userId) && match.getCreatedByUserProfile() != null
                && StringUtils.isNotEmpty(match.getCreatedByUserProfile().getId())
                && match.getCreatedByUserProfile().getId().equalsIgnoreCase(userId);
        boolean isScorerUser = StringUtils.isNotEmpty(userId) && match.getScorerUserProfile() != null
                && StringUtils.isNotEmpty(match.getScorerUserProfile().getId())
                && match.getScorerUserProfile().getId().equalsIgnoreCase(userId);
        boolean isRefereeUser = StringUtils.isNotEmpty(userId) && match.getRefereeUserProfile() != null
                && StringUtils.isNotEmpty(match.getRefereeUserProfile().getId())
                && match.getRefereeUserProfile().getId().equalsIgnoreCase(userId);
        boolean isMatchOfficial = StringUtils.isNotEmpty(userId) && match.getMatchOfficialUserProfile() != null
                && StringUtils.isNotEmpty(match.getMatchOfficialUserProfile().getId())
                && match.getMatchOfficialUserProfile().getId().equalsIgnoreCase(userId);

        badmintonMatchDto.setCanManage(isCreatedUser || isScorerUser || isRefereeUser || isMatchOfficial);

        badmintonMatchDto.setIsAutogenerated(match.getIsAutogenerated() != null && match.getIsAutogenerated());

        if (match.getGameFormat() == GameFormat.DOUBLES) {
            badmintonMatchDto.setTeams(new ArrayList<>());
            // First check in badmintonMatchTeamPlayers, if no mapping exists then check in
            // teams
            if (!CollectionUtils.isEmpty(match.getPickleballMatchTeamPlayers())) {
                // New handling for TEAMS format
                Map<String, PickleballTeamDto> teamMap = new HashMap<>();

                for (PickleballMatchTeamPlayerMapping mapping : match.getPickleballMatchTeamPlayers()) {
                    String teamId = mapping.getTeam().getId();
                    PickleballTeamDto teamDto = teamMap.computeIfAbsent(teamId, id -> {
                        PickleballTeamDto dto = new PickleballTeamDto();
                        dto.setId(id);
                        dto.setTeamName(mapping.getTeam().getTeamName());
                        dto.setPlayers(new ArrayList<>());
                        return dto;
                    });

                    PickleBallTeamPlayerDto playerDto = new PickleBallTeamPlayerDto();
                    if (mapping.getPlayerUserProfile() != null) {
                        playerDto.setPlayerUserId(mapping.getPlayerUserProfile().getId());
                        playerDto.setUserProfile(
                                modelMapper.map(mapping.getPlayerUserProfile(), UserProfileMinDto.class));
                    } else {
                        playerDto.setGuestPlayerName(mapping.getGuestName());
                    }

                    teamDto.getPlayers().add(playerDto);
                }

                badmintonMatchDto.setTeams(new ArrayList<>(teamMap.values()));
            } else if (!CollectionUtils.isEmpty(match.getTeams())) {
                List<PickleballTeamMapping> badmintonTeamMappings = match.getTeams();
                List<PickleballTeamDto> teamDtos = badmintonTeamMappings.stream().map(badmintonTeamMapping -> {
                    PickleballTeamDto teamDto = new PickleballTeamDto();
                    teamDto.setId(badmintonTeamMapping.getTeam().getId());
                    teamDto.setTeamName(badmintonTeamMapping.getTeam().getTeamName());
                    List<PickleBallTeamPlayerDto> teamPlayerDtos = badmintonTeamMapping.getTeam().getPlayers().stream()
                            .map(teamPlayer -> {
                                PickleBallTeamPlayerDto teamPlayerDto = new PickleBallTeamPlayerDto();
                                teamPlayerDto.setPlayerUserId(teamPlayer.getPlayerUserProfile() == null ? null
                                        : teamPlayer.getPlayerUserProfile().getId());
                                teamPlayerDto.setGuestPlayerName(teamPlayer.getGuestPlayerName());
                                teamPlayerDto.setUserProfile(teamPlayer.getPlayerUserProfile() == null ? null
                                        : modelMapper.map(teamPlayer.getPlayerUserProfile(), UserProfileMinDto.class));
                                return teamPlayerDto;
                            }).toList();
                    teamDto.setPlayers(teamPlayerDtos);
                    return teamDto;
                }).toList();
                badmintonMatchDto.setTeams(teamDtos);
            }
        } else if (match.getGameFormat() == GameFormat.SINGLES) {
            List<PickleballSinglesPlayerMapping> badmintonSinglesPlayerMappings = match.getSinglePlayers();
            List<PickleBallTeamPlayerDto> teamPlayerDtos = badmintonSinglesPlayerMappings.stream()
                    .map(badmintonSinglesPlayerMapping -> {
                        PickleBallTeamPlayerDto teamPlayerDto = new PickleBallTeamPlayerDto();
                        teamPlayerDto
                                .setPlayerUserId(badmintonSinglesPlayerMapping.getPlayerUserProfile() == null ? null
                                        : badmintonSinglesPlayerMapping.getPlayerUserProfile().getId());
                        teamPlayerDto.setUserProfile(badmintonSinglesPlayerMapping.getPlayerUserProfile() == null ? null
                                : modelMapper.map(badmintonSinglesPlayerMapping.getPlayerUserProfile(),
                                        UserProfileMinDto.class));
                        teamPlayerDto.setGuestPlayerName(badmintonSinglesPlayerMapping.getGuestName());
                        return teamPlayerDto;
                    }).toList();
            badmintonMatchDto.setPlayers(teamPlayerDtos);
        }

        badmintonMatchDto.setCreatedByUserId(
                match.getCreatedByUserProfile() != null ? match.getCreatedByUserProfile().getId() : null);
        badmintonMatchDto.setCreatedByUserProfile(match.getCreatedByUserProfile() != null
                ? modelMapper.map(match.getCreatedByUserProfile(), UserProfileMinDto.class)
                : null);

        if (match.getScorerUserProfile() != null) {
            badmintonMatchDto.setScorer(modelMapper.map(match.getScorerUserProfile(), UserProfileMinDto.class));
        }

        if (match.getRefereeUserProfile() != null) {
            badmintonMatchDto.setReferee(modelMapper.map(match.getRefereeUserProfile(), UserProfileMinDto.class));
        }

        if (match.getMatchOfficialUserProfile() != null) {
            badmintonMatchDto.setMatchOfficial(modelMapper.map(match.getMatchOfficialUserProfile(),
                    UserProfileMinDto.class));
        }

        if (match.getCourt() != null) {
            badmintonMatchDto.setCourt(modelMapper.map(match.getCourt(), CourtDto.class));
        }

        if (match.getTournament() != null) {
            badmintonMatchDto.setTournamentId(match.getTournament().getId());
        }

        Optional<PickleballMatchPlayDetail> details = pickleballMatchPlayDetailRepo
                .findByPickleballMatch_Id(match.getId());

        if (details.isPresent()) {
            // PickleballMatchDetailDto matchDetailDto =
            // toPickleballMatchDetailDto(details.get());

            // badmintonMatchDto.setRounds(matchDetailDto.getRounds());

            List<PickleballMatchRound> matchRounds = details.get().getPickleballMatchRounds();
            List<PickleballMatchRoundsPlayDetail> roundDetails = matchRounds.stream()
                    .flatMap(each -> each.getPickleballMatchRoundsPlayDetails().stream())
                    .toList();

            List<PickleballPlayerScoreDto> playerScoreDtos = roundDetails.stream()
                    .map(each -> toPickleballPlayerScoreDto(each)).toList();

            badmintonMatchDto.setRounds(playerScoreDtos);
        }

        return badmintonMatchDto;
    }

    @Override
    public List<PickleballMatchDto> convertToDto(String userId, List<PickleballMatch> match, Sports sport)
            throws ResourceException {
        return match.stream()
                .map(m -> {
                    try {
                        return convertToDto(userId, m, sport);
                    } catch (ResourceException e) {
                        log.error("Error while converting match to dto", e);
                        return null;
                    }
                })
                .toList();
    }

    @Override
    public Map<String, String> streamMatch(String matchId, StreamingStatus status) throws ResourceException {
        PickleballMatch badmintonMatch = pickleballMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));

        if (badmintonMatch.getMatchStatus() != null
                && badmintonMatch.getMatchStatus() == MatchStatus.ENDED) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Match is already ended");
        }

        Map<String, String> response = new HashMap<>();
        if (status == StreamingStatus.STARTED) {
            String channel = String.format("s_%s", matchId);
            String token = rtcTokenProvider.generateToken(channel, NO_UID);
            response.put("channel", channel);
            response.put("token", token);
            pickleballMatchRepo.startStreaming(matchId, channel, token);
        } else if (status == StreamingStatus.ENDED) {
            pickleballMatchRepo.endStreaming(matchId);
            BadmintonScoreNotificationDto badmintonScoreNotificationDto = new BadmintonScoreNotificationDto();
            badmintonScoreNotificationDto.setMatchId(matchId);
            badmintonScoreNotificationDto.setType(MatchWSMessageType.MATCH_LIVE_STREAMING_END);
            webSocketBrokerService.sendMessageToMatch(matchId, AppConstants.GSON.toJson(badmintonScoreNotificationDto));
        }
        return response;
    }

    @Override
    public PickleballMatchDetailDto submitMatchDetails(String matchId,
            PickleballMatchDetailRequestDto pickleballMatchDetailRequestDto, Sports sport) throws ResourceException {

        PickleballMatch match = pickleballMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));

        if (match.getMatchStatus() != MatchStatus.IN_PROGRESS) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Match is not in progress");
        }

        // Fetch existing play detail or create new
        PickleballMatchPlayDetail playDetail = pickleballMatchPlayDetailRepo
                .findByPickleballMatch_Id(matchId)
                .orElseGet(() -> {
                    PickleballMatchPlayDetail p = new PickleballMatchPlayDetail();
                    p.setPickleballMatch(match);
                    p.setTournament(match.getTournament());
                    p.setPickleballMatchRounds(new ArrayList<>());
                    return p;
                });

        // ---- Winning info at match level ----
        if (match.getGameFormat() == GameFormat.SINGLES) {
            if (StringUtils.isNotEmpty(pickleballMatchDetailRequestDto.getWinningPlayerUserId())) {
                playDetail.setWinningPlayerUserProfile(
                        UserProfile.builder().id(pickleballMatchDetailRequestDto.getWinningPlayerUserId()).build());
                playDetail.setWinningGuestPlayerName(null);
                playDetail.setWinningTeam(null);
                playDetail.setIsTied(false);
            } else if (StringUtils.isNotEmpty(pickleballMatchDetailRequestDto.getWinningGuestPlayerName())) {
                playDetail.setWinningGuestPlayerName(pickleballMatchDetailRequestDto.getWinningGuestPlayerName());
                playDetail.setWinningPlayerUserProfile(null);
                playDetail.setWinningTeam(null);
                playDetail.setIsTied(false);
            } else if (BooleanUtils.isTrue(pickleballMatchDetailRequestDto.getIsTied())) {
                playDetail.setIsTied(true);
                playDetail.setWinningPlayerUserProfile(null);
                playDetail.setWinningGuestPlayerName(null);
                playDetail.setWinningTeam(null);
            } else {
                throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Winning player is required");
            }
        } else if (match.getGameFormat() == GameFormat.DOUBLES) {
            if (StringUtils.isNotEmpty(pickleballMatchDetailRequestDto.getWinningTeamId())) {
                playDetail.setWinningTeam(
                        PickleballTeam.builder().id(pickleballMatchDetailRequestDto.getWinningTeamId()).build());
                playDetail.setWinningPlayerUserProfile(null);
                playDetail.setWinningGuestPlayerName(null);
                playDetail.setIsTied(false);
            } else if (BooleanUtils.isTrue(pickleballMatchDetailRequestDto.getIsTied())) {
                playDetail.setIsTied(true);
                playDetail.setWinningTeam(null);
                playDetail.setWinningPlayerUserProfile(null);
                playDetail.setWinningGuestPlayerName(null);
            }
        } else {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Unsupported game format");
        }

        // Defensive init
        if (playDetail.getPickleballMatchRounds() == null) {
            playDetail.setPickleballMatchRounds(new ArrayList<>());
        }

        // Map of existing rounds
        Map<Integer, PickleballMatchRound> existingRoundsMap = playDetail.getPickleballMatchRounds()
                .stream()
                .collect(Collectors.toMap(PickleballMatchRound::getRoundNumber, r -> r));

        // Validate request rounds
        List<PickleballMatchRoundDetailsRequestDto> incomingRounds = pickleballMatchDetailRequestDto.getRounds() == null
                ? Collections.emptyList()
                : pickleballMatchDetailRequestDto.getRounds();

        Set<Integer> seenRoundNumbers = new HashSet<>();
        for (PickleballMatchRoundDetailsRequestDto rDto : incomingRounds) {
            if (rDto == null)
                continue;
            if (!seenRoundNumbers.add(rDto.getRound())) {
                throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                        "Duplicate round number in request: " + rDto.getRound());
            }
        }

        // Process incoming rounds
        for (PickleballMatchRoundDetailsRequestDto roundDto : incomingRounds) {
            if (roundDto == null)
                continue;

            int roundNumber = roundDto.getRound();
            PickleballMatchRound round = existingRoundsMap.get(roundNumber);
            boolean isNewRound = (round == null);

            if (isNewRound) {
                round = new PickleballMatchRound();
                round.setRoundNumber(roundNumber);
                round.setPickleballMatchPlayDetail(playDetail);
                round.setPickleballMatchRoundsPlayDetails(new ArrayList<>());
            } else {
                // Replace scores if round already exists
                if (round.getPickleballMatchRoundsPlayDetails() != null) {
                    round.getPickleballMatchRoundsPlayDetails().clear();
                } else {
                    round.setPickleballMatchRoundsPlayDetails(new ArrayList<>());
                }
            }

            // Round times
            round.setRoundStartTime(StringUtils.isNotEmpty(roundDto.getRoundStartTime())
                    ? Timestamp.from(Instant.parse(roundDto.getRoundStartTime()))
                    : null);
            round.setRoundEndTime(StringUtils.isNotEmpty(roundDto.getRoundEndTime())
                    ? Timestamp.from(Instant.parse(roundDto.getRoundEndTime()))
                    : null);

            // Winner for round
            if (match.getGameFormat() == GameFormat.SINGLES) {
                if (StringUtils.isNotEmpty(roundDto.getWinningPlayerUserId())) {
                    round.setWinningPlayerUserProfile(
                            UserProfile.builder().id(roundDto.getWinningPlayerUserId()).build());
                    round.setWinningGuestPlayerName(null);
                    round.setWinningTeam(null);
                } else if (StringUtils.isNotEmpty(roundDto.getWinningGuestPlayerName())) {
                    round.setWinningGuestPlayerName(roundDto.getWinningGuestPlayerName());
                    round.setWinningPlayerUserProfile(null);
                    round.setWinningTeam(null);
                } else {
                    throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                            "Winning player is required for round " + roundNumber);
                }
            } else if (match.getGameFormat() == GameFormat.DOUBLES) {
                if (StringUtils.isNotEmpty(roundDto.getWinningTeamId())) {
                    round.setWinningTeam(PickleballTeam.builder().id(roundDto.getWinningTeamId()).build());
                    round.setWinningPlayerUserProfile(null);
                    round.setWinningGuestPlayerName(null);
                } else {
                    throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                            "Winning team is required for round " + roundNumber);
                }
            }

            // Player scores
            List<PickleballPlayerScoreRequestDto> scoreDtos = roundDto.getPlayerScores() == null
                    ? Collections.emptyList()
                    : roundDto.getPlayerScores();
            for (PickleballPlayerScoreRequestDto scoreDto : scoreDtos) {
                PickleballMatchRoundsPlayDetail score = new PickleballMatchRoundsPlayDetail();
                score.setPickleballMatchRound(round);
                score.setScore(scoreDto.getScore());
                score.setGuestPlayerName(scoreDto.getGuestPlayerName());

                if (StringUtils.isNotEmpty(scoreDto.getPlayerUserId())) {
                    score.setPlayerUserProfile(UserProfile.builder().id(scoreDto.getPlayerUserId()).build());
                }
                if (StringUtils.isNotEmpty(scoreDto.getTeamId())) {
                    score.setTeam(PickleballTeam.builder().id(scoreDto.getTeamId()).build());
                }
                round.getPickleballMatchRoundsPlayDetails().add(score);
            }

            if (isNewRound) {
                playDetail.getPickleballMatchRounds().add(round);
                existingRoundsMap.put(roundNumber, round);
            }
        }

        pickleballMatchPlayDetailRepo.save(playDetail);

        return toPickleballMatchDetailDto(
                pickleballMatchPlayDetailRepo.findByPickleballMatch_Id(matchId).get());
    }

    @Override
    public PickleballMatchDetailDto getMatchDetails(String matchId, Sports sport) throws ResourceException {
        PickleballMatch badmintonMatch = pickleballMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));

        if (badmintonMatch.getMatchStatus() != MatchStatus.ENDED) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Match is not ended");
        }

        PickleballMatchPlayDetail badmintonMatchPlayDetail = pickleballMatchPlayDetailRepo
                .findByPickleballMatch_Id(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match details not found"));

        return toPickleballMatchDetailDto(badmintonMatchPlayDetail);
    }

    // @Override
    // public void submitScore(String matchId, PickleballScoreRequestDto
    // pickleballScoreRequestDto, Sports sport) throws ResourceException {
    //
    // BadmintonScoreNotificationDto picklebadmintScoreNotificationDto = new
    // BadmintonScoreNotificationDto();
    // picklebadmintScoreNotificationDto.setMatchId(matchId);
    // picklebadmintScoreNotificationDto.setComment(pickleballScoreRequestDto.getComment());
    // picklebadmintScoreNotificationDto.setPoints(pickleballScoreRequestDto.getPoints());
    // picklebadmintScoreNotificationDto.setTeamId(pickleballScoreRequestDto.getTeamId());
    //
    // if (StringUtils.isNotEmpty(pickleballScoreRequestDto.getGuestPlayerName()) &&
    // StringUtils.isEmpty(pickleballScoreRequestDto.getPlayerUserId())) {
    // throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Player is
    // required");
    // }
    //
    // BadmintonScoreNotificationDto badmintonScoreNotificationDto = new
    // BadmintonScoreNotificationDto();
    // badmintonScoreNotificationDto.setMatchId(matchId);
    // badmintonScoreNotificationDto.setComment(pickleballScoreRequestDto.getComment());
    // badmintonScoreNotificationDto.setPoints(pickleballScoreRequestDto.getPoints());
    // badmintonScoreNotificationDto.setTeamId(pickleballScoreRequestDto.getTeamId());
    //
    // if (StringUtils.isNotEmpty(pickleballScoreRequestDto.getPlayerUserId())) {
    // UserProfileDto userProfileDto =
    // userProfileRepo.findById(pickleballScoreRequestDto.getPlayerUserId())
    // .map(userProfile -> modelMapper.map(userProfile,
    // UserProfileDto.class)).orElse(null);
    // if (userProfileDto == null) {
    // throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
    // }
    // badmintonScoreNotificationDto
    // .setPlayerUserProfile(modelMapper.map(userProfileDto,
    // UserProfileMinDto.class));
    // } else {
    // badmintonScoreNotificationDto.setGuestPlayerName(pickleballScoreRequestDto.getGuestPlayerName());
    //
    // String message = AppConstants.GSON.toJson(badmintonScoreNotificationDto);
    //
    // Long clientsCount =
    // redisTemplate.convertAndSend(scoreUpdatesTopic.getTopic(), message);
    //
    // log.info("clientsCount: {}", clientsCount);
    // log.info("Publishes score update for match {} to redis", matchId);
    // // TODO: pending
    //
    //
    // }
    // }

    @Transactional
    @Override
    public void deleteAllAutogeneratedMatchesByTournament(String academyId, String tournamentId, Sports sport)
            throws ResourceException {
        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }

        List<PickleballMatch> matches = pickleballMatchRepo.findByMatchStatusInAndTournament_Id(
                List.of(MatchStatus.ENDED, MatchStatus.ABANDONED, MatchStatus.IN_PROGRESS), tournamentId);

        if (CollectionUtils.isEmpty(matches)) {
            pickleballMatchRepo.deleteByAutogenMatchesByTournament(tournamentId);
        } else {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Matches are in progress or ended");
        }
    }

    @Override
    public void updateRealTimeScore(String matchId, RealTimeScoreUpdateRequestDto requestDto) throws ResourceException {

    }

    @Override
    public void finalizeMatchScores(String matchId, PickleballMatchDetailRequestDto finalScores)
            throws ResourceException {

    }

    @Override
    public List<BadmintonScoreTrendDto> getScoreTrend(String matchId, Long roundNumber) throws ResourceException {
        List<BadmintonScoreTrendDto> scoreTrends = new ArrayList<>();

        if (roundNumber == null) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Round number not found");
        }
        PickleballMatch match = pickleballMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));

        List<BadmintonLiveScore> liveScores = liveScoreRepo.findByMatchId(matchId);
        if (CollectionUtils.isEmpty(liveScores)) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Live score not found");
        }

        liveScores = liveScores.stream()
                .filter(live -> live.getRoundNumber() == roundNumber)
                .collect(Collectors.toList());

        // Sort live scores by score time
        liveScores.sort(Comparator.comparing(BadmintonLiveScore::getScoreTime));

        // To be used later for ID to name mapping
        List<String> playerOrTeamNames = new ArrayList<>();

        if (match.getGameFormat() == GameFormat.SINGLES) {
            List<PickleballSinglesPlayerMapping> singlePlayers = match.getSinglePlayers();

            if (StringUtils.isEmpty(singlePlayers.get(0).getGuestName())) {
                playerOrTeamNames.add(singlePlayers.get(0).getPlayerUserProfile().getId() + "#"
                        + singlePlayers.get(0).getPlayerUserProfile().getDisplayName());
            } else {
                playerOrTeamNames.add("GUEST#" + singlePlayers.get(0).getGuestName());
            }

            if (StringUtils.isEmpty(singlePlayers.get(1).getGuestName())) {
                playerOrTeamNames.add(singlePlayers.get(1).getPlayerUserProfile().getId() + "#"
                        + singlePlayers.get(1).getPlayerUserProfile().getDisplayName());
            } else {
                playerOrTeamNames.add("GUEST#" + singlePlayers.get(1).getGuestName());
            }
        } else {
            List<PickleballTeamMapping> teamMappings = match.getTeams();

            playerOrTeamNames
                    .add(teamMappings.get(0).getTeam().getId() + "#" + teamMappings.get(0).getTeam().getTeamName());
            playerOrTeamNames
                    .add(teamMappings.get(1).getTeam().getId() + "#" + teamMappings.get(1).getTeam().getTeamName());
        }

        for (BadmintonLiveScore score : liveScores) {
            scoreTrends = updateScoreTrend(scoreTrends, playerOrTeamNames, score, match.getGameFormat());
        }

        return scoreTrends;
    }

    private List<BadmintonScoreTrendDto> updateScoreTrend(List<BadmintonScoreTrendDto> scoreTrends,
            List<String> playerOrTeamNames, BadmintonLiveScore score, GameFormat gameFormat) {
        String scoringPlayerOrTeam = null, opponentPlayerOrTeam = null;

        if (gameFormat == GameFormat.SINGLES) {
            if (!StringUtils.isEmpty(score.getGuestPlayerName())) {
                if (playerOrTeamNames.get(0).startsWith("GUEST")
                        && playerOrTeamNames.get(0).equalsIgnoreCase("GUEST#" + score.getGuestPlayerName())) {
                    scoringPlayerOrTeam = playerOrTeamNames.get(0).split("#")[1];
                    opponentPlayerOrTeam = playerOrTeamNames.get(1).split("#")[1];
                } else {
                    /**
                     * Assumes if first player is not scorer than it's going to be opponent and
                     * other player will be scorer
                     */
                    scoringPlayerOrTeam = playerOrTeamNames.get(1).split("#")[1];
                    opponentPlayerOrTeam = playerOrTeamNames.get(0).split("#")[1];
                }
            } else if (!StringUtils.isEmpty(score.getPlayerId())) {
                if (playerOrTeamNames.get(0).startsWith(score.getPlayerId())) {
                    scoringPlayerOrTeam = playerOrTeamNames.get(0).split("#")[1];
                    opponentPlayerOrTeam = playerOrTeamNames.get(1).split("#")[1];
                } else {
                    scoringPlayerOrTeam = playerOrTeamNames.get(1).split("#")[1];
                    opponentPlayerOrTeam = playerOrTeamNames.get(0).split("#")[1];
                }
            }
        } else if (gameFormat == GameFormat.DOUBLES) {
            if (playerOrTeamNames.get(0).startsWith(score.getTeamId())) {
                scoringPlayerOrTeam = playerOrTeamNames.get(0).split("#")[1];
                opponentPlayerOrTeam = playerOrTeamNames.get(1).split("#")[1];
            } else {
                scoringPlayerOrTeam = playerOrTeamNames.get(1).split("#")[1];
                opponentPlayerOrTeam = playerOrTeamNames.get(0).split("#")[1];
            }
        }

        if (StringUtils.isEmpty(scoringPlayerOrTeam) || StringUtils.isEmpty(opponentPlayerOrTeam)) {
            log.info("Scoring player or team or opponent player or team is empty. THIS SHOULD NEVER HAPPEN");
            return scoreTrends;
        }

        if (CollectionUtils.isEmpty(scoreTrends)) {
            scoreTrends = new ArrayList<>();
            BadmintonScoreTrendDto scoreTrend = new BadmintonScoreTrendDto();
            scoreTrend.setScoreTime(score.getScoreTime());
            scoreTrend.setScoreTrend(new HashMap<>());
            scoreTrend.getScoreTrend().put(scoringPlayerOrTeam, score.getScore());
            scoreTrend.getScoreTrend().put(opponentPlayerOrTeam, 0L);
            scoreTrends.add(scoreTrend);
        } else {
            long cummulativeScore = score.getScore()
                    + scoreTrends.get(scoreTrends.size() - 1).getScoreTrend().get(scoringPlayerOrTeam);
            BadmintonScoreTrendDto scoreTrend = new BadmintonScoreTrendDto();
            scoreTrend.setScoreTrend(new HashMap<>());
            scoreTrend.setScoreTime(score.getScoreTime());
            scoreTrend.getScoreTrend().put(scoringPlayerOrTeam, cummulativeScore);
            scoreTrend.getScoreTrend().put(opponentPlayerOrTeam,
                    scoreTrends.get(scoreTrends.size() - 1).getScoreTrend().get(opponentPlayerOrTeam));
            scoreTrends.add(scoreTrend);
        }

        return scoreTrends;
    }

    @Override
    public Map<String, Map<String, Long>> getScoreByShot(String matchId, Long roundNumber) throws ResourceException {
        Map<String, Map<String, Long>> scoreByShots = new HashMap<>();

        if (roundNumber == null) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Round number not found");
        }

        PickleballMatch match = pickleballMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));

        List<String> allPlayerIds = new ArrayList<>();
        List<String> allTeamIds = new ArrayList<>();
        List<String> allGuests = new ArrayList<>();

        if (match.getGameFormat() == GameFormat.DOUBLES) {
            if (!CollectionUtils.isEmpty(match.getTeams())) {
                allTeamIds = match.getTeams().stream()
                        .map(teamMapping -> teamMapping.getTeam().getId())
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
            }
        } else {
            if (!CollectionUtils.isEmpty(match.getSinglePlayers())) {
                List<String> singlePlayerIds = match.getSinglePlayers().stream()
                        .map(playerMapping -> playerMapping.getPlayerUserProfile() != null
                                ? playerMapping.getPlayerUserProfile().getId()
                                : null)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                allPlayerIds.addAll(singlePlayerIds);
            }

            List<String> guestPlayersName = match.getSinglePlayers().stream()
                    .map(PickleballSinglesPlayerMapping::getGuestName).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(guestPlayersName)) {
                allGuests = guestPlayersName.stream().distinct().collect(Collectors.toList());
            }

            if (!CollectionUtils.isEmpty(match.getPickleballMatchTeamPlayers())) {
                List<String> teamPlayerIds = match.getPickleballMatchTeamPlayers().stream()
                        .map(teamPlayerMapping -> teamPlayerMapping.getPlayerUserProfile() != null
                                ? teamPlayerMapping.getPlayerUserProfile().getId()
                                : null)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                allPlayerIds.addAll(teamPlayerIds);
            }

            allPlayerIds = allPlayerIds.stream().distinct().toList();
        }

        Map<String, UserProfile> userProfilesById = new HashMap<>();

        if (!CollectionUtils.isEmpty(allPlayerIds)) {
            userProfilesById = userProfileRepo.findByIdIn(allPlayerIds)
                    .stream().collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        }

        Map<String, PickleballTeam> teamsById = new HashMap<>();
        if (!CollectionUtils.isEmpty(allTeamIds)) {
            teamsById = pickleballTeamRepo.findByIdIn(allTeamIds)
                    .stream().collect(Collectors.toMap(PickleballTeam::getId, Function.identity()));
        }

        List<BadmintonLiveScore> liveScores = liveScoreRepo.findByMatchId(matchId);
        if (CollectionUtils.isEmpty(liveScores)) {
            return scoreByShots;
        }

        // Filter by round number
        liveScores = liveScores.stream()
                .filter(live -> live.getRoundNumber() != null)
                .filter(live -> live.getRoundNumber().equals(roundNumber))
                .toList();

        // Include all relevant participants (users/teams/guests) with empty maps
        // initially
        if (match.getGameFormat() == GameFormat.DOUBLES) {
            teamsById.values().forEach(team -> scoreByShots.putIfAbsent(team.getTeamName(), new HashMap<>()));
        } else {
            userProfilesById.values()
                    .forEach(profile -> scoreByShots.putIfAbsent(profile.getDisplayName(), new HashMap<>()));

            // Add guest players from live scores
            allGuests.forEach(guestName -> scoreByShots.putIfAbsent(guestName, new HashMap<>()));
        }

        for (BadmintonLiveScore score : liveScores) {
            String shotType = StringUtils.isNotEmpty(score.getShotType()) ? score.getShotType() : "-";

            if (match.getGameFormat() == GameFormat.DOUBLES) {
                if (StringUtils.isNotEmpty(score.getTeamId())) {
                    PickleballTeam team = teamsById.get(score.getTeamId());
                    if (team != null) {
                        String teamName = team.getTeamName();
                        scoreByShots.get(teamName).merge(shotType, 1L, Long::sum);
                    }
                }
            } else {
                if (StringUtils.isNotEmpty(score.getPlayerId())) {
                    UserProfile player = userProfilesById.get(score.getPlayerId());
                    if (player != null) {
                        String playerName = player.getDisplayName();
                        scoreByShots.get(playerName).merge(shotType, 1L, Long::sum);
                    }
                } else if (StringUtils.isNotEmpty(score.getGuestPlayerName())) {
                    String guestName = score.getGuestPlayerName();
                    scoreByShots.get(guestName).merge(shotType, 1L, Long::sum);
                }
            }
        }

        return scoreByShots;
    }

    private List<PickleballTeamMapping> buildPickleballTeamMapping(String matchId, List<String> teamIds) {
        return teamIds.stream()
                .map(teamId -> {
                    PickleballTeamMapping pickleballTeamMapping = new PickleballTeamMapping();
                    pickleballTeamMapping.setTeam(PickleballTeam.builder().id(teamId).build());
                    pickleballTeamMapping.setPickleballMatch(PickleballMatch.builder().id(matchId).build());
                    return pickleballTeamMapping;
                })
                .toList();
    }

    private List<PickleballSinglesPlayerMapping> buildPickleballSinglesPlayerMapping(String matchId,
            List<PickleBallTeamPlayerDto> teamPlayersDtos) {
        return teamPlayersDtos.stream()
                .map(teamPlayerDto -> {
                    PickleballSinglesPlayerMapping pickleballSinglesPlayerMapping = new PickleballSinglesPlayerMapping();
                    pickleballSinglesPlayerMapping.setPlayerUserProfile(
                            StringUtils.isEmpty(teamPlayerDto.getPlayerUserId()) ? null
                                    : UserProfile.builder().id(teamPlayerDto.getPlayerUserId()).build());
                    pickleballSinglesPlayerMapping.setPickleballMatch(PickleballMatch.builder().id(matchId).build());
                    pickleballSinglesPlayerMapping.setGuestName(teamPlayerDto.getGuestPlayerName());
                    return pickleballSinglesPlayerMapping;
                })
                .toList();
    }

    private List<PickleballMatchTeamPlayerMapping> buildPickleballTeamMatchPlayerMapping(String matchId,
            List<PickleballTeamDto> teams) {
        if (teams == null || teams.isEmpty())
            return Collections.emptyList();

        List<PickleballMatchTeamPlayerMapping> mappings = new ArrayList<>();
        PickleballMatch matchRef = PickleballMatch.builder().id(matchId).build();

        for (PickleballTeamDto teamDto : teams) {
            PickleballTeam teamRef = PickleballTeam.builder().id(teamDto.getId()).build();

            // Registered players
            if (teamDto.getPlayers() != null) {
                for (PickleBallTeamPlayerDto playerDto : teamDto.getPlayers()) {
                    if (playerDto == null || playerDto.getPlayerUserId() == null
                            || playerDto.getPlayerUserId().isBlank()) {
                        continue;
                    }
                    mappings.add(PickleballMatchTeamPlayerMapping.builder()
                            .pickleballMatch(matchRef)
                            .team(teamRef)
                            .playerUserProfile(UserProfile.builder().id(playerDto.getPlayerUserId()).build())
                            .build());
                }
            }

            // Guest players
            if (teamDto.getPlayers() != null) {
                for (PickleBallTeamPlayerDto guestDto : teamDto.getPlayers()) {
                    if (guestDto == null || guestDto.getGuestPlayerName() == null
                            || guestDto.getGuestPlayerName().isBlank()) {
                        continue;
                    }

                    mappings.add(PickleballMatchTeamPlayerMapping.builder()
                            .pickleballMatch(matchRef)
                            .team(teamRef)
                            .guestName(guestDto.getGuestPlayerName())
                            .build());
                }
            }
        }
        return mappings;
    }

    private PickleballMatchDetailDto toPickleballMatchDetailDto(PickleballMatchPlayDetail badmintonMatchPlayDetail) {
        return PickleballMatchDetailDto.builder()
                .rounds(badmintonMatchPlayDetail.getPickleballMatchRounds().stream()
                        .map(this::toPickleballMatchRoundDetailsDto)
                        .sorted(Comparator.comparing(PickleballMatchRoundDetailsDto::getRound))
                        .toList())
                .winningPlayerUserProfile(badmintonMatchPlayDetail.getWinningPlayerUserProfile() == null ? null
                        : modelMapper.map(badmintonMatchPlayDetail.getWinningPlayerUserProfile(),
                                UserProfileMinDto.class))
                .winningGuestPlayerName(badmintonMatchPlayDetail.getWinningGuestPlayerName())
                .winningTeam(badmintonMatchPlayDetail.getWinningTeam() == null ? null
                        : pickleballTeamService.convertToDto(badmintonMatchPlayDetail.getWinningTeam()))
                .isTied(badmintonMatchPlayDetail.getIsTied())
                .build();
    }

    private PickleballMatchRoundDetailsDto toPickleballMatchRoundDetailsDto(PickleballMatchRound badmintonMatchRound) {
        Set<String> seenTeams = new HashSet<>();

        List<PickleballPlayerScoreDto> playerScores = badmintonMatchRound.getPickleballMatchRoundsPlayDetails().stream()
                .map(detail -> toPickleballPlayerScoreDto(detail))
                .filter(dto -> {
                    // Only include the first player from each team to avoid duplicate scores
                    if (dto.getTeam() != null && dto.getTeam().getId() != null) {
                        String teamId = dto.getTeam().getId();
                        return seenTeams.add(teamId); // Returns true only for first occurrence
                    }
                    return true; // Include players without teams
                })
                .toList();

        return PickleballMatchRoundDetailsDto.builder()
                .round(badmintonMatchRound.getRoundNumber())
                .roundStartTime(badmintonMatchRound.getRoundStartTime().toString())
                .roundEndTime(badmintonMatchRound.getRoundEndTime().toString())
                .playerScores(playerScores)
                .winningPlayerUserProfile(badmintonMatchRound.getWinningPlayerUserProfile() == null ? null
                        : modelMapper.map(badmintonMatchRound.getWinningPlayerUserProfile(), UserProfileMinDto.class))
                .winningGuestPlayerName(badmintonMatchRound.getWinningGuestPlayerName())
                .winningTeam(badmintonMatchRound.getWinningTeam() == null ? null
                        : pickleballTeamService.convertToDto(badmintonMatchRound.getWinningTeam()))
                .build();
    }

    private PickleballPlayerScoreDto toPickleballPlayerScoreDto(
            PickleballMatchRoundsPlayDetail badmintonMatchRoundsPlayDetail) {
        return PickleballPlayerScoreDto.builder().score(badmintonMatchRoundsPlayDetail.getScore())
                .playerUserProfile(badmintonMatchRoundsPlayDetail.getPlayerUserProfile() == null ? null
                        : modelMapper.map(badmintonMatchRoundsPlayDetail.getPlayerUserProfile(),
                                UserProfileMinDto.class))
                .team(badmintonMatchRoundsPlayDetail.getTeam() == null ? null
                        : pickleballTeamService.convertToDto(badmintonMatchRoundsPlayDetail.getTeam()))
                .guestPlayerName(badmintonMatchRoundsPlayDetail.getGuestPlayerName())
                .roundNumber(badmintonMatchRoundsPlayDetail.getPickleballMatchRound().getRoundNumber()).build();
    }

    private List<PickleballMatch> sort(List<PickleballMatch> badmintonMatches) {
        return badmintonMatches.stream()
                .sorted(Comparator.comparing(PickleballMatch::getCreatedAtTimestampUtc).reversed()).toList();
    }

    private void sendNotificationToScorer(List<UserProfileDto> scorerUserProfiles, String matchId, String academyId) {
        if (CollectionUtils.isEmpty(scorerUserProfiles)) {
            return;
        }
        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("matchId", matchId);
        extraParams.put("isAssociateWithAcademy", String.valueOf(StringUtils.isNotEmpty(academyId)));
        extraParams.put("academyId", academyId);
        List<String> sentUserIds = new ArrayList<>();
        for (UserProfileDto userProfileDto : scorerUserProfiles) {
            if (StringUtils.isEmpty(userProfileDto.getAndroidFcmPushToken())) {
                continue;
            }
            pushNotificationService.sendMessageToPushToken(userProfileDto.getAndroidFcmPushToken(),
                    NotificationType.LIVE_NOTIFICATION, "You are added as scorer",
                    "You have been assigned as scorer for a match.", "MATCH_DETAILS", CtaType.SCREEN, extraParams);
            sentUserIds.add(userProfileDto.getId());
        }
        pushNotificationService.addNotification(sentUserIds, "You are added as scorer", CtaType.SCREEN, "MATCH_DETAILS",
                extraParams);
    }

    @Override
    public Pair<Long, Long> getMatchCounts(String tournamentId) throws ResourceException {
        var tuple = pickleballMatchRepo.getMatchesCount(tournamentId).get(0);
        return Pair.of(Long.valueOf(tuple.get("total_matches_played") + ""),
                Long.valueOf(tuple.get("total_matches_completed") + ""));
    }

    @Override
    public List<PlayerPerformanceDto> getGuestPlayerPerformances(String tournamentId) throws ResourceException {
        var guestPerformances = pickleballSinglesPlayerMappingRepo.getGuestPlayerPerformance(tournamentId, 10);
        return guestPerformances.stream()
                .map(columns -> {
                    var playerPerformanceDto = new PlayerPerformanceDto();
                    playerPerformanceDto.setPlayerId(null);
                    playerPerformanceDto.setPlayerName((String) columns[0]);
                    var totalPlayed = ((Number) columns[1]).intValue();
                    var matchesWon = ((Number) columns[2]).intValue();
                    var matchesLost = ((Number) columns[3]).intValue();
                    double winPercentage = Math.round((100.0 * matchesWon / totalPlayed) * 100.0) / 100.0;
                    double lossPercentage = Math.round((100.0 * matchesLost / totalPlayed) * 100.0) / 100.0;

                    playerPerformanceDto.setMatchesPlayed(totalPlayed);
                    playerPerformanceDto.setMatchesWon(matchesWon);
                    playerPerformanceDto.setMatchesLost(matchesLost);
                    playerPerformanceDto.setMatchesTied(((Number) columns[4]).intValue());
                    playerPerformanceDto.setWinPercentage(winPercentage);
                    playerPerformanceDto.setLossPercentage(lossPercentage);
                    // playerPerformanceDto.setTotalPoints(((Number) columns[5]).intValue());
                    // TODO: For Smash Premier League, calculating matchesWon * 2
                    playerPerformanceDto.setTotalPoints(matchesWon >= 0 ? matchesWon * 2 : 0);
                    return playerPerformanceDto;
                })
                .toList();
    }

    @Override
    public List<PlayerPerformanceDto> getPlayerPerformances(String tournamentId) throws ResourceException {
        return pickleballSinglesPlayerMappingRepo.getPlayerPerformance(tournamentId, 10).stream()
                .map(columns -> {
                    var playerPerformanceDto = new PlayerPerformanceDto();
                    playerPerformanceDto.setPlayerId((String) columns[0]);
                    playerPerformanceDto.setPlayerName((String) columns[1]);
                    var totalPlayed = ((Number) columns[2]).intValue();
                    var matchesWon = ((Number) columns[3]).intValue();
                    var matchesLost = ((Number) columns[4]).intValue();
                    double winPercentage = Math.round((100.0 * matchesWon / totalPlayed) * 100.0) / 100.0;
                    double lossPercentage = Math.round((100.0 * matchesLost / totalPlayed) * 100.0) / 100.0;

                    playerPerformanceDto.setMatchesPlayed(totalPlayed);
                    playerPerformanceDto.setMatchesWon(matchesWon);
                    playerPerformanceDto.setMatchesLost(matchesLost);
                    playerPerformanceDto.setMatchesTied(((Number) columns[5]).intValue());
                    playerPerformanceDto.setWinPercentage(winPercentage);
                    playerPerformanceDto.setLossPercentage(lossPercentage);
                    // playerPerformanceDto.setTotalPoints(((Number) columns[6]).intValue());
                    // TODO: For Smash Premier League, calculating matchesWon * 2
                    playerPerformanceDto.setTotalPoints(matchesWon >= 0 ? matchesWon * 2 : 0);
                    return playerPerformanceDto;
                })
                .toList();
    }

    private UserProfile getUserProfileWithFallback(UserProfile tournamentProfile, String requestUserId) {
        if (StringUtils.isEmpty(requestUserId)) {
            return tournamentProfile != null ? UserProfile.builder().id(tournamentProfile.getId()).build() : null;
        }

        // Always prefer requestUserId when it's provided
        return UserProfile.builder().id(requestUserId).build();
    }

}
