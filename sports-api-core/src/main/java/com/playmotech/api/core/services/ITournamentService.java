package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.TournamentStatus;
import com.playmotech.api.core.exceptions.ResourceException;

public interface ITournamentService<R, T, U> {
    T createTournament(String userId, String academyId, R request, Sports sport) throws ResourceException;

    T createTournament(String userId, R request, Sports sport) throws ResourceException;

    List<T> getAllTournaments(String userId, String academyId, TournamentStatus status, String searchTxt, Sports sport)
            throws ResourceException;

    T getTournament(String userId, String academyId, String tournamentId, Sports sport) throws ResourceException;

    void deleteTournament(String academyId, String tournamentId, Sports sport) throws ResourceException;

    void deleteTournament(String tournamentId, Sports sport) throws ResourceException;

    T updateTournament(String userId, String academyId, String matchId, U updateRequest, Sports sport) throws ResourceException;

    T updateTournament(String userId, String matchId, U updateRequest, Sports sport) throws ResourceException;
}
