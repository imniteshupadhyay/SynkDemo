package com.playmotech.api.core.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.constants.MatchStatus;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dto.BadmintonMatchDetailDto;
import com.playmotech.api.core.dto.BadmintonMatchDetailRequestDto;
import com.playmotech.api.core.dto.BadmintonMatchDto;
import com.playmotech.api.core.dto.BadmintonScoreTrendDto;
import com.playmotech.api.core.dto.CreateBadmintonMatchDto;
import com.playmotech.api.core.dto.PaginatedResponse;
import com.playmotech.api.core.dto.RealTimeScoreUpdateRequestDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.UpdateBadmintonMatchDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IBadmintonMatchService;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel
 **/

@Slf4j
@RestController
@RequestMapping("/matches/badminton")
@AllArgsConstructor
public class BadmintonMatchController extends BaseController {

        private final IBadmintonMatchService badmintonMatchService;
        private final RedisTemplate<String, Object> redisTemplate;

        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDto>> createMatch(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestBody @Valid CreateBadmintonMatchDto createBadmintonMatchDto) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        BadmintonMatchDto badmintonMatchDto = badmintonMatchService.createMatch(currentUser.getUserId(),
                                        createBadmintonMatchDto, sport);
                        return ResponseEntity.status(HttpStatus.CREATED).body(Response.<BadmintonMatchDto>builder()
                                        .status(HttpStatus.CREATED.value()).message("success").body(badmintonMatchDto)
                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonMatchDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @GetMapping("paginated")
        public ResponseEntity<?> getMatchesPaginated(
                        @RequestParam(value = "matchStatus", required = false) MatchStatus matchStatus,
                        @RequestParam(value = "tournamentId", required = false) String tournamentId,
                        @RequestParam(value = "t", required = false) String searchTxt,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestParam(defaultValue = "0", required = false) Integer page,
                        @RequestParam(defaultValue = "10", required = false) Integer size,
                        @RequestParam(defaultValue = "createdAtTimestampUtc", required = false) String orderBy,
                        @RequestParam(defaultValue = "desc", required = false) String sortBy) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        PaginatedResponse<BadmintonMatchDto> badmintonMatchDtos;

                        GenericFilter filter = new GenericFilter();
                        filter.setPageSize(size.shortValue());
                        filter.setOrderBy(orderBy);
                        filter.setSearch(searchTxt);
                        filter.setCurrentPage(page.shortValue());
                        filter.setMatchStatus(matchStatus);
                        filter.setAcademyId(null);
                        filter.setTournamentId(tournamentId);
                        filter.setAscending(sortBy.equalsIgnoreCase("asc"));

                        badmintonMatchDtos = badmintonMatchService.getMatchesByStatusSpecification(
                                        currentUser.getUserId(), filter, sport);

                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(badmintonMatchDtos);
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonMatchDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<List<BadmintonMatchDto>>> getMatches(
                        @RequestParam(value = "matchStatus", required = false) MatchStatus matchStatus,
                        @RequestParam(value = "tournamentId", required = false) String tournamentId,
                        @RequestParam(value = "t", required = false) String searchTxt,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        List<BadmintonMatchDto> badmintonMatchDtos;

                        if (matchStatus != null) {
                                badmintonMatchDtos = badmintonMatchService.getMatchesByStatus(
                                                currentUser.getUserId(),
                                                null,
                                                matchStatus, tournamentId, searchTxt, sport);
                        } else {
                                badmintonMatchDtos = badmintonMatchService.getAllMatches(
                                                currentUser.getUserId(), null,
                                                tournamentId,
                                                searchTxt, sport);
                        }
                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<List<BadmintonMatchDto>>builder()
                                                        .status(HttpStatus.OK.value()).message("success")
                                                        .body(badmintonMatchDtos)
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<List<BadmintonMatchDto>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                } catch (Throwable e) {
                        log.error("Failed to get matches.", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Response.<List<BadmintonMatchDto>>builder()
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .message("Failed to get matches.").build());
                }
        }

