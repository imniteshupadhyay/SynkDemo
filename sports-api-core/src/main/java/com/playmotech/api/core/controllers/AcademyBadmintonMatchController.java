package com.playmotech.api.core.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
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
import com.playmotech.api.core.constants.StreamingStatus;
import com.playmotech.api.core.dto.BadmintonMatchDetailDto;
import com.playmotech.api.core.dto.BadmintonMatchDetailRequestDto;
import com.playmotech.api.core.dto.BadmintonMatchDto;
import com.playmotech.api.core.dto.CreateBadmintonMatchDto;
import com.playmotech.api.core.dto.PaginatedResponse;
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
@RequestMapping("/matches/academies/{academyId}/badminton")
@AllArgsConstructor
public class AcademyBadmintonMatchController extends BaseController {

        private final IBadmintonMatchService badmintonMatchService;

        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDto>> createMatch(@PathVariable("academyId") String academyId,
                        @RequestBody @Valid CreateBadmintonMatchDto createBadmintonMatchDto,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        BadmintonMatchDto badmintonMatchDto = badmintonMatchService.createMatch(currentUser.getUserId(),
                                        academyId,
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
                        @PathVariable("academyId") String academyId,
                        @RequestParam(value = "matchStatus", required = false) MatchStatus matchStatus,
                        @RequestParam(value = "tournamentId", required = false) String tournamentId,
                        @RequestParam(value = "t", required = false) String searchText,
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
                        filter.setSearch(searchText);
                        filter.setCurrentPage(page.shortValue());
                        filter.setMatchStatus(matchStatus);
                        filter.setAcademyId(academyId);
                        filter.setTournamentId(tournamentId);
                        filter.setAcademyId(academyId);
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
                        @PathVariable("academyId") String academyId,
                        @RequestParam(value = "matchStatus", required = false) MatchStatus matchStatus,
                        @RequestParam(value = "tournamentId", required = false) String tournamentId,
                        @RequestParam(value = "t", required = false) String searchText,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        List<BadmintonMatchDto> badmintonMatchDtos;

                        if (matchStatus != null) {
                                badmintonMatchDtos = badmintonMatchService.getMatchesByStatus(
                                                currentUser.getUserId(),
                                                academyId,
                                                matchStatus, tournamentId, searchText, sport);
                        } else {
                                badmintonMatchDtos = badmintonMatchService.getAllMatches(
                                                currentUser.getUserId(),
                                                academyId,
                                                tournamentId, searchText, sport);
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
        public ResponseEntity<Response<BadmintonMatchDto>> updateMatch(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @PathVariable("academyId") String academyId,
                        @PathVariable("matchId") String matchId,
                        @RequestBody UpdateBadmintonMatchDto updateBadmintonMatchDto) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        return ResponseEntity.status(HttpStatus.OK).body(Response.<BadmintonMatchDto>builder()
                                        .status(HttpStatus.OK.value()).message("success").body(badmintonMatchService
                                                        .updateMatch(currentUser.getUserId(), academyId, matchId,
                                                                        updateBadmintonMatchDto, sport))
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
        public ResponseEntity<Response<BadmintonMatchDto>> getMatch(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @PathVariable("academyId") String academyId,
                        @PathVariable("matchId") String matchId) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<BadmintonMatchDto>builder().status(HttpStatus.OK.value())
                                                        .message("success")
                                                        .body(badmintonMatchService.getMatch(currentUser.getUserId(),
                                                                        academyId, matchId, sport))
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
                                                        .message("Failed to get matche.").build());
                }
        }

        @GetMapping(value = "/{matchId}/stream", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<Map<String, String>>> stream(
                        @PathVariable("matchId") String matchId,
                        @RequestParam(name = "streamingStatus") StreamingStatus streamingStatus,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<Map<String, String>>builder().status(HttpStatus.OK.value())
                                                        .message("success")
                                                        .body(badmintonMatchService.streamMatch(matchId,
                                                                        streamingStatus, sport))
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<Map<String, String>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                } catch (Throwable e) {
                        log.error("Failed to update matche.", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Response.<Map<String, String>>builder()
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .message("Failed to Update matche.").build());
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

        @GetMapping(value = "/{matchId}/status/end", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDto>> endMatch(@PathVariable("academyId") String academyId,
                        @PathVariable("matchId") String matchId,
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
        public ResponseEntity<Response<BadmintonMatchDto>> startMatch(@PathVariable("academyId") String academyId,
                        @PathVariable("matchId") String matchId,
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

        @DeleteMapping(value = "/{matchId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonMatchDto>> deleteMatch(@PathVariable("academyId") String academyId,
                        @PathVariable("matchId") String matchId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        badmintonMatchService.deleteMatch(academyId, matchId, sport);
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

}
