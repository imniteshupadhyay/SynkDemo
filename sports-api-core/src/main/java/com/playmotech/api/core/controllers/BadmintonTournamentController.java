package com.playmotech.api.core.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.TournamentStatus;
import com.playmotech.api.core.dto.BadmintonMatchDto;
import com.playmotech.api.core.dto.BadmintonTournamentDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.MediaDto;
import com.playmotech.api.core.dto.PaginatedResponse;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.TournamentTeamMappingDto;
import com.playmotech.api.core.dto.UpdateBadmintonTournamentDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IBadmintonTournamentService;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel
 **/

@Slf4j
@RestController
@RequestMapping("/tournaments/badminton")
@AllArgsConstructor
public class BadmintonTournamentController extends BaseController {

        private final IBadmintonTournamentService badmintonTournamentService;

        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonTournamentDto>> createTournament(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestBody @Valid BadmintonTournamentDto request) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        BadmintonTournamentDto badmintonTournamentDto = badmintonTournamentService
                                        .createTournament(currentUser.getUserId(), request, sport);
                        return ResponseEntity.status(HttpStatus.CREATED).body(Response.<BadmintonTournamentDto>builder()
                                        .status(HttpStatus.CREATED.value()).message("success")
                                        .body(badmintonTournamentDto).build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonTournamentDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @GetMapping("paginated")
        public ResponseEntity<?> getTournamentsPaginated(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestParam(value = "t", required = false) String searchTxt,
                        @RequestParam(required = false) TournamentStatus status,
                        @RequestParam(defaultValue = "0", required = false) Integer page,
                        @RequestParam(defaultValue = "10", required = false) Integer size,
                        @RequestParam(defaultValue = "createdOn", required = false) String orderBy,
                        @RequestParam(defaultValue = "desc", required = false) String sortBy) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

                        GenericFilter filter = new GenericFilter();
                        filter.setPageSize(size.shortValue());
                        filter.setOrderBy(orderBy);
                        filter.setSearch(searchTxt);
                        filter.setCurrentPage(page.shortValue());
                        filter.setAcademyId(null);
                        filter.setTournamentStatus(status);
                        filter.setAscending(sortBy.equalsIgnoreCase("asc"));

                        PaginatedResponse<BadmintonTournamentDto> badmintonTournamentDtos = badmintonTournamentService
                                        .getTournamentsPaginated(currentUser.getUserId(), filter, sport);

                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(badmintonTournamentDtos);
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonMatchDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<List<BadmintonTournamentDto>>> getTournaments(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestParam(value = "status", required = false) TournamentStatus status,
                        @RequestParam(value = "t", required = false) String searchTxt) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        List<BadmintonTournamentDto> badmintonTournamentDtos = badmintonTournamentService
                                        .getAllTournaments(currentUser.getUserId(), null, status, searchTxt, sport);
                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<List<BadmintonTournamentDto>>builder()
                                                        .status(HttpStatus.OK.value()).message("success")
                                                        .body(badmintonTournamentDtos).build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<List<BadmintonTournamentDto>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @PutMapping(value = "/{tournamentId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonTournamentDto>> updateTournament(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @PathVariable("tournamentId") String tournamentId,
                        @RequestBody UpdateBadmintonTournamentDto updateBadmintonTournamentDto) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        return ResponseEntity.status(HttpStatus.OK).body(Response.<BadmintonTournamentDto>builder()
                                        .status(HttpStatus.OK.value()).message("success")
                                        .body(badmintonTournamentService
                                                        .updateTournament(currentUser.getUserId(), tournamentId,
                                                                        updateBadmintonTournamentDto, sport))
                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonTournamentDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                } catch (Throwable e) {
                        log.error("Failed to update match.", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Response.<BadmintonTournamentDto>builder()
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .message("Failed to Update matche.").build());
                }
        }

