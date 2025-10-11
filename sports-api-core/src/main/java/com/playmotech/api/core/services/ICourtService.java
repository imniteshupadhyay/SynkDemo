package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dto.CourtDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface ICourtService {
    CourtDto addCourt(String academyId, CourtDto courtDto) throws ResourceException;

    CourtDto addCourt(CourtDto courtDto) throws ResourceException;

    List<CourtDto> getCourts(String academyId, String searchTxt, Sports sport) throws ResourceException;

    List<CourtDto> getCourts(String searchTxt, Sports sport) throws ResourceException;

    CourtDto getCourt(String academyId, String courtId) throws ResourceException;

    CourtDto getCourt(String courtId) throws ResourceException;

    void deleteCourt(String academyId, String courtId) throws ResourceException;

    void deleteCourt(String courtId) throws ResourceException;
}
