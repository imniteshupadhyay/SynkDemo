package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import com.playmotech.api.core.agora.RtcTokenProvider;
import com.playmotech.api.core.constants.AppConstants;
import com.playmotech.api.core.constants.BadmintonEventType;
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
import com.playmotech.api.core.dao_postgres.BadmintonMatch;
import com.playmotech.api.core.dao_postgres.BadmintonMatchPlayDetail;
import com.playmotech.api.core.dao_postgres.BadmintonMatchRound;
import com.playmotech.api.core.dao_postgres.BadmintonMatchRoundsPlayDetail;
import com.playmotech.api.core.dao_postgres.BadmintonMatchTeamPlayerMapping;
import com.playmotech.api.core.dao_postgres.BadmintonSinglesPlayerMapping;
import com.playmotech.api.core.dao_postgres.BadmintonTeamMapping;
import com.playmotech.api.core.dao_postgres.BadmintonTournament;
import com.playmotech.api.core.dao_postgres.Court;
import com.playmotech.api.core.dao_postgres.Team;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.BadmintonMatchContext;
import com.playmotech.api.core.dto.BadmintonMatchDetailDto;
import com.playmotech.api.core.dto.BadmintonMatchDetailRequestDto;
import com.playmotech.api.core.dto.BadmintonMatchDto;
import com.playmotech.api.core.dto.BadmintonMatchRoundDetailsDto;
import com.playmotech.api.core.dto.BadmintonMatchRoundDetailsRequestDto;
import com.playmotech.api.core.dto.BadmintonPlayerScoreDto;
import com.playmotech.api.core.dto.BadmintonPlayerScoreRequestDto;
import com.playmotech.api.core.dto.BadmintonScoreNotificationDto;
import com.playmotech.api.core.dto.BadmintonScoreTrendDto;
import com.playmotech.api.core.dto.CourtDto;
import com.playmotech.api.core.dto.CreateBadmintonMatchDto;
import com.playmotech.api.core.dto.CreatePickleballMatchDto;
import com.playmotech.api.core.dto.PaginatedResponse;
import com.playmotech.api.core.dto.PickleballMatchDetailDto;
import com.playmotech.api.core.dto.PickleballMatchDetailRequestDto;
import com.playmotech.api.core.dto.PickleballMatchDto;
import com.playmotech.api.core.dto.PlayerPerformanceDto;
import com.playmotech.api.core.dto.PlayerScoreUpdateDto;
import com.playmotech.api.core.dto.RealTimeScoreUpdateRequestDto;
import com.playmotech.api.core.dto.TeamDto;
import com.playmotech.api.core.dto.TeamMatchPlayerDto;
import com.playmotech.api.core.dto.TeamPlayerDto;
import com.playmotech.api.core.dto.UpdateBadmintonMatchDto;
import com.playmotech.api.core.dto.UpdatePickleballMatchDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.BadmintonLiveScoreRepository;
import com.playmotech.api.core.repo.BadmintonMatchPlayDetailRepo;
import com.playmotech.api.core.repo.BadmintonMatchRepo;
import com.playmotech.api.core.repo.BadmintonMatchTeamPlayerMappingRepo;
import com.playmotech.api.core.repo.BadmintonSinglesPlayerMappingRepo;
import com.playmotech.api.core.repo.BadmintonTeamMappingRepo;
import com.playmotech.api.core.repo.BadmintonTournamentRepo;
import com.playmotech.api.core.repo.TeamRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.services.CommentaryService;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IBadmintonMatchService;
import com.playmotech.api.core.services.ICoachService;
import com.playmotech.api.core.services.IPickleballMatchService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.ITeamsService;
import com.playmotech.api.core.specification.BadmintonMatchSpecification;
import com.playmotech.api.core.utils.EntityToDtoUtils;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.websockets.SubscribeMatchUpdatesWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BadmintonMatchService implements IBadmintonMatchService {

    private final static Integer NO_UID = 0;

    private final ModelMapper modelMapper = new ModelMapper();
    private final BadmintonMatchRepo badmintonMatchRepo;
    private final IAcademyService academyService;
    private final BadmintonTeamMappingRepo badmintonTeamMappingRepo;
    private final BadmintonSinglesPlayerMappingRepo badmintonSinglesPlayerMappingRepo;
    private final RtcTokenProvider rtcTokenProvider;
    private final BadmintonMatchPlayDetailRepo badmintonMatchPlayDetailRepo;
    private final ICoachService coachService;
    private final UserProfileRepo userProfileRepo;
    private final TeamRepo teamRepo;
    private final ITeamsService teamsService;
    private final Map<String, List<WebSocketSession>> badmintonMatchSubscribedSessions;
    private final IPushNotificationService pushNotificationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic scoreUpdatesTopic;
    private final BadmintonLiveScoreRepository liveScoreRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final SubscribeMatchUpdatesWebSocketHandler webSocketHandler;
    private final CommentaryService commentaryService;
    private final WebSocketBrokerService webSocketBrokerService;
    private final BadmintonMatchTeamPlayerMappingRepo badmintonMatchTeamPlayerMappingRepo;
    private final BadmintonTournamentRepo badmintonTournamentRepo;
    private final IPickleballMatchService pickleballMatchService;
    private final EntityToDtoUtils entityToDtoUtilsConverter;

    public BadmintonMatchService(final BadmintonMatchRepo badmintonMatchRepo, final IAcademyService academyService,
            final BadmintonTeamMappingRepo badmintonTeamMappingRepo,
            final BadmintonSinglesPlayerMappingRepo badmintonSinglesPlayerMappingRepo,
            final RtcTokenProvider rtcTokenProvider, final BadmintonMatchPlayDetailRepo badmintonMatchPlayDetailRepo,
            final ICoachService coachService, final UserProfileRepo userProfileRepo, final TeamRepo teamRepo,
            final ITeamsService teamsService,
            final Map<String, List<WebSocketSession>> badmintonMatchSubscribedSessions,
            final IPushNotificationService pushNotificationService, final RedisTemplate<String, Object> redisTemplate,
            final ChannelTopic scoreUpdatesTopic, final BadmintonLiveScoreRepository liveScoreRepo,
            final SimpMessagingTemplate messagingTemplate, final SubscribeMatchUpdatesWebSocketHandler webSocketHandler,
            final CommentaryService commentaryService, final WebSocketBrokerService webSocketBrokerService,
            final BadmintonMatchTeamPlayerMappingRepo badmintonMatchTeamPlayerMappingRepo,
            final BadmintonTournamentRepo badmintonTournamentRepo, IPickleballMatchService pickleballMatchService,
            EntityToDtoUtils entityToDtoUtilsConverter) {
        this.badmintonMatchRepo = badmintonMatchRepo;
        this.academyService = academyService;
        this.badmintonTeamMappingRepo = badmintonTeamMappingRepo;
        this.badmintonSinglesPlayerMappingRepo = badmintonSinglesPlayerMappingRepo;
        this.pickleballMatchService = pickleballMatchService;
        this.entityToDtoUtilsConverter = entityToDtoUtilsConverter;
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        this.rtcTokenProvider = rtcTokenProvider;
        this.badmintonMatchPlayDetailRepo = badmintonMatchPlayDetailRepo;
        this.coachService = coachService;
        this.userProfileRepo = userProfileRepo;
        this.teamRepo = teamRepo;
        this.teamsService = teamsService;
        this.badmintonMatchSubscribedSessions = badmintonMatchSubscribedSessions;
        this.pushNotificationService = pushNotificationService;
        this.redisTemplate = redisTemplate;
        this.scoreUpdatesTopic = scoreUpdatesTopic;
        this.liveScoreRepo = liveScoreRepo;
        this.messagingTemplate = messagingTemplate;
        this.webSocketHandler = webSocketHandler;
        this.commentaryService = commentaryService;
        this.webSocketBrokerService = webSocketBrokerService;
        this.badmintonMatchTeamPlayerMappingRepo = badmintonMatchTeamPlayerMappingRepo;
        this.badmintonTournamentRepo = badmintonTournamentRepo;
    }

    @Override
    public BadmintonMatchDto createMatch(String userId, String academyId, CreateBadmintonMatchDto request, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            CreatePickleballMatchDto request2 = entityToDtoUtilsConverter
                    .convertCreateBadmintonMatchDtoToCreatePickleballMatchDto(request);
            PickleballMatchDto dto = pickleballMatchService.createMatch(userId, academyId, request2, sport);

            return entityToDtoUtilsConverter.convertPickleballMatchDtoToBadmintonMatchDto(dto);
        }

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

        BadmintonMatch badmintonMatch = modelMapper.map(request, BadmintonMatch.class);
        String matchId = UUID.randomUUID().toString();
        badmintonMatch.setId(matchId);
        badmintonMatch.setCreatedByUserProfile(UserProfile.builder().id(userId).build());
        badmintonMatch.setAcademy(Academy.builder().id(academyId).build());
        badmintonMatch.setCreatedAtTimestampUtc(Timestamp.from(Instant.now()));
        badmintonMatch.setUpdatedAtTimestampUtc(Timestamp.from(Instant.now()));
        badmintonMatch.setIsAutogenerated(request.getIsAutogenerated() != null && request.getIsAutogenerated());
        if (!StringUtils.isEmpty(request.getTournamentId())) {
            badmintonMatch.setTournament(BadmintonTournament.builder().id(request.getTournamentId()).build());
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

        BadmintonTournament tournament = null;
        if (StringUtils.isNotEmpty(request.getTournamentId())) {
            tournament = badmintonTournamentRepo.findById(request.getTournamentId())
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
                ? request.getTeamIds().stream().map(TeamDto::getId).collect(Collectors.toList())
                : new ArrayList<>();
        badmintonMatch.setTeams(
                request.getGameFormat() == GameFormat.DOUBLES ? buildBadmintonTeamMapping(matchId, teamIds)
                        : null);
        badmintonMatch.setSinglePlayers(request.getGameFormat() == GameFormat.SINGLES
                ? buildBadmintonSinglesPlayerMapping(matchId, request.getPlayers())
                : null);

        // for game format TEAMS
        badmintonMatch.setBadmintonMatchTeamPlayers(request.getTeamIds() != null
                ? buildBadmintonTeamMatchPlayerMapping(matchId, request.getTeamIds())
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

        badmintonMatchRepo.save(badmintonMatch);

        return getMatch(academyId, matchId, sport);
    }

    @Override
    public BadmintonMatchDto createMatch(String userId, CreateBadmintonMatchDto request, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            CreatePickleballMatchDto request2 = entityToDtoUtilsConverter
                    .convertCreateBadmintonMatchDtoToCreatePickleballMatchDto(request);

            PickleballMatchDto dto = pickleballMatchService.createMatch(userId, request2, sport);

            return entityToDtoUtilsConverter.convertPickleballMatchDtoToBadmintonMatchDto(dto);
        }

        if (request.getGameFormat() == GameFormat.SINGLES && CollectionUtils.isEmpty(request.getPlayers())) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Player are required for singles match");
        } else if (request.getGameFormat() == GameFormat.DOUBLES && CollectionUtils.isEmpty(request.getTeamIds())) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Teams are required for doubles match");
        }

        if (request.getMatchStatus() == MatchStatus.SCHEDULED && StringUtils.isEmpty(request.getScheduledStartTime())) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                    "scheduledStartTime is required for scheduled match");
        }

        BadmintonMatch badmintonMatch = modelMapper.map(request, BadmintonMatch.class);
        String matchId = UUID.randomUUID().toString();
        badmintonMatch.setId(matchId);
        badmintonMatch.setCreatedByUserProfile(UserProfile.builder().id(userId).build());
        badmintonMatch.setCreatedAtTimestampUtc(Timestamp.from(Instant.now()));
        badmintonMatch.setUpdatedAtTimestampUtc(Timestamp.from(Instant.now()));
        if (!StringUtils.isEmpty(request.getTournamentId())) {
            badmintonMatch.setTournament(BadmintonTournament.builder().id(request.getTournamentId()).build());
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
        BadmintonTournament tournament = null;
        if (StringUtils.isNotEmpty(request.getTournamentId())) {
            tournament = badmintonTournamentRepo.findById(request.getTournamentId())
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
                ? request.getTeamIds().stream().map(TeamDto::getId).collect(Collectors.toList())
                : new ArrayList<>();
        badmintonMatch.setTeams(
                request.getGameFormat() == GameFormat.DOUBLES ? buildBadmintonTeamMapping(matchId, teamIds) : null);
        badmintonMatch.setSinglePlayers(request.getGameFormat() == GameFormat.SINGLES
                ? buildBadmintonSinglesPlayerMapping(matchId, request.getPlayers())
                : null);

        // for game format TEAMS
        badmintonMatch.setBadmintonMatchTeamPlayers(
                request.getTeamIds() != null ? buildBadmintonTeamMatchPlayerMapping(matchId, request.getTeamIds())
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

        badmintonMatchRepo.save(badmintonMatch);

        return getMatch(userId, matchId, sport);
    }

    @Override
    public PaginatedResponse<BadmintonMatchDto> getMatchesByStatusSpecification(String userId,
            GenericFilter filter, Sports sport)
            throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            PaginatedResponse<PickleballMatchDto> matchesByStatusPaginated = pickleballMatchService
                    .getMatchesByStatusSpecification(userId, filter);
            List<BadmintonMatchDto> list = matchesByStatusPaginated.getBody()
                    .stream()
                    .map(m -> entityToDtoUtilsConverter.convertPickleballMatchDtoToBadmintonMatchDto(m))
                    .toList();

            return new PaginatedResponse<BadmintonMatchDto>(
                    list,
                    matchesByStatusPaginated.getTotalElements(),
                    matchesByStatusPaginated.getTotalPages(),
                    matchesByStatusPaginated.getCurrentPage());
        }

        if (StringUtils.isNotEmpty(filter.getAcademyId())) {
            academyService.getAcademyById(filter.getAcademyId());
        }

        BadmintonMatchSpecification spec = new BadmintonMatchSpecification(filter, filter.getMatchStatus());

        Pageable pageable = PageRequest.of(filter.getCurrentPage(), filter.getPageSize());

        Page<BadmintonMatch> matches = badmintonMatchRepo.findAll(spec, pageable);

        long totalElements = matches.getTotalElements();
        int totalPages = matches.getTotalPages();

        if (matches.isEmpty()) {
            return new PaginatedResponse<BadmintonMatchDto>(List.of(), totalElements,
                    totalPages, filter.getCurrentPage());
        }

        List<BadmintonMatchDto> list = matches.getContent().stream().map(match -> {
            try {
                return convertToDto(userId, match, sport);
            } catch (ResourceException e) {
                log.error("Error while converting match to dto", e);
                return null;
            }
        }).filter(Objects::nonNull).toList();

        return new PaginatedResponse<>(list, totalElements,
                totalPages, filter.getCurrentPage());
    }

    @Override
    public List<BadmintonMatchDto> getMatchesByStatus(String userId, String academyId, MatchStatus matchStatus,
            String tournamentId, String searchText, Sports sport) throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            List<PickleballMatchDto> matchesByStatus = pickleballMatchService.getMatchesByStatus(userId, academyId,
                    matchStatus, tournamentId, searchText, sport);

            return matchesByStatus.stream()
                    .map(m -> entityToDtoUtilsConverter.convertPickleballMatchDtoToBadmintonMatchDto(m))
                    .toList();
        }

        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }
        List<BadmintonMatch> matches;
        if (StringUtils.isEmpty(tournamentId)) {
            if (StringUtils.isEmpty(academyId)) {
                matches = badmintonMatchRepo.findByMatchStatusIn(List.of(matchStatus)).stream()
                        .filter(match -> !match.getInactive()).toList();
            } else {
                matches = badmintonMatchRepo.findByMatchStatusAndAcademy_Id(matchStatus, academyId).stream()
                        .filter(match -> !match.getInactive()).toList();
            }
        } else {
            if (StringUtils.isEmpty(academyId)) {
                matches = badmintonMatchRepo.findByMatchStatusAndTournament_Id(matchStatus, tournamentId).stream()
                        .filter(match -> !match.getInactive()).toList();
            } else {
                matches = badmintonMatchRepo
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
                        boolean foundInTeamPlayers = match.getBadmintonMatchTeamPlayers() != null &&
                                match.getBadmintonMatchTeamPlayers().stream()
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

        return sort(matches).stream().map(match -> {
            try {
                return convertToDto(userId, match, sport);
            } catch (ResourceException e) {
                log.error("Error while converting match to dto", e);
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }

    @Override
    public List<BadmintonMatchDto> getAllMatches(String userId, String academyId, String tournamentId,
            String searchText, Sports sport)
            throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            List<PickleballMatchDto> allMatches = pickleballMatchService.getAllMatches(userId, academyId, tournamentId,
                    searchText, sport);

            return allMatches.stream()
                    .map(m -> entityToDtoUtilsConverter.convertPickleballMatchDtoToBadmintonMatchDto(m))
                    .toList();
        }

        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }
        List<BadmintonMatch> matches;
        if (StringUtils.isEmpty(tournamentId)) {
            if (StringUtils.isNotEmpty(academyId)) {
                matches = StreamSupport.stream(badmintonMatchRepo.findAll().spliterator(), false)
                        .filter(match -> !match.getInactive()).toList();
            } else {
                matches = badmintonMatchRepo.findByAcademy_Id(academyId).stream().filter(match -> !match.getInactive())
                        .toList();
            }
        } else {
            if (StringUtils.isNotEmpty(academyId)) {
                matches = badmintonMatchRepo.findByTournament_Id(tournamentId).stream()
                        .filter(match -> !match.getInactive()).toList();
            } else {
                matches = badmintonMatchRepo.findByAcademy_IdAndTournament_Id(academyId, tournamentId).stream()
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
                        boolean foundInTeamPlayers = match.getBadmintonMatchTeamPlayers() != null &&
                                match.getBadmintonMatchTeamPlayers().stream()
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

        return sort(matches).stream().map(match -> {
            try {
                return convertToDto(userId, match, sport);
            } catch (ResourceException e) {
                log.error("Error while converting match to dto", e);
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }

    @Override
    public BadmintonMatchDto getMatch(String userId, String academyId, String matchId, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballMatchDto match = pickleballMatchService.getMatch(userId, academyId, matchId, sport);

            return entityToDtoUtilsConverter.convertPickleballMatchDtoToBadmintonMatchDto(match);
        }

        academyService.getAcademyById(academyId);
        Optional<BadmintonMatch> match = badmintonMatchRepo.findByAcademy_IdAndId(academyId, matchId);
        if (match.isEmpty() || match.get().getInactive()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
        }
        return convertToDto(userId, match.get(), sport);
    }

    @Override
    public BadmintonMatchDto getMatch(String userId, String matchId, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballMatchDto match = pickleballMatchService.getMatch(userId, matchId, sport);

            return entityToDtoUtilsConverter.convertPickleballMatchDtoToBadmintonMatchDto(match);
        }

        Optional<BadmintonMatch> match = badmintonMatchRepo.findById(matchId);
        if (match.isEmpty() || match.get().getInactive()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
        }
        return convertToDto(userId, match.get(), sport);
    }

    @Override
    public void updateMatchStatus(String userId, String matchId, MatchStatus matchStatus, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballMatchService.updateMatchStatus(userId, matchId, matchStatus, sport);
        } else {
            Optional<BadmintonMatch> match = badmintonMatchRepo.findById(matchId);
            if (match.isEmpty() || match.get().getInactive()) {
                throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
            }

            boolean isCreatedUser = match.get().getCreatedByUserProfile() != null
                    && StringUtils.isNotEmpty(match.get().getCreatedByUserProfile().getId())
                    && match.get().getCreatedByUserProfile().getId().equalsIgnoreCase(userId);
            boolean isScorerUser = match.get().getScorerUserProfile() != null
                    && StringUtils.isNotEmpty(match.get().getScorerUserProfile().getId())
                    && match.get().getScorerUserProfile().getId().equalsIgnoreCase(userId);
            boolean isRefereeUser = match.get().getRefereeUserProfile() != null
                    && StringUtils.isNotEmpty(match.get().getRefereeUserProfile().getId())
                    && match.get().getRefereeUserProfile().getId().equalsIgnoreCase(userId);
            boolean isMatchOfficialUser = match.get().getMatchOfficialUserProfile() != null
                    && StringUtils.isNotEmpty(match.get().getMatchOfficialUserProfile().getId())
                    && match.get().getMatchOfficialUserProfile().getId().equalsIgnoreCase(userId);

            boolean isAuthorized = StringUtils.isNotEmpty(userId) && (isCreatedUser || isScorerUser || isRefereeUser
                    || isMatchOfficialUser);

            if (isAuthorized) {
                BadmintonMatch badmintonMatch = match.get();
                badmintonMatch.setMatchStatus(matchStatus);
                badmintonMatch.setUpdatedAtTimestampUtc(Timestamp.from(Instant.now()));

                if (matchStatus == MatchStatus.ENDED) {
                    badmintonMatch.setEndTime(Instant.now().toString());
                } else if (matchStatus == MatchStatus.IN_PROGRESS) {
                    badmintonMatch.setStartTime(Instant.now().toString());
                }
                badmintonMatchRepo.save(badmintonMatch);
            } else {
                throw new ResourceException(ErrorCodes.INVALID_REQUEST,
                        "User is not authorized to update match status");
            }
        }
    }

    @Override
    public void deleteMatch(String academyId, String matchId, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballMatchService.deleteMatch(academyId, matchId, sport);
        } else {
            academyService.getAcademyById(academyId);
            Optional<BadmintonMatch> match = badmintonMatchRepo.findByAcademy_IdAndId(academyId, matchId);
            if (match.isEmpty() || match.get().getInactive()) {
                throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
            }
            match.get().setInactive(true);
            badmintonMatchRepo.save(match.get());
        }
    }

    @Override
    public void deleteMatch(String matchId, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballMatchService.deleteMatch(matchId, sport);
        } else {
            Optional<BadmintonMatch> match = badmintonMatchRepo.findByAcademyIsNullAndId(matchId);
            if (match.isEmpty() || match.get().getInactive()) {
                throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
            }
            match.get().setInactive(true);
            badmintonMatchRepo.save(match.get());
        }

    }

    @Transactional
    @Override
    public BadmintonMatchDto updateMatch(String userId, String academyId, String matchId,
            UpdateBadmintonMatchDto request, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            UpdatePickleballMatchDto request2 = entityToDtoUtilsConverter
                    .convertUpdateBadmintonMatchDtoToUpdatePickleballMatchDto(request);
            PickleballMatchDto matchDto = pickleballMatchService.updateMatch(userId, academyId, matchId, request2,
                    sport);

            return entityToDtoUtilsConverter.convertPickleballMatchDtoToBadmintonMatchDto(matchDto);
        }

        academyService.getAcademyById(academyId);

        Optional<BadmintonMatch> match = badmintonMatchRepo.findByAcademy_IdAndId(academyId, matchId);
        if (match.isEmpty() || match.get().getInactive()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
        }

        if (match.get().getGameFormat() == GameFormat.SINGLES) {
            updateSinglePlayers(match.get(), request.getPlayers());
        } else if (match.get().getGameFormat() == GameFormat.DOUBLES) {
            if (!CollectionUtils.isEmpty(request.getTeamIds())) {
                List<String> teamIds = request.getTeamIds() != null
                        ? request.getTeamIds().stream().map(TeamDto::getId).collect(Collectors.toList())
                        : new ArrayList<>();
                updateTeams(match.get(), teamIds);
            } else if (!CollectionUtils.isEmpty(request.getTeams())) {
                updateTeamPlayers(match.get(), request.getTeams());
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

        // if (request.getStreamingStatus() != null) {
        // match.get().setStreamingStatus(request.getStreamingStatus());
        // }
        //
        // if (request.getStreamingStatus() == StreamingStatus.ENDED &&
        // match.get().getStreamingStatus() != StreamingStatus.ENDED) {
        // BadmintonScoreNotificationDto badmintonScoreNotificationDto = new
        // BadmintonScoreNotificationDto();
        // badmintonScoreNotificationDto.setMatchId(matchId);
        // badmintonScoreNotificationDto.setType(MatchWSMessageType.MATCH_LIVE_STREAMING_END);
        // webSocketBrokerService.sendMessageToMatch(matchId,
        // AppConstants.GSON.toJson(badmintonScoreNotificationDto));
        // }

        match.get().setUpdatedAtTimestampUtc(Timestamp.from(Instant.now()));
        badmintonMatchRepo.save(match.get());

        return getMatch(academyId, matchId, sport);
    }

    @Transactional
    @Override
    public BadmintonMatchDto updateMatch(String userId, String matchId, UpdateBadmintonMatchDto request, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {

            UpdatePickleballMatchDto request2 = entityToDtoUtilsConverter
                    .convertUpdateBadmintonMatchDtoToUpdatePickleballMatchDto(request);
            PickleballMatchDto pickleballMatchDto = pickleballMatchService.updateMatch(userId, matchId, request2,
                    sport);

            return entityToDtoUtilsConverter.convertPickleballMatchDtoToBadmintonMatchDto(pickleballMatchDto);
        }
        Optional<BadmintonMatch> match = badmintonMatchRepo.findByAcademyIsNullAndId(matchId);
        if (match.isEmpty() || match.get().getInactive()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
        }

        if (match.get().getGameFormat() == GameFormat.SINGLES) {
            updateSinglePlayers(match.get(), request.getPlayers());
        } else if (match.get().getGameFormat() == GameFormat.DOUBLES) {
            if (!CollectionUtils.isEmpty(request.getTeamIds())) {
                List<String> teamIds = request.getTeamIds() != null
                        ? request.getTeamIds().stream().map(TeamDto::getId).collect(Collectors.toList())
                        : new ArrayList<>();
                updateTeams(match.get(), teamIds);
            } else if (!CollectionUtils.isEmpty(request.getTeams())) {
                updateTeamPlayers(match.get(), request.getTeams());
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
        badmintonMatchRepo.save(match.get());

        return getMatch(userId, matchId, sport);
    }

    // Helper method to update single players
    private void updateSinglePlayers(BadmintonMatch match, List<TeamPlayerDto> players) {
        badmintonSinglesPlayerMappingRepo.deleteAll(match.getSinglePlayers());
        List<BadmintonSinglesPlayerMapping> mappings = players.stream().map(player -> {
            BadmintonSinglesPlayerMapping mapping = new BadmintonSinglesPlayerMapping();

            if (StringUtils.isNotEmpty(player.getPlayerUserId())) {
                UserProfile userProfile = null;
                try {
                    userProfile = userProfileRepo.findById(player.getPlayerUserId()).orElseThrow(
                            () -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Player not found"));
                } catch (ResourceException e) {
                    log.error("Error while updating single players", e);
                }
                mapping.setBadmintonMatch(match);
                mapping.setPlayerUserProfile(userProfile);
            } else {
                mapping.setBadmintonMatch(match);
                mapping.setGuestName(player.getGuestPlayerName());
                mapping.setPlayerUserProfile(null);
            }
            return mapping;
        }).toList();
        match.setSinglePlayers(new ArrayList<>(mappings));
    }

    // Helper method to update teams
    private void updateTeams(BadmintonMatch match, List<String> teamIds) {
        badmintonTeamMappingRepo.deleteAll(match.getTeams());
        List<BadmintonTeamMapping> mappings = teamIds.stream().map(teamId -> {
            Team team = null;
            try {
                team = teamRepo.findById(teamId)
                        .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Team not found"));
            } catch (ResourceException e) {
                log.error("Error while updating teams", e);
            }

            BadmintonTeamMapping mapping = new BadmintonTeamMapping();
            mapping.setTeam(team);
            mapping.setBadmintonMatch(match);
            return mapping;
        }).toList();

        match.setTeams(new ArrayList<>(mappings));
    }

    // Helper method to update teams when game format is TEAMS
    private void updateTeamPlayers(BadmintonMatch match, List<TeamMatchPlayerDto> teams) {
        badmintonMatchTeamPlayerMappingRepo.deleteAll(match.getBadmintonMatchTeamPlayers());

        List<BadmintonMatchTeamPlayerMapping> mappings = new ArrayList<>();

        for (TeamMatchPlayerDto teamDto : teams) {
            String teamId = teamDto.getTeamId();

            Team team = null;
            try {
                team = teamRepo.findById(teamId)
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

                        BadmintonMatchTeamPlayerMapping mapping = new BadmintonMatchTeamPlayerMapping();
                        mapping.setBadmintonMatch(match);
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

                    BadmintonMatchTeamPlayerMapping mapping = new BadmintonMatchTeamPlayerMapping();
                    mapping.setBadmintonMatch(match);
                    mapping.setTeam(team);
                    mapping.setGuestName(guestName);
                    mappings.add(mapping);
                }
            }
        }

        match.setBadmintonMatchTeamPlayers(new ArrayList<>(mappings));
    }

    @Override
    public BadmintonMatchDto convertToDto(String userId, BadmintonMatch match, Sports sport) throws ResourceException {
        BadmintonMatchDto badmintonMatchDto = modelMapper.map(match, BadmintonMatchDto.class);
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
            if (!CollectionUtils.isEmpty(match.getBadmintonMatchTeamPlayers())) {
                // New handling for TEAMS format
                Map<String, TeamDto> teamMap = new HashMap<>();

                for (BadmintonMatchTeamPlayerMapping mapping : match.getBadmintonMatchTeamPlayers()) {
                    String teamId = mapping.getTeam().getId();
                    TeamDto teamDto = teamMap.computeIfAbsent(teamId, id -> {
                        TeamDto dto = new TeamDto();
                        dto.setId(id);
                        dto.setTeamName(mapping.getTeam().getTeamName());
                        dto.setPlayers(new ArrayList<>());
                        return dto;
                    });

                    TeamPlayerDto playerDto = new TeamPlayerDto();
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
                List<BadmintonTeamMapping> badmintonTeamMappings = match.getTeams();
                List<TeamDto> teamDtos = badmintonTeamMappings.stream().map(badmintonTeamMapping -> {
                    TeamDto teamDto = new TeamDto();
                    teamDto.setId(badmintonTeamMapping.getTeam().getId());
                    teamDto.setTeamName(badmintonTeamMapping.getTeam().getTeamName());
                    List<TeamPlayerDto> teamPlayerDtos = badmintonTeamMapping.getTeam().getPlayers().stream()
                            .map(teamPlayer -> {
                                TeamPlayerDto teamPlayerDto = new TeamPlayerDto();
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
            List<BadmintonSinglesPlayerMapping> badmintonSinglesPlayerMappings = match.getSinglePlayers();
            List<TeamPlayerDto> teamPlayerDtos = badmintonSinglesPlayerMappings.stream()
                    .map(badmintonSinglesPlayerMapping -> {
                        TeamPlayerDto teamPlayerDto = new TeamPlayerDto();
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
        // else if (match.getGameFormat() == GameFormat.TEAMS) {
        // // New handling for TEAMS format
        // Map<String, TeamDto> teamMap = new HashMap<>();

        // for (BadmintonMatchTeamPlayerMapping mapping :
        // match.getBadmintonMatchTeamPlayers()) {
        // String teamId = mapping.getTeam().getId();
        // TeamDto teamDto = teamMap.computeIfAbsent(teamId, id -> {
        // TeamDto dto = new TeamDto();
        // dto.setId(id);
        // dto.setTeamName(mapping.getTeam().getTeamName());
        // dto.setPlayers(new ArrayList<>());
        // return dto;
        // });

        // TeamPlayerDto playerDto = new TeamPlayerDto();
        // if (mapping.getPlayerUserProfile() != null) {
        // playerDto.setPlayerUserId(mapping.getPlayerUserProfile().getId());
        // playerDto.setUserProfile(modelMapper.map(mapping.getPlayerUserProfile(),
        // UserProfileMinDto.class));
        // } else {
        // playerDto.setGuestPlayerName(mapping.getGuestName());
        // }

        // teamDto.getPlayers().add(playerDto);
        // }

        // badmintonMatchDto.setTeams(new ArrayList<>(teamMap.values()));
        // }

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

        Optional<BadmintonMatchPlayDetail> details = badmintonMatchPlayDetailRepo
                .findByBadmintonMatch_Id(match.getId());

        if (details.isPresent()) {
            // BadmintonMatchDetailDto matchDetailDto =
            // toBadmintonMatchDetailDto(details.get());
            // badmintonMatchDto.setRounds(matchDetailDto.getRounds());
            List<BadmintonMatchRound> matchRoundsPlayDetail = details.get().getBadmintonMatchRounds();
            List<BadmintonMatchRoundsPlayDetail> roundDetails = matchRoundsPlayDetail.stream()
                    .flatMap(each -> each.getBadmintonMatchRoundsPlayDetails().stream())
                    .toList();

            List<BadmintonPlayerScoreDto> playerScores = roundDetails.stream()
                    .map(each -> toBadmintonPlayerScoreDto(each)).toList();

            badmintonMatchDto.setRounds(playerScores);
        }
        return badmintonMatchDto;
    }

    @Override
    public List<BadmintonMatchDto> convertToDto(String userId, List<BadmintonMatch> match, Sports sport) {
        return match.stream().map(badmintonMatch -> {
            try {
                return convertToDto(userId, badmintonMatch, sport);
            } catch (ResourceException e) {
                log.error("Error while converting match to dto", e);
                return null;
            }
        }).toList();
    }

    @Override
    public Map<String, String> streamMatch(String matchId, StreamingStatus status, Sports sport)
            throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            return pickleballMatchService.streamMatch(matchId, status);
        }

        Optional<BadmintonMatch> badmintonMatch = badmintonMatchRepo.findById(matchId);
        if (badmintonMatch.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
        }

        if (badmintonMatch.get().getMatchStatus() != null
                && badmintonMatch.get().getMatchStatus() == MatchStatus.ENDED) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Match is already ended");
        }

        Map<String, String> response = new HashMap<>();
        if (status == StreamingStatus.STARTED) {
            String channel = String.format("s_%s", matchId);
            String token = rtcTokenProvider.generateToken(channel, NO_UID);
            response.put("channel", channel);
            response.put("token", token);
            badmintonMatchRepo.startStreaming(matchId, channel, token);
        } else if (status == StreamingStatus.ENDED) {
            badmintonMatchRepo.endStreaming(matchId);
            BadmintonScoreNotificationDto badmintonScoreNotificationDto = new BadmintonScoreNotificationDto();
            badmintonScoreNotificationDto.setMatchId(matchId);
            badmintonScoreNotificationDto.setType(MatchWSMessageType.MATCH_LIVE_STREAMING_END);
            webSocketBrokerService.sendMessageToMatch(matchId, AppConstants.GSON.toJson(badmintonScoreNotificationDto));
        }
        return response;
    }

    @Override
    public BadmintonMatchDetailDto submitMatchDetails(String matchId,
            BadmintonMatchDetailRequestDto badmintonMatchDetailRequestDto, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballMatchDetailRequestDto request2 = entityToDtoUtilsConverter
                    .convertBadmintonMatchDetailsDtoToPickleballMatchDetailsDto(badmintonMatchDetailRequestDto);

            PickleballMatchDetailDto pickleballMatchDetailDto = pickleballMatchService.submitMatchDetails(matchId,
                    request2, sport);

            return entityToDtoUtilsConverter
                    .convertPickleballMatchDetailDtoToBadmintonMatchDetailDto(pickleballMatchDetailDto);
        }

        BadmintonMatch badmintonMatch = badmintonMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found"));

        if (badmintonMatch.getMatchStatus() != MatchStatus.IN_PROGRESS) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Match is not in progress");
        }

        Optional<BadmintonMatchPlayDetail> existingBadmintonMatchPlayDetail = badmintonMatchPlayDetailRepo
                .findByBadmintonMatch_Id(matchId);
        if (existingBadmintonMatchPlayDetail.isPresent()) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Match details already submitted.");
        }

        BadmintonMatchPlayDetail badmintonMatchPlayDetail = new BadmintonMatchPlayDetail();
        badmintonMatchPlayDetail.setBadmintonMatch(badmintonMatch);
        badmintonMatchPlayDetail.setTournament(badmintonMatch.getTournament());
        badmintonMatchPlayDetail.setBadmintonMatchRounds(new ArrayList<>());

        if (badmintonMatch.getGameFormat() == GameFormat.SINGLES) {
            if (StringUtils.isNotEmpty(badmintonMatchDetailRequestDto.getWinningPlayerUserId())) {
                badmintonMatchPlayDetail.setWinningPlayerUserProfile(
                        UserProfile.builder().id(badmintonMatchDetailRequestDto.getWinningPlayerUserId()).build());
            } else if (StringUtils.isNotEmpty(badmintonMatchDetailRequestDto.getWinningGuestPlayerName())) {
                badmintonMatchPlayDetail
                        .setWinningGuestPlayerName(badmintonMatchDetailRequestDto.getWinningGuestPlayerName());
            } else if (BooleanUtils.isTrue(badmintonMatchDetailRequestDto.getIsTied())) {
                badmintonMatchPlayDetail.setIsTied(true);
            } else {
                throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Winning player is required");
            }
        } else if (badmintonMatch.getGameFormat() == GameFormat.DOUBLES) {
            if (StringUtils.isNotEmpty(badmintonMatchDetailRequestDto.getWinningTeamId())) {
                badmintonMatchPlayDetail
                        .setWinningTeam(Team.builder().id(badmintonMatchDetailRequestDto.getWinningTeamId()).build());
            }
            if (BooleanUtils.isTrue(badmintonMatchDetailRequestDto.getIsTied())) {
                badmintonMatchPlayDetail.setIsTied(true);
            }
        } else {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Winning teams are required");
        }

        for (BadmintonMatchRoundDetailsRequestDto badmintonMatchRoundDetailsRequestDto : badmintonMatchDetailRequestDto
                .getRounds()) {
            BadmintonMatchRound badmintonMatchRound = new BadmintonMatchRound();
            badmintonMatchRound.setRoundNumber(badmintonMatchRoundDetailsRequestDto.getRound());
            badmintonMatchRound.setRoundStartTime(
                    Timestamp.from(Instant.parse(badmintonMatchRoundDetailsRequestDto.getRoundStartTime())));
            badmintonMatchRound.setRoundEndTime(
                    Timestamp.from(Instant.parse(badmintonMatchRoundDetailsRequestDto.getRoundEndTime())));
            badmintonMatchRound.setBadmintonMatchPlayDetail(badmintonMatchPlayDetail);
            badmintonMatchRound.setBadmintonMatchRoundsPlayDetails(new ArrayList<>());

            if (badmintonMatch.getGameFormat() == GameFormat.SINGLES) {
                if (StringUtils.isNotEmpty(badmintonMatchRoundDetailsRequestDto.getWinningPlayerUserId())) {
                    badmintonMatchRound.setWinningPlayerUserProfile(UserProfile.builder()
                            .id(badmintonMatchRoundDetailsRequestDto.getWinningPlayerUserId()).build());
                } else if (StringUtils.isNotEmpty(badmintonMatchRoundDetailsRequestDto.getWinningGuestPlayerName())) {
                    badmintonMatchRound.setWinningGuestPlayerName(
                            badmintonMatchRoundDetailsRequestDto.getWinningGuestPlayerName());
                } else {
                    throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Winning player is required");
                }
            } else if (badmintonMatch.getGameFormat() == GameFormat.DOUBLES
                    && StringUtils.isNotEmpty(badmintonMatchRoundDetailsRequestDto.getWinningTeamId())) {
                badmintonMatchRound.setWinningTeam(
                        Team.builder().id(badmintonMatchRoundDetailsRequestDto.getWinningTeamId()).build());
            } else {
                throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Winning teams are required");
            }

            for (BadmintonPlayerScoreRequestDto badmintonPlayerScoreRequestDto : badmintonMatchRoundDetailsRequestDto
                    .getPlayerScores()) {
                BadmintonMatchRoundsPlayDetail badmintonMatchRoundsPlayDetail = new BadmintonMatchRoundsPlayDetail();

                badmintonMatchRoundsPlayDetail.setBadmintonMatchRound(badmintonMatchRound);
                badmintonMatchRoundsPlayDetail.setScore(badmintonPlayerScoreRequestDto.getScore());
                badmintonMatchRoundsPlayDetail.setTeam(badmintonPlayerScoreRequestDto.getTeamId() == null ? null
                        : Team.builder().id(badmintonPlayerScoreRequestDto.getTeamId()).build());
                if (StringUtils.isNotEmpty(badmintonPlayerScoreRequestDto.getPlayerUserId())) {
                    badmintonMatchRoundsPlayDetail.setPlayerUserProfile(
                            UserProfile.builder().id(badmintonPlayerScoreRequestDto.getPlayerUserId()).build());
                }
                if (StringUtils.isNotEmpty(badmintonPlayerScoreRequestDto.getTeamId())) {
                    badmintonMatchRoundsPlayDetail
                            .setTeam(Team.builder().id(badmintonPlayerScoreRequestDto.getTeamId()).build());
                }
                badmintonMatchRoundsPlayDetail.setGuestPlayerName(badmintonPlayerScoreRequestDto.getGuestPlayerName());
                badmintonMatchRound.getBadmintonMatchRoundsPlayDetails().add(badmintonMatchRoundsPlayDetail);
            }
            badmintonMatchPlayDetail.getBadmintonMatchRounds().add(badmintonMatchRound);
        }
        badmintonMatchPlayDetailRepo.save(badmintonMatchPlayDetail);
        return toBadmintonMatchDetailDto(badmintonMatchPlayDetailRepo.findByBadmintonMatch_Id(matchId).get());
    }

    @Override
    public BadmintonMatchDetailDto getMatchDetails(String matchId, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            PickleballMatchDetailDto matchDetails = pickleballMatchService.getMatchDetails(matchId, sport);

            return entityToDtoUtilsConverter.convertPickleballMatchDetailDtoToBadmintonMatchDetailDto(matchDetails);
        }

        Optional<BadmintonMatch> badmintonMatch = badmintonMatchRepo.findById(matchId);
        if (badmintonMatch.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
        }

        if (badmintonMatch.get().getMatchStatus() != MatchStatus.ENDED) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Match is not ended");
        }

        Optional<BadmintonMatchPlayDetail> badmintonMatchPlayDetail = badmintonMatchPlayDetailRepo
                .findByBadmintonMatch_Id(matchId);
        if (badmintonMatchPlayDetail.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match details not found");
        }

        return toBadmintonMatchDetailDto(badmintonMatchPlayDetail.get());
    }

    // @Override
    // public void submitScore(String matchId, BadmintonScoreRequestDto
    // badmintonScoreRequestDto, Sports sport)
    // throws ResourceException {
    // PickleballScoreRequestDto request2 =
    // convertBadmintonScoreRequestDtoToPickleballScoreRequestDto(badmintonScoreRequestDto);
    //
    // if (Sports.PICKLEBALL.equals(sport)) {
    // pickleballMatchService.submitScore(matchId, request2, sport);
    // }
    //
    // if (StringUtils.isEmpty(badmintonScoreRequestDto.getGuestPlayerName())
    // && StringUtils.isEmpty(badmintonScoreRequestDto.getPlayerUserId())) {
    // throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Player is
    // required");
    // }
    //
    // BadmintonScoreNotificationDto badmintonScoreNotificationDto = new
    // BadmintonScoreNotificationDto();
    // badmintonScoreNotificationDto.setMatchId(matchId);
    // badmintonScoreNotificationDto.setComment(badmintonScoreRequestDto.getComment());
    // badmintonScoreNotificationDto.setPoints(badmintonScoreRequestDto.getPoints());
    // badmintonScoreNotificationDto.setTeamId(badmintonScoreRequestDto.getTeamId());
    // if (StringUtils.isNotEmpty(badmintonScoreRequestDto.getPlayerUserId())) {
    // UserProfileDto userProfileDto =
    // userProfileRepo.findById(badmintonScoreRequestDto.getPlayerUserId())
    // .map(userProfile -> modelMapper.map(userProfile,
    // UserProfileDto.class)).orElse(null);
    // if (userProfileDto == null) {
    // throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
    // }
    // badmintonScoreNotificationDto
    // .setPlayerUserProfile(modelMapper.map(userProfileDto,
    // UserProfileMinDto.class));
    // } else {
    // badmintonScoreNotificationDto.setGuestPlayerName(badmintonScoreRequestDto.getGuestPlayerName());
    // }
    //
    // String message = AppConstants.GSON.toJson(badmintonScoreNotificationDto);
    // Long clientsCount =
    // redisTemplate.convertAndSend(scoreUpdatesTopic.getTopic(), message);
    //
    // log.info("clientsCount: {}", clientsCount);
    // log.info("Published score update for match {} to Redis", matchId);
    //
    // List<WebSocketSession> sessions =
    // badmintonMatchSubscribedSessions.get(matchId);
    // if (CollectionUtils.isEmpty(sessions)) {
    // return;
    // }
    //
    // sessions.forEach(session -> {
    // try {
    // session.sendMessage(new
    // TextMessage(AppConstants.GSON.toJson(badmintonScoreNotificationDto)));
    // } catch (IOException e) {
    // log.error("Error while sending message", e);
    // }
    // });
    // }

    @Transactional
    @Override
    public void deleteAllAutogeneratedMatchesByTournament(String academyId, String tournamentId, Sports sport)
            throws ResourceException {

        if (Sports.PICKLEBALL.equals(sport)) {
            pickleballMatchService.deleteAllAutogeneratedMatchesByTournament(academyId, tournamentId, sport);
        }

        if (StringUtils.isNotEmpty(academyId)) {
            academyService.getAcademyById(academyId);
        }
        List<BadmintonMatch> matches = badmintonMatchRepo.findByMatchStatusInAndTournament_Id(
                List.of(MatchStatus.ENDED, MatchStatus.ABANDONED, MatchStatus.IN_PROGRESS), tournamentId);
        if (CollectionUtils.isEmpty(matches)) {
            badmintonMatchRepo.deleteByAutogenMatchesByTournament(tournamentId);
        } else {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Matches are in progress or ended");
        }
    }

    private List<BadmintonMatch> sort(List<BadmintonMatch> badmintonMatches) {
        return badmintonMatches.stream()
                .sorted(Comparator.comparing(BadmintonMatch::getCreatedAtTimestampUtc).reversed()).toList();
    }

    private List<BadmintonTeamMapping> buildBadmintonTeamMapping(String matchId, List<String> teamIds) {
        return teamIds.stream().map(teamId -> {
            BadmintonTeamMapping badmintonTeamMapping = new BadmintonTeamMapping();
            badmintonTeamMapping.setTeam(Team.builder().id(teamId).build());
            badmintonTeamMapping.setBadmintonMatch(BadmintonMatch.builder().id(matchId).build());
            return badmintonTeamMapping;
        }).toList();
    }

    private List<BadmintonSinglesPlayerMapping> buildBadmintonSinglesPlayerMapping(String matchId,
            List<TeamPlayerDto> teamPlayerDtos) {
        return teamPlayerDtos.stream().map(teamPlayerDto -> {
            BadmintonSinglesPlayerMapping badmintonSinglesPlayerMapping = new BadmintonSinglesPlayerMapping();
            badmintonSinglesPlayerMapping.setPlayerUserProfile(
                    org.apache.commons.lang3.StringUtils.isEmpty(teamPlayerDto.getPlayerUserId()) ? null
                            : UserProfile.builder().id(teamPlayerDto.getPlayerUserId()).build());
            badmintonSinglesPlayerMapping.setBadmintonMatch(BadmintonMatch.builder().id(matchId).build());
            badmintonSinglesPlayerMapping.setGuestName(teamPlayerDto.getGuestPlayerName());
            return badmintonSinglesPlayerMapping;
        }).toList();
    }

    private List<BadmintonMatchTeamPlayerMapping> buildBadmintonTeamMatchPlayerMapping(
            String matchId,
            List<TeamDto> teams) {

        if (teams == null || teams.isEmpty())
            return Collections.emptyList();

        List<BadmintonMatchTeamPlayerMapping> mappings = new ArrayList<>();

        BadmintonMatch matchRef = BadmintonMatch.builder().id(matchId).build();

        for (TeamDto teamDto : teams) {
            Team teamRef = Team.builder().id(teamDto.getId()).build();

            // Registered players
            if (teamDto.getPlayers() != null) {
                for (TeamPlayerDto playerDto : teamDto.getPlayers()) {
                    if (playerDto == null || playerDto.getPlayerUserId() == null
                            || playerDto.getPlayerUserId().isBlank())
                        continue;

                    mappings.add(BadmintonMatchTeamPlayerMapping.builder()
                            .badmintonMatch(matchRef)
                            .team(teamRef)
                            .playerUserProfile(UserProfile.builder().id(playerDto.getPlayerUserId()).build())
                            .build());
                }
            }

            // Guest players
            if (teamDto.getPlayers() != null) {
                for (TeamPlayerDto guestDto : teamDto.getPlayers()) {
                    if (guestDto == null || guestDto.getGuestPlayerName() == null
                            || guestDto.getGuestPlayerName().isBlank())
                        continue;

                    mappings.add(BadmintonMatchTeamPlayerMapping.builder()
                            .badmintonMatch(matchRef)
                            .team(teamRef)
                            .guestName(guestDto.getGuestPlayerName())
                            .build());
                }
            }
        }

        return mappings;
    }

    private BadmintonMatchDetailDto toBadmintonMatchDetailDto(BadmintonMatchPlayDetail badmintonMatchPlayDetail)
            throws ResourceException {
        return BadmintonMatchDetailDto.builder()
                .rounds(badmintonMatchPlayDetail.getBadmintonMatchRounds().stream()
                        .map(this::toBadmintonMatchRoundDetailsDto)
                        .sorted(Comparator.comparing(BadmintonMatchRoundDetailsDto::getRound))
                        .toList())
                .winningPlayerUserProfile(badmintonMatchPlayDetail.getWinningPlayerUserProfile() == null ? null
                        : modelMapper.map(badmintonMatchPlayDetail.getWinningPlayerUserProfile(),
                                UserProfileMinDto.class))
                .winningGuestPlayerName(badmintonMatchPlayDetail.getWinningGuestPlayerName())
                .winningTeam(badmintonMatchPlayDetail.getWinningTeam() == null ? null
                        : teamsService.convertToDto(badmintonMatchPlayDetail.getWinningTeam()))
                .isTied(badmintonMatchPlayDetail.getIsTied())
                .build();
    }

    private BadmintonMatchRoundDetailsDto toBadmintonMatchRoundDetailsDto(BadmintonMatchRound badmintonMatchRound) {
        return BadmintonMatchRoundDetailsDto.builder().round(badmintonMatchRound.getRoundNumber())
                .roundStartTime(badmintonMatchRound.getRoundStartTime().toString())
                .roundEndTime(badmintonMatchRound.getRoundEndTime().toString())
                .playerScores(badmintonMatchRound.getBadmintonMatchRoundsPlayDetails().stream()
                        .map(this::toBadmintonPlayerScoreDto).toList())
                .winningPlayerUserProfile(badmintonMatchRound.getWinningPlayerUserProfile() == null ? null
                        : modelMapper.map(badmintonMatchRound.getWinningPlayerUserProfile(), UserProfileMinDto.class))
                .winningGuestPlayerName(badmintonMatchRound.getWinningGuestPlayerName())
                .winningTeam(badmintonMatchRound.getWinningTeam() == null ? null
                        : teamsService.convertToDto(badmintonMatchRound.getWinningTeam()))
                .build();
    }

    private BadmintonPlayerScoreDto toBadmintonPlayerScoreDto(
            BadmintonMatchRoundsPlayDetail badmintonMatchRoundsPlayDetail) {
        return BadmintonPlayerScoreDto.builder().score(badmintonMatchRoundsPlayDetail.getScore())
                .playerUserProfile(badmintonMatchRoundsPlayDetail.getPlayerUserProfile() == null ? null
                        : modelMapper.map(badmintonMatchRoundsPlayDetail.getPlayerUserProfile(),
                                UserProfileMinDto.class))
                .team(badmintonMatchRoundsPlayDetail.getTeam() == null ? null
                        : teamsService.convertToDto(badmintonMatchRoundsPlayDetail.getTeam()))
                .guestPlayerName(badmintonMatchRoundsPlayDetail.getGuestPlayerName())
                .roundNumber(badmintonMatchRoundsPlayDetail.getBadmintonMatchRound().getRoundNumber()).build();
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
    public Pair<Long, Long> getMatchCounts(String tournamentId, Sports sport) throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            return pickleballMatchService.getMatchCounts(tournamentId);
        }
        var tuple = badmintonMatchRepo.getMatchesCount(tournamentId).get(0);
        return Pair.of(Long.valueOf(tuple.get("total_matches_played") + ""),
                Long.valueOf(tuple.get("total_matches_completed") + ""));
    }

    @Override
    public List<PlayerPerformanceDto> getGuestPlayerPerformances(String tournamentId, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            return pickleballMatchService.getGuestPlayerPerformances(tournamentId);
        }

        var guestPerformances = badmintonSinglesPlayerMappingRepo.getGuestPlayerPerformance(tournamentId, 10);
        return guestPerformances.stream().map(columns -> {
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
        }).toList();
    }

    @Override
    public List<PlayerPerformanceDto> getPlayerPerformances(String tournamentId, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            return pickleballMatchService.getPlayerPerformances(tournamentId);
        }

        return badmintonSinglesPlayerMappingRepo.getPlayerPerformance(tournamentId, 10).stream().map(columns -> {
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
        }).toList();
    }

    @Transactional
    @Override
    public void updateRealTimeScore(String matchId, RealTimeScoreUpdateRequestDto requestDto) throws ResourceException {
        // Verify match exists and is in progress
        BadmintonMatch match = badmintonMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.INVALID_REQUEST, "Match not found"));

        if (match.getMatchStatus() != MatchStatus.IN_PROGRESS) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Match is not in progress");
        }

        // If initial is true, we only need to set the BadmintonLiveScore entry with
        // matchId, intial and metadata
        if (Boolean.TRUE.equals(requestDto.getInitial())) {
            log.info("Initial score update received for match: {}", matchId);
            String initialComment = "Match is about to start";
            BadmintonLiveScore liveScore = new BadmintonLiveScore();
            liveScore.setMatchId(matchId);
            liveScore.setMetadata(requestDto.getMetadata());
            liveScore.setInitial(requestDto.getInitial());
            liveScore.setScoreTime(LocalDateTime.now().toString());
            liveScore.setLastUpdated(new Timestamp(System.currentTimeMillis()));

            // Ensure requestId is generated for the initial entry
            // For consistency, an initial entry also gets a request ID
            requestDto.setRequestId(UUID.randomUUID().toString());
            liveScore.setRequestId(requestDto.getRequestId());

            liveScore.setRoundNumber(null); // Or 0L if preferred as default
            liveScore.setPlayerId(null);
            liveScore.setGuestPlayerName(null);
            liveScore.setTeamId(null);
            liveScore.setScore(0l); // Or 0L if preferred as default
            liveScore.setShotType(null);
            liveScore.setCommentary(null);
            liveScore.setCommentary(initialComment);

            liveScoreRepo.save(liveScore);

            // separate method to send initial score notification via ws
            sendInitialScoreNotification(matchId, match, requestDto, initialComment);

            return;
        }

        // Handle requestId for UNDO functionality
        if (Boolean.TRUE.equals(requestDto.getIsUndo())) {
            // For UNDO action, we need to find the previous score entry
            // First, fetch all scores for this match ordered by last updated time
            List<BadmintonLiveScore> allMatchScores = liveScoreRepo.findByMatchIdOrderByLastUpdatedDesc(matchId);

            // Get the most recent score entry
            BadmintonLiveScore latestScore = allMatchScores.isEmpty() ? null : allMatchScores.get(0);

            if (latestScore.getRequestId() == null) {
                throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Cannot undo: missing request ID");
            }

            // Delete the latest entry
            liveScoreRepo.delete(latestScore);

            // If we have at least two entries, we can get the previous score value
            if (allMatchScores.size() > 1) {
                // We have a previous entry - revert to it
                BadmintonLiveScore previousScore = allMatchScores.get(1);
                requestDto.setScore(previousScore.getScore());
                requestDto.setRoundNumber(previousScore.getRoundNumber());
                requestDto.setMetadata(previousScore.getMetadata());
                requestDto.setInitial(previousScore.getInitial() != null ? previousScore.getInitial() : Boolean.FALSE);

                // Update the Redis key to point to the previous requestId
                String redisKey = "match:" + matchId + ":latestRequestId";
                redisTemplate.opsForValue().set(redisKey, previousScore.getRequestId());

                // Send notification with the previous score's data
                sendUndoNotification(matchId, match, requestDto, previousScore.getRequestId());

                return;
            } else {
                // No previous entry - reset to initial status (score = 0)
                log.info("No previous score found for matchId: {} - resetting to initial state", matchId);

                requestDto.setScore(0L);
                requestDto.setMetadata(latestScore.getMetadata());
                requestDto.setInitial(latestScore.getInitial() != null ? latestScore.getInitial() : Boolean.FALSE);
                // Clear the latest requestId in Redis
                String redisKey = "match:" + matchId + ":latestRequestId";
                redisTemplate.delete(redisKey);

                // Send notification with empty/reset data
                sendUndoNotification(matchId, match, requestDto,
                        latestScore.getRequestId() != null ? latestScore.getRequestId() : null);

                return;
            }
        } else {
            // Generate a new unique requestId for this action
            requestDto.setRequestId(UUID.randomUUID().toString());
        }
        // Always create a new BadmintonLiveScore entry instead of updating existing
        // ones
        BadmintonLiveScore liveScore = new BadmintonLiveScore();

        liveScore.setScoreTime(LocalDateTime.now().toString());
        liveScore.setMatchId(matchId);
        liveScore.setPlayerId(requestDto.getPlayerId());
        liveScore.setRoundNumber(requestDto.getRoundNumber());
        liveScore.setGuestPlayerName(requestDto.getGuestPlayerName());
        liveScore.setTeamId(requestDto.getTeamId());
        liveScore.setScore(requestDto.getScore());
        liveScore.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        liveScore.setShotType(requestDto.getShotType());
        // Set the requestId in the database entry
        liveScore.setRequestId(requestDto.getRequestId());
        // Set the metadata required for the mobile team
        log.info("metadata: {}", requestDto.getMetadata());
        liveScore.setMetadata(requestDto.getMetadata());

        // Store the latest requestId in Redis for quick access
        String redisKey = "match:" + matchId + ":latestRequestId";
        redisTemplate.opsForValue().set(redisKey, requestDto.getRequestId());

        // Generate commentary based on shot type
        String playerName = requestDto.getPlayerId() != null ? userProfileRepo.findById(requestDto.getPlayerId())
                .map(UserProfile::getDisplayName)
                .orElse("A player") : requestDto.getGuestPlayerName();

        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "A player";
        }

        BadmintonMatchContext matchContext = createMatchContextFromMatch(match, requestDto);
        BadmintonEventType eventType = determineEventType(matchContext, requestDto);
        String commentary = null;
        if (eventType != null) {
            commentary = commentaryService.generateCommentary(eventType, matchContext);
        }

        if (commentary != null && !commentary.isEmpty()) {
            liveScore.setCommentary(commentary);
        }

        liveScoreRepo.save(liveScore);

        // Create notification DTO
        BadmintonScoreNotificationDto notificationDto = new BadmintonScoreNotificationDto();
        notificationDto.setMatchId(matchId);
        notificationDto.setPoints(Integer.parseInt(requestDto.getScore().toString()));

        // Add commentary to notification if available
        if (commentary != null && !commentary.isEmpty()) {
            notificationDto.setComment(commentary);
        }

        if (org.springframework.util.StringUtils.hasText(matchContext.getScoreTime())) {
            notificationDto.setScoreTime(matchContext.getScoreTime());
        }

        // Set player info if available
        if (requestDto.getPlayerId() != null) {
            notificationDto.setPlayerUserProfile(
                    userProfileRepo.findById(requestDto.getPlayerId())
                            .map(up -> modelMapper.map(up, UserProfileMinDto.class))
                            .orElse(null));
        } else if (requestDto.getGuestPlayerName() != null) {
            notificationDto.setGuestPlayerName(requestDto.getGuestPlayerName());
        }

        notificationDto.setTeamId(requestDto.getTeamId());

        // Set requestId and isUndo flag in the notification
        notificationDto.setRequestId(requestDto.getRequestId());
        notificationDto.setIsUndo(requestDto.getIsUndo());

        // Set the metadata
        log.info("metadata: {}", requestDto.getMetadata());
        notificationDto.setMetadata(requestDto.getMetadata());
        notificationDto.setInitial(requestDto.getInitial() != null ? requestDto.getInitial() : Boolean.FALSE);

        // Publish the notification
        try {
            // Publish to Redis channel
            redisTemplate.convertAndSend(scoreUpdatesTopic.getTopic(), notificationDto);
            log.info("Published real-time commentary for match {}: {}", matchId, commentary);
        } catch (Exception e) {
            log.error("Error publishing real-time update", e);
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to update real-time score");
        }
    }

    private void sendInitialScoreNotification(String matchId, BadmintonMatch match,
            RealTimeScoreUpdateRequestDto requestDto, String comment) throws ResourceException {
        BadmintonScoreNotificationDto initialNotificationDto = new BadmintonScoreNotificationDto();

        initialNotificationDto.setMatchId(matchId);
        initialNotificationDto.setInitial(Boolean.TRUE); // Mark as initial notification
        initialNotificationDto.setMetadata(requestDto.getMetadata()); // Pass along metadata
        initialNotificationDto.setRequestId(requestDto.getRequestId()); // Pass along the generated request ID
        initialNotificationDto.setScoreTime(LocalDateTime.now().toString());

        // Set score-specific fields to null or defaults
        initialNotificationDto.setComment(comment);
        initialNotificationDto.setIsUndo(Boolean.FALSE);

        // Publish the notification
        try {
            // Publish to Redis channel
            redisTemplate.convertAndSend(scoreUpdatesTopic.getTopic(), initialNotificationDto);
            log.info("Published initial score notification for match {}: {}", matchId,
                    initialNotificationDto.getComment());
        } catch (Exception e) {
            log.error("Error publishing initial score notification", e);
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE,
                    "Failed to publish initial score notification");
        }
    }

    private void sendUndoNotification(String matchId, BadmintonMatch match, RealTimeScoreUpdateRequestDto requestDto,
            String requestId) {
        // Generate commentary based on shot type
        String playerName = requestDto.getPlayerId() != null ? userProfileRepo.findById(requestDto.getPlayerId())
                .map(UserProfile::getDisplayName)
                .orElse("A player") : requestDto.getGuestPlayerName();

        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "A player";
        }

        BadmintonMatchContext matchContext = createMatchContextFromMatch(match, requestDto);
        String commentary = "Undo last action";

        // Create notification DTO
        BadmintonScoreNotificationDto notificationDto = new BadmintonScoreNotificationDto();
        notificationDto.setMatchId(matchId);
        notificationDto.setComment(commentary);

        if (org.springframework.util.StringUtils.hasText(matchContext.getScoreTime())) {
            notificationDto.setScoreTime(matchContext.getScoreTime());
        }

        // Set player info if available
        if (requestDto.getPlayerId() != null) {
            notificationDto.setPlayerUserProfile(
                    userProfileRepo.findById(requestDto.getPlayerId())
                            .map(up -> modelMapper.map(up, UserProfileMinDto.class))
                            .orElse(null));
        } else if (requestDto.getGuestPlayerName() != null) {
            notificationDto.setGuestPlayerName(requestDto.getGuestPlayerName());
        }

        notificationDto.setTeamId(requestDto.getTeamId());

        // Set requestId and isUndo flag in the notification
        notificationDto.setRequestId(requestId);
        notificationDto.setIsUndo(true);
        notificationDto.setMetadata(requestDto.getMetadata());
        notificationDto.setInitial(requestDto.getInitial());

        // Publish the notification
        try {
            // Publish to Redis channel
            redisTemplate.convertAndSend(scoreUpdatesTopic.getTopic(), notificationDto);
            log.info("Published undo notification for match {}", matchId);
        } catch (Exception e) {
            log.error("Error publishing undo notification", e);
        }

    }

    @Transactional
    // @Override
    public void updateRealTimeScoreOld(String matchId, RealTimeScoreUpdateRequestDto requestDto)
            throws ResourceException {
        // Verify match exists and is in progress
        BadmintonMatch match = badmintonMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.INVALID_REQUEST, "Match not found"));

        if (match.getMatchStatus() != MatchStatus.IN_PROGRESS) {
            throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Match is not in progress");
        }

        // Update or create live scores
        PlayerScoreUpdateDto scoreUpdate = requestDto.getPlayerScores();
        BadmintonLiveScore liveScore = scoreUpdate.getPlayerId() != null
                ? liveScoreRepo.findByMatchIdAndPlayerId(matchId, scoreUpdate.getPlayerId())
                        .orElse(new BadmintonLiveScore())
                : liveScoreRepo.findByMatchIdAndGuestPlayerName(matchId, scoreUpdate.getGuestPlayerName())
                        .orElse(new BadmintonLiveScore());

        liveScore.setMatchId(matchId);
        liveScore.setRoundNumber(requestDto.getRoundNumber());
        liveScore.setPlayerId(scoreUpdate.getPlayerId());
        liveScore.setGuestPlayerName(scoreUpdate.getGuestPlayerName());
        liveScore.setTeamId(scoreUpdate.getTeamId());
        liveScore.setScore(scoreUpdate.getScore());
        liveScore.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        liveScore.setShotType(scoreUpdate.getShotType());
        liveScoreRepo.save(liveScore);

        // Generate match context and commentary
        BadmintonMatchContext matchContext = createMatchContextFromMatchOld(match, requestDto);
        String commentary = null;
        BadmintonEventType eventType = determineEventTypeOld(matchContext, requestDto);
        if (eventType != null) {
            commentary = commentaryService.generateCommentary(eventType, matchContext);
        }

        // Publish update to Redis for notifications
        BadmintonScoreNotificationDto notificationDto = new BadmintonScoreNotificationDto();
        notificationDto.setMatchId(matchId);
        notificationDto.setPoints(requestDto.getPlayerScores().getScore().intValue()); // Assuming first score for
        // points
        notificationDto.setTeamId(requestDto.getPlayerScores().getTeamId());

        // Add commentary to notification if available
        if (commentary != null && !commentary.isEmpty()) {
            notificationDto.setComment(commentary);
        }

        // Set player or guest info
        PlayerScoreUpdateDto firstScore = requestDto.getPlayerScores();
        if (firstScore.getPlayerId() != null) {
            UserProfileDto userProfile = userProfileRepo.findById(firstScore.getPlayerId())
                    .map(up -> modelMapper.map(up, UserProfileDto.class)).orElse(null);
            if (userProfile != null) {
                notificationDto.setPlayerUserProfile(modelMapper.map(userProfile, UserProfileMinDto.class));
            }
        } else {
            notificationDto.setGuestPlayerName(firstScore.getGuestPlayerName());
        }

        // Create and send the message
        String message = AppConstants.GSON.toJson(notificationDto);

        // Store in Redis only (no WebSocket history)
        String redisKey = "match:scores:" + matchId;
        try {
            // Store the latest message
            redisTemplate.opsForValue().set(redisKey + ":latest", message);

            // Add to a list of all updates for this match
            redisTemplate.opsForList().rightPush(redisKey + ":history", message);

            // Trim the list to keep only the last 100 updates
            redisTemplate.opsForList().trim(redisKey + ":history", -100, -1);

            // Set expiration (optional - e.g., 24 hours)
            redisTemplate.expire(redisKey + ":latest", 24, TimeUnit.HOURS);
            redisTemplate.expire(redisKey + ":history", 24, TimeUnit.HOURS);

            // Publish to Redis channel for any other services that might be listening
            // redisTemplate.convertAndSend(scoreUpdatesTopic.getTopic(), message);
            redisTemplate.convertAndSend(scoreUpdatesTopic.getTopic(), notificationDto);
            log.info("Stored match data in Redis for match {}", matchId);
        } catch (Exception e) {
            log.error("Error storing match data in Redis for match {}: {}", matchId, e.getMessage());
        }

        log.info("Published real-time score update for match {} to Redis", matchId);
    }

    /**
     * Creates a BadmintonMatchContext from the match and score update data
     *
     * @param match      The badminton match entity
     * @param requestDto The score update request
     * @return A BadmintonMatchContext with relevant match data
     */
    private BadmintonMatchContext createMatchContextFromMatch(BadmintonMatch match,
            RealTimeScoreUpdateRequestDto requestDto) {
        BadmintonMatchContext context = new BadmintonMatchContext();
        String shotType = requestDto.getShotType() != null ? requestDto.getShotType() : "powerful shot";
        context.setMatchId(match.getId());

        // Set player information from the request
        String playerName = requestDto.getPlayerId() != null ? userProfileRepo.findById(requestDto.getPlayerId())
                .map(UserProfile::getDisplayName)
                .orElse("Player") : requestDto.getGuestPlayerName();

        context.setCurrentPlayer(playerName);
        context.setShotType(shotType);

        // Set basic match info
        context.setScoreTime(LocalDateTime.now().toString());

        // Set game format
        boolean isDoubles = match.getGameFormat() == GameFormat.DOUBLES;
        context.setDoubles(isDoubles);

        // Set player/team names if available
        if (isDoubles && match.getTeams() != null && !match.getTeams().isEmpty()) {
            List<BadmintonTeamMapping> teams = match.getTeams();
            if (teams.size() > 0) {
                BadmintonTeamMapping teamMapping1 = teams.get(0);
                // Ensure team and team name are not null before trying to access them
                if (teamMapping1 != null && teamMapping1.getTeam() != null
                        && StringUtils.isNotBlank(teamMapping1.getTeam().getTeamName())) {
                    context.setTeam1(teamMapping1.getTeam().getTeamName());
                    context.setPlayer1(teamMapping1.getTeam().getTeamName());
                } else {
                    // Fallback if team or team name is missing
                    context.setTeam1("Team 1");
                    context.setPlayer1("Team 1"); // Fallback Player1 to fallback Team Name
                }
            }

            if (teams.size() > 1) {
                BadmintonTeamMapping teamMapping2 = teams.get(1);
                // Ensure team and team name are not null
                if (teamMapping2 != null && teamMapping2.getTeam() != null
                        && StringUtils.isNotBlank(teamMapping2.getTeam().getTeamName())) {
                    context.setTeam2(teamMapping2.getTeam().getTeamName());
                    context.setPlayer2(teamMapping2.getTeam().getTeamName());
                } else {
                    // Fallback if team or team name is missing
                    context.setTeam2("Team 2");
                    context.setPlayer2("Team 2"); // Fallback Player2 to fallback Team Name
                }
            }

            // Set the current player with team name for both registered and guest players
            String finalPlayerName = playerName;

            // For doubles, append team name if teamId is provided
            if (isDoubles && StringUtils.isNotBlank(requestDto.getTeamId())) {
                String teamId = requestDto.getTeamId();
                for (BadmintonTeamMapping teamMapping : teams) {
                    if (teamMapping.getTeam() != null && teamId.equals(teamMapping.getTeam().getId())) {
                        String teamName = org.springframework.util.StringUtils.hasText(
                                teamMapping.getTeam().getTeamName()) ? teamMapping.getTeam().getTeamName() : "Team";
                        if (StringUtils.isNotBlank(teamName)) {
                            finalPlayerName = playerName + " (" + teamName + ")";
                        }
                        break;
                    }
                }
            }

            context.setCurrentPlayer(finalPlayerName);
        } else if (match.getSinglePlayers() != null && !match.getSinglePlayers().isEmpty()) {
            List<BadmintonSinglesPlayerMapping> players = match.getSinglePlayers();
            if (players.size() > 0) {
                context.setPlayer1(players.get(0).getPlayerUserProfile() != null
                        ? players.get(0).getPlayerUserProfile().getDisplayName()
                        : players.get(0).getGuestName());
            }
            if (players.size() > 1) {
                context.setPlayer2(players.get(1).getPlayerUserProfile() != null
                        ? players.get(1).getPlayerUserProfile().getDisplayName()
                        : players.get(1).getGuestName());
            }

            // Set the current player based on the playerId in the request
            context.setCurrentPlayer(playerName);
        }
        // context.setShotType(requestDto.getShotType());
        return context;
    }

    /**
     * Determines the appropriate event type based on the match context and score
     * update
     *
     * @param context    The match context
     * @param requestDto The score update request
     * @return The appropriate BadmintonEventType for commentary
     */
    private BadmintonEventType determineEventType(BadmintonMatchContext context,
            RealTimeScoreUpdateRequestDto requestDto) {
        // Simple logic to determine event type based on score and context
        Long playerScore = requestDto.getScore();

        // Check for match point
        if (playerScore >= 20) {
            return BadmintonEventType.MATCH_POINT;
        }

        // Default event type
        return BadmintonEventType.POINT_SCORED;
    }

    /**
     * (OLD - BUT USEFUL) Creates a BadmintonMatchContext from the match and score
     * update data
     *
     * @param match      The badminton match entity
     * @param requestDto The score update request
     * @return A BadmintonMatchContext with relevant match data
     */
    private BadmintonMatchContext createMatchContextFromMatchOld(BadmintonMatch match,
            RealTimeScoreUpdateRequestDto requestDto) {
        BadmintonMatchContext context = new BadmintonMatchContext();
        context.setMatchId(match.getId());

        // Get player/team information
        if (match.getGameFormat() == GameFormat.SINGLES && match.getSinglePlayers() != null
                && !match.getSinglePlayers().isEmpty()) {
            List<BadmintonSinglesPlayerMapping> players = match.getSinglePlayers();
            if (players.size() > 0) {
                context.setPlayer1(
                        players.get(0).getPlayerUserProfile() != null ? players.get(0).getPlayerUserProfile().getId()
                                : players.get(0).getGuestName());
            }
            if (players.size() > 1) {
                context.setPlayer2(
                        players.get(1).getPlayerUserProfile() != null ? players.get(1).getPlayerUserProfile().getId()
                                : players.get(1).getGuestName());
            }
            context.setDoubles(false);
        } else if (match.getGameFormat() == GameFormat.DOUBLES && match.getTeams() != null
                && !match.getTeams().isEmpty()) {
            List<BadmintonTeamMapping> teams = match.getTeams();
            if (teams.size() > 0) {
                context.setTeam1(teams.get(0).getTeam().getTeamName());
                context.setPlayer1(teams.get(0).getTeam().getTeamName());
            }
            if (teams.size() > 1) {
                context.setTeam2(teams.get(1).getTeam().getTeamName());
                context.setPlayer2(teams.get(1).getTeam().getTeamName());
            }
            context.setDoubles(true);
        }

        // Set scores from the request
        if (requestDto.getPlayerScores() != null) {
            PlayerScoreUpdateDto scoreUpdate = requestDto.getPlayerScores();
            // For now, we'll set both player scores to the same value
            // You might want to adjust this based on your game logic
            context.setPlayer1Score(scoreUpdate.getScore() != null ? scoreUpdate.getScore().intValue() : 0);
            context.setPlayer2Score(scoreUpdate.getScore() != null ? scoreUpdate.getScore().intValue() : 0);
        }

        // Set current round/set
        context.setCurrentSet(requestDto.getRoundNumber() != null ? requestDto.getRoundNumber() : 1);

        // For simplicity, we'll set some default values for other fields
        context.setRallyLength(5); // Default value

        // Determine serving player based on score (simplified logic)
        int totalScore = context.getPlayer1Score() + context.getPlayer2Score();
        boolean player1Serving = (totalScore % 2 == 0);
        context.setServingPlayer(player1Serving ? context.getPlayer1() : context.getPlayer2());
        context.setReceivingPlayer(player1Serving ? context.getPlayer2() : context.getPlayer1());

        if (requestDto.getPlayerScores() != null) {
            PlayerScoreUpdateDto scoreUpdate = requestDto.getPlayerScores();

            // Check if this is the winning player
            boolean isWinningPlayer = (requestDto.getWinningPlayerId() != null &&
                    scoreUpdate.getPlayerId() != null &&
                    requestDto.getWinningPlayerId().equals(scoreUpdate.getPlayerId())) ||
                    (requestDto.getWinningGuestPlayerName() != null &&
                            scoreUpdate.getGuestPlayerName() != null &&
                            requestDto.getWinningGuestPlayerName().equals(scoreUpdate.getGuestPlayerName()));

            // Set the shot type if available
            if (isWinningPlayer && scoreUpdate.getShotType() != null) {
                context.setLastShotType(scoreUpdate.getShotType());
            } else {
                // Default shot type if none provided or not the winning player
                context.setLastShotType("powerful shot");
            }
        }
        return context;
    }

    /**
     * (OLD - BUT USEFUL) Determines the appropriate event type based on the match
     * context and score update
     *
     * @param context    The match context
     * @param requestDto The score update request
     * @return The appropriate BadmintonEventType for commentary
     */
    private BadmintonEventType determineEventTypeOld(BadmintonMatchContext context,
            RealTimeScoreUpdateRequestDto requestDto) {
        // Simple logic to determine event type based on score and context
        int player1Score = context.getPlayer1Score();
        int player2Score = context.getPlayer2Score();

        // Check for match point
        if (player1Score >= 20 && player1Score > player2Score) {
            return BadmintonEventType.MATCH_POINT;
        } else if (player2Score >= 20 && player2Score > player1Score) {
            return BadmintonEventType.MATCH_POINT;
        }

        // Check for deuce
        if (player1Score >= 20 && player2Score >= 20 && player1Score == player2Score) {
            return BadmintonEventType.DEUCE;
        }

        // Check for advantage
        if (player1Score >= 20 && player2Score >= 20 && Math.abs(player1Score - player2Score) == 1) {
            return BadmintonEventType.ADVANTAGE;
        }

        // Check for streak (3 or more consecutive points)
        if (requestDto.getWinningPlayerId() != null || requestDto.getWinningGuestPlayerName() != null) {
            // For simplicity, we'll just use POINT_SCORED for now
            return BadmintonEventType.POINT_SCORED;
        }

        // Default event type
        return BadmintonEventType.POINT_SCORED;
    }

    @Transactional
    @Override
    public void finalizeMatchScores(String matchId, BadmintonMatchDetailRequestDto finalScores, Sports sport)
            throws ResourceException {
        // 1. Save final scores using existing implementation
        submitMatchDetails(matchId, finalScores, sport);

        // 2. Clean up live scores
        List<BadmintonLiveScore> liveScores = liveScoreRepo.findByMatchId(matchId);
        if (!liveScores.isEmpty()) {
            liveScoreRepo.deleteAll(liveScores);
        }
    }

    @Override
    public List<BadmintonScoreTrendDto> getScoreTrend(String matchId, Long roundNumber, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            return pickleballMatchService.getScoreTrend(matchId, roundNumber);
        }

        List<BadmintonScoreTrendDto> scoreTrends = new ArrayList<>();

        if (roundNumber == null) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Round number not found");
        }

        Optional<BadmintonMatch> match = badmintonMatchRepo.findById(matchId);
        if (match.isEmpty()) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Match not found");
        }

        List<BadmintonLiveScore> liveScore = liveScoreRepo.findByMatchId(matchId);
        if (CollectionUtils.isEmpty(liveScore)) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Live score not found");
        }

        liveScore = liveScore.stream()
                .filter(live -> live.getRoundNumber() == roundNumber)
                .collect(Collectors.toList());

        // Sort live scores by score time
        liveScore.sort(Comparator.comparing(BadmintonLiveScore::getScoreTime));

        /**
         * To be used later for ID to name mapping
         */
        List<String> playerOrTeamNames = new ArrayList<>();
        if (match.get().getGameFormat() == GameFormat.SINGLES) {
            List<BadmintonSinglesPlayerMapping> singlePlayers = match.get().getSinglePlayers();

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
            List<BadmintonTeamMapping> teamMappings = match.get().getTeams();
            playerOrTeamNames
                    .add(teamMappings.get(0).getTeam().getId() + "#" + teamMappings.get(0).getTeam().getTeamName());
            playerOrTeamNames
                    .add(teamMappings.get(1).getTeam().getId() + "#" + teamMappings.get(1).getTeam().getTeamName());
        }

        for (BadmintonLiveScore score : liveScore) {
            // if (!StringUtils.isEmpty(score.getGuestPlayerName())) {
            scoreTrends = updateScoreTrend(scoreTrends, playerOrTeamNames, score, match.get().getGameFormat());
            // }
        }
        return scoreTrends;
    }

    @Override
    public Map<String, Map<String, Long>> getScoreByShot(String matchId, Long roundNumber, Sports sport)
            throws ResourceException {
        if (Sports.PICKLEBALL.equals(sport)) {
            return pickleballMatchService.getScoreByShot(matchId, roundNumber);
        }

        Map<String, Map<String, Long>> scoreByShots = new HashMap<>();

        if (roundNumber == null) {
            throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Round number not found");
        }

        BadmintonMatch match = badmintonMatchRepo.findById(matchId)
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
                    .map(BadmintonSinglesPlayerMapping::getGuestName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(guestPlayersName)) {
                allGuests = guestPlayersName.stream()
                        .filter(Objects::nonNull)
                        .distinct().collect(Collectors.toList());
            }

            if (!CollectionUtils.isEmpty(match.getBadmintonMatchTeamPlayers())) {
                List<String> teamPlayerIds = match.getBadmintonMatchTeamPlayers().stream()
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

        Map<String, Team> teamsById = new HashMap<>();
        if (!CollectionUtils.isEmpty(allTeamIds)) {
            teamsById = teamRepo.findByIdIn(allTeamIds)
                    .stream().collect(Collectors.toMap(Team::getId, Function.identity()));
        }

        List<BadmintonLiveScore> liveScore = liveScoreRepo.findByMatchId(matchId);
        if (CollectionUtils.isEmpty(liveScore)) {
            return scoreByShots;
        }

        // Filter by round number
        liveScore = liveScore.stream()
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

        for (BadmintonLiveScore score : liveScore) {
            String shotType = StringUtils.isNotEmpty(score.getShotType()) ? score.getShotType() : "-";

            if (match.getGameFormat() == GameFormat.DOUBLES) {
                if (StringUtils.isNotEmpty(score.getTeamId())) {
                    Team team = teamsById.get(score.getTeamId());
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

    private List<BadmintonScoreTrendDto> updateScoreTrend(List<BadmintonScoreTrendDto> scoreTrends,
            List<String> playerOrTeamNames,
            BadmintonLiveScore score, GameFormat gameFormat) {

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

    private UserProfile getUserProfileWithFallback(UserProfile tournamentProfile, String requestUserId) {
        if (StringUtils.isEmpty(requestUserId)) {
            return tournamentProfile != null ? UserProfile.builder().id(tournamentProfile.getId()).build() : null;
        }

        // Always prefer requestUserId when it's provided
        return UserProfile.builder().id(requestUserId).build();
    }
}