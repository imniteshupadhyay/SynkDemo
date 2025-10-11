package com.playmotech.api.core.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dto.TournamentPerformanceDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IBadmintonTournamentService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel
 **/

@Slf4j
@RestController
@RequestMapping("/matches/badminton/stats")
@AllArgsConstructor
public class BadmintonStatsController extends BaseController {

    private final IBadmintonTournamentService tournamentService;

    @GetMapping("/tournaments/{tournamentId}")
    public TournamentPerformanceDto getTournamentStats(@PathVariable(name = "tournamentId") String tournamentId,
            @RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport)
            throws ResourceException {
        return tournamentService.getTournamentPerformance(tournamentId, sport);
    }
}
