package com.playmotech.api.core.controllers;

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

import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.TeamDto;
import com.playmotech.api.core.dto.TeamPlayerDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.ITeamsService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

/**
 * Created By: deep.patel
 **/
@RestController
@RequestMapping("/teams")
@AllArgsConstructor
public class TeamsController extends BaseController {

    private final ITeamsService teamsService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<TeamDto>> addTeam(@Valid @RequestBody TeamDto teamDto,
            @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetail currentUser = (UserDetail) authentication.getPrincipal();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Response.<TeamDto>builder().status(HttpStatus.CREATED.value()).message("success")
                            .body(teamsService.addTeam(currentUser.getUserId(), teamDto, sport)).build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<TeamDto>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<List<TeamDto>>> getTeams(
            @RequestParam(value = "t", required = false) String searchTxt,
            @RequestParam(value = "badmintonTournamentId", required = false) String badmintonTournamentId,
            @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetail currentUser = (UserDetail) authentication.getPrincipal();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Response.<List<TeamDto>>builder().status(HttpStatus.OK.value()).message("success")
                            .body(teamsService.getTeams(currentUser.getUserId(), searchTxt, badmintonTournamentId,
                                    sport))
                            .build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<TeamDto>>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @DeleteMapping(value = "/{teamId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<TeamDto>> deleteTeam(@PathVariable("teamId") String teamId,
            @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
            @RequestParam(value = "disassociateTournament", required = false, defaultValue = "false") Boolean disassociateTournament,
            @RequestParam(value = "badmintonTournamentId", required = false) String badmintonTournamentId) {
        try {
            teamsService.deleteTeam(teamId, disassociateTournament, badmintonTournamentId, sport);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Response.<TeamDto>builder().status(HttpStatus.OK.value()).message("success").build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<TeamDto>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    /**
     * Add new players to an existing team This endpoint allows team members to add
     * more players to their team
     *
     * @param teamId  Team ID
     * @param players Request containing list of players to add
     * @param sport   Sports
     * @return Updated team with new players added
     */

    @PutMapping("/{teamId}/players")
    public ResponseEntity<Response<TeamDto>> addPlayersToTeam(@PathVariable String teamId,
            @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
            @RequestBody List<TeamPlayerDto> players) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetail currentUser = (UserDetail) authentication.getPrincipal();

            TeamDto updatedTeam = teamsService.addPlayersToTeam(currentUser.getUserId(), teamId, players, sport);

            return ResponseEntity.status(HttpStatus.OK).body(Response.<TeamDto>builder().status(HttpStatus.OK.value())
                    .message("success").body(updatedTeam).build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<TeamDto>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

}