        @PutMapping(value = "/{matchId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDto>> updateMatch(@PathVariable("matchId") String matchId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestBody UpdateBadmintonMatchDto updateBadmintonMatchDto) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        return ResponseEntity.status(HttpStatus.OK).body(Response.<BadmintonMatchDto>builder()
                                        .status(HttpStatus.OK.value()).message("success")
                                        .body(badmintonMatchService.updateMatch(currentUser.getUserId(), matchId,
                                                        updateBadmintonMatchDto,
                                                        sport))
                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonMatchDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                } catch (Throwable e) {
                        log.error("Failed to update matche.", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Response.<BadmintonMatchDto>builder()
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .message("Failed to Update matche.").build());
                }
        }

        @GetMapping(value = "/{matchId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDto>> getMatch(@PathVariable("matchId") String matchId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<BadmintonMatchDto>builder().status(HttpStatus.OK.value())
                                                        .message("success")
                                                        .body(badmintonMatchService.getMatch(currentUser.getUserId(),
                                                                        matchId, sport))
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonMatchDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                } catch (Throwable e) {
                        log.error("Failed to get matche.", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Response.<BadmintonMatchDto>builder()
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .message("Failed to get match.").build());
                }
        }

        @GetMapping(value = "/{matchId}/status/end", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDto>> endMatch(@PathVariable("matchId") String matchId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        badmintonMatchService.updateMatchStatus(currentUser.getUserId(), matchId, MatchStatus.ENDED,
                                        sport);
                        return ResponseEntity.status(HttpStatus.OK).body(
                                        Response.<BadmintonMatchDto>builder().status(HttpStatus.OK.value())
                                                        .message("success").build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonMatchDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @GetMapping(value = "/{matchId}/status/start", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDto>> startMatch(@PathVariable("matchId") String matchId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        badmintonMatchService.updateMatchStatus(currentUser.getUserId(), matchId,
                                        MatchStatus.IN_PROGRESS, sport);
                        return ResponseEntity.status(HttpStatus.OK).body(
                                        Response.<BadmintonMatchDto>builder().status(HttpStatus.OK.value())
                                                        .message("success").build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonMatchDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @PostMapping(value = "/{matchId}/scores", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDetailDto>> submitScores(@PathVariable("matchId") String matchId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestBody BadmintonMatchDetailRequestDto badmintonMatchDetailRequestDto) {
                try {
                        BadmintonMatchDetailDto badmintonMatchDetailDto = badmintonMatchService.submitMatchDetails(
                                        matchId,
                                        badmintonMatchDetailRequestDto, sport);
                        return ResponseEntity.status(HttpStatus.OK).body(Response.<BadmintonMatchDetailDto>builder()
                                        .status(HttpStatus.OK.value()).message("success").body(badmintonMatchDetailDto)
                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonMatchDetailDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                } catch (Throwable e) {
                        log.error("Failed to update match scores.", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Response.<BadmintonMatchDetailDto>builder()
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .message("Failed to Update match scores.").build());
                }
        }

        // @PostMapping(value = "/{matchId}/scores/stream", consumes =
        // MediaType.APPLICATION_JSON_VALUE, produces =
        // MediaType.APPLICATION_JSON_VALUE)
        // public ResponseEntity<Response<BadmintonMatchDetailDto>>
        // submitScore(@PathVariable("matchId") String matchId,
        // @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false)
        // Sports sport,
        // @RequestBody BadmintonScoreRequestDto badmintonScoreRequestDto) {
        // try {
        // badmintonMatchService.submitScore(matchId, badmintonScoreRequestDto, sport);
        // return
        // ResponseEntity.status(HttpStatus.OK).body(Response.<BadmintonMatchDetailDto>builder()
        // .status(HttpStatus.OK.value()).message("success").build());
        // } catch (ResourceException e) {
        // return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
        // .body(Response.<BadmintonMatchDetailDto>builder().status(e.getErrorCodes().getCustomError())
        // .message(e.getMessage()).build());
        // } catch (Throwable e) {
        // log.error("Failed to update match scores.", e);
        // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        // .body(Response.<BadmintonMatchDetailDto>builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        // .message("Failed to Update match scores.").build());
        // }
        // }

        @GetMapping(value = "/{matchId}/scores", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDetailDto>> getScores(@PathVariable("matchId") String matchId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        BadmintonMatchDetailDto badmintonMatchDetailDto = badmintonMatchService.getMatchDetails(matchId,
                                        sport);
                        return ResponseEntity.status(HttpStatus.OK).body(Response.<BadmintonMatchDetailDto>builder()
                                        .status(HttpStatus.OK.value()).message("success").body(badmintonMatchDetailDto)
                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonMatchDetailDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                } catch (Throwable e) {
                        log.error("Failed to get match scores.", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Response.<BadmintonMatchDetailDto>builder()
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .message("Failed to Update match scores.").build());
                }
        }

        @DeleteMapping(value = "/{matchId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDto>> deleteMatch(@PathVariable("matchId") String matchId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        badmintonMatchService.deleteMatch(matchId, sport);
                        return ResponseEntity.status(HttpStatus.OK).body(
                                        Response.<BadmintonMatchDto>builder().status(HttpStatus.OK.value())
                                                        .message("success").build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonMatchDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @PostMapping(value = "/{matchId}/scores/real-time", consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<?>> updateRealTimeScore(
                        @PathVariable String matchId,
                        @Valid @RequestBody RealTimeScoreUpdateRequestDto requestDto) {
                try {
                        badmintonMatchService.updateRealTimeScore(matchId, requestDto);
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.builder().status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage())
                                                        .build());
                }
                return ResponseEntity
                                .ok(Response.builder().body(null).status(HttpStatus.OK.value()).message("success")
                                                .build());
        }

        @GetMapping("/{matchId}/scores/history")
        public ResponseEntity<Response<List<Object>>> getMatchScoreHistory(@PathVariable String matchId) {
                log.info("Retrieving score history for match: {}", matchId);

                String redisKey = "match:scores:" + matchId + ":history";
                List<Object> scoreHistory = new ArrayList<>();

                try {
                        // Get the entire list from Redis
                        Long size = redisTemplate.opsForList().size(redisKey);
                        if (size != null && size > 0) {
                                scoreHistory = redisTemplate.opsForList().range(redisKey, 0, -1);
                        }

                        return ResponseEntity.ok(Response.<List<Object>>builder()
                                        .body(scoreHistory)
                                        .status(HttpStatus.OK.value())
                                        .message("Score history retrieved successfully")
                                        .build());
                } catch (Exception e) {
                        log.error("Error retrieving score history from Redis for match {}: {}", matchId,
                                        e.getMessage());
                        return ResponseEntity.ok(Response.<List<Object>>builder()
                                        .body(new ArrayList<>())
                                        .status(HttpStatus.OK.value())
                                        .message("Failed to retrieve score history")
                                        .build());
                }
        }

        @GetMapping("/{matchId}/scores/trend")
        public ResponseEntity<Response<List<BadmintonScoreTrendDto>>> getMatchScoreTrend(@PathVariable String matchId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestParam Long roundNumber) {

                log.info("Retrieving score tread for match: {}", matchId);

                try {
                        List<BadmintonScoreTrendDto> scoreTrend = badmintonMatchService.getScoreTrend(matchId,
                                        roundNumber, sport);
                        return ResponseEntity.ok(Response.<List<BadmintonScoreTrendDto>>builder()
                                        .body(scoreTrend)
                                        .status(HttpStatus.OK.value())
                                        .message("Score trend retrieved successfully")
                                        .build());
                } catch (Exception e) {
                        log.error("Error retrieving score trend for match {}", matchId, e);
                        return ResponseEntity.ok(Response.<List<BadmintonScoreTrendDto>>builder()
                                        .body(new ArrayList<>())
                                        .status(HttpStatus.OK.value())
                                        .message("Failed to retrieve score trend")
                                        .build());
                }
        }

        @GetMapping("/{matchId}/scores/shots")
        public ResponseEntity<Response<Map<String, Map<String, Long>>>> getMatchScoreByShotType(
                        @PathVariable String matchId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestParam Long roundNumber) {
                log.info("Retrieving score for match: {}", matchId);

                try {
                        Map<String, Map<String, Long>> scoreByShotType = badmintonMatchService.getScoreByShot(matchId,
                                        roundNumber,
                                        sport);

                        return ResponseEntity.ok(Response.<Map<String, Map<String, Long>>>builder()
                                        .body(scoreByShotType)
                                        .status(HttpStatus.OK.value())
                                        .message("Score retrieved successfully")
                                        .build());
                } catch (Exception e) {
                        log.error("Error retrieving score for match {}", matchId, e);
                        return ResponseEntity.ok(Response.<Map<String, Map<String, Long>>>builder()
                                        .body(new HashMap<>())
                                        .status(HttpStatus.OK.value())
                                        .message("Failed to retrieve score")
                                        .build());
                }
        }
}