        @GetMapping(value = "/{tournamentId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonTournamentDto>> getTournament(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @PathVariable("tournamentId") String tournamentId) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<BadmintonTournamentDto>builder().status(HttpStatus.OK.value())
                                                        .message("success")
                                                        .body(badmintonTournamentService.getTournament(
                                                                        currentUser.getUserId(), null, tournamentId,
                                                                        sport))
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonTournamentDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                } catch (Throwable e) {
                        log.error("Failed to Get match.", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Response.<BadmintonTournamentDto>builder()
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .message("Failed to Get matche.").build());
                }
        }

        @DeleteMapping(value = "/{tournamentId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<BadmintonTournamentDto>> deleteTournament(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @PathVariable("tournamentId") String tournamentId) {
                try {
                        badmintonTournamentService.deleteTournament(tournamentId, sport);
                        return ResponseEntity.status(HttpStatus.OK).body(Response.<BadmintonTournamentDto>builder()
                                        .status(HttpStatus.OK.value()).message("success").build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<BadmintonTournamentDto>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @PostMapping(value = "/{tournamentId}/media")
        public ResponseEntity<Response<List<MediaDto>>> createPost(@PathVariable("tournamentId") String tournamentId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestParam(value = "file", required = false) MultipartFile[] mediaFile) throws IOException {
                try {
                        List<FileObjectDto> fileObjectDtos = new ArrayList<>();
                        if (mediaFile != null) {
                                for (MultipartFile file : mediaFile) {
                                        FileObjectDto fileObjectDto = new FileObjectDto();
                                        fileObjectDto.setOriginalFilename(file.getOriginalFilename());
                                        fileObjectDto.setContent(file.getBytes());
                                        fileObjectDto.setContentType(file.getContentType());
                                        fileObjectDtos.add(fileObjectDto);
                                }
                        }
                        List<MediaDto> mediaDtos = badmintonTournamentService.uploadGalleryMedia(tournamentId,
                                        fileObjectDtos, sport);
                        return ResponseEntity.status(HttpStatus.OK).body(Response.<List<MediaDto>>builder()
                                        .status(HttpStatus.OK.value()).message("success").body(mediaDtos).build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<List<MediaDto>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @DeleteMapping(value = "/{tournamentId}/media/{mediaId}")
        public ResponseEntity<Response> createPost(@PathVariable("tournamentId") String tournamentId,
                        @PathVariable("mediaId") Long mediaId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport)
                        throws IOException {
                try {
                        badmintonTournamentService.deleteGalleryMedia(tournamentId, mediaId, sport);
                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<List<MediaDto>>builder().status(HttpStatus.OK.value())
                                                        .message("success").build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<List<MediaDto>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @DeleteMapping(value = "/{tournamentId}/delete-autogenerated-matches")
        public ResponseEntity<Response> deleteAutoGeneratedMatches(
                        @PathVariable("tournamentId") String tournamentId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport)
                        throws IOException {
                try {
                        badmintonTournamentService.deleteAllAutogeneratedMatches(null, tournamentId, sport);
                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.<List<MediaDto>>builder().status(HttpStatus.OK.value())
                                                        .message("success").build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<List<MediaDto>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @PostMapping(value = "/{tournamentId}/players", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<List<UserProfileMinDto>>> addPlayersToTournament(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @PathVariable("tournamentId") String tournamentId, @RequestBody List<String> userIds) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        List<UserProfileMinDto> userProfileMinDtos = badmintonTournamentService
                                        .addPlayersToTournament(tournamentId, userIds, sport);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(Response.<List<UserProfileMinDto>>builder()
                                                        .status(HttpStatus.CREATED.value()).message("success")
                                                        .body(userProfileMinDtos).build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<List<UserProfileMinDto>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @DeleteMapping(value = "/{tournamentId}/players/{playerUserId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<List<UserProfileMinDto>>> removePlayersFromTournament(
                        @PathVariable("tournamentId") String tournamentId,
                        @PathVariable("playerUserId") String playerUserId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        badmintonTournamentService.deletePlayersFromTournament(tournamentId, playerUserId, sport);
                        return ResponseEntity.status(HttpStatus.ACCEPTED)
                                        .body(Response.<List<UserProfileMinDto>>builder()
                                                        .status(HttpStatus.ACCEPTED.value())
                                                        .message(HttpStatus.ACCEPTED.getReasonPhrase()).build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<List<UserProfileMinDto>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @GetMapping(value = "/{tournamentId}/players", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Response<List<UserProfileMinDto>>> getTournamentPlayers(
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestParam(value = "t", required = false) String searchTxt,
                        @PathVariable("tournamentId") String tournamentId) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        List<UserProfileMinDto> userProfileMinDtos = badmintonTournamentService
                                        .getTournanentPlayers(searchTxt, tournamentId, sport);
                        return ResponseEntity.status(HttpStatus.OK).body(Response.<List<UserProfileMinDto>>builder()
                                        .status(HttpStatus.OK.value()).message("success").body(userProfileMinDtos)
                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<List<UserProfileMinDto>>builder()
                                                        .status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }

        @PostMapping("/{tournamentId}/teams")
        public ResponseEntity<Response<?>> mapTeamsToTournament(@PathVariable String tournamentId,
                        @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
                        @RequestBody TournamentTeamMappingDto teamPlayerMappingDtos) {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
                        badmintonTournamentService.mapTeamsToTournament(currentUser.getUserId(), tournamentId,
                                        teamPlayerMappingDtos, null, sport);

                        return ResponseEntity.status(HttpStatus.OK)
                                        .body(Response.builder().status(HttpStatus.OK.value()).message("success")
                                                        .build());
                } catch (ResourceException e) {
                        return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                                        .body(Response.<Void>builder().status(e.getErrorCodes().getCustomError())
                                                        .message(e.getMessage()).build());
                }
        }
}
