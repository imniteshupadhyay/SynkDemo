package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.constants.MatchStatus;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.exceptions.ResourceException;

public interface IMatchService<R, T, U> {
    T createMatch(String userId, String academyId, R request, Sports sport) throws ResourceException;

    T createMatch(String userId, R request, Sports sport) throws ResourceException;

    List<T> getMatchesByStatus(String userId, String academyId, MatchStatus matchStatus, String tournamentId, String searchText, Sports sport)
            throws ResourceException;

    List<T> getAllMatches(String userId, String academyId, String tournamentId, String searchText, Sports sport) throws ResourceException;

    T getMatch(String userId, String academyId, String matchId, Sports sport) throws ResourceException;

    T getMatch(String userId, String matchId, Sports sport) throws ResourceException;

    void updateMatchStatus(String userId, String matchId, MatchStatus matchStatus, Sports sport) throws ResourceException;

    void deleteMatch(String academyId, String matchId, Sports sport) throws ResourceException;

    void deleteMatch(String matchId, Sports sport) throws ResourceException;

    T updateMatch(String userId, String academyId, String matchId, U updateRequest, Sports sport) throws ResourceException;

    T updateMatch(String userId, String matchId, U updateRequest, Sports sport) throws ResourceException;
}
