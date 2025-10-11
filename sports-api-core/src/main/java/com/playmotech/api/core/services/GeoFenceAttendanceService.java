package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.AutoCheckOutRequestDto;
import com.playmotech.api.core.dto.GeoFenceAttendanceCheckRequest;
import com.playmotech.api.core.response.ServiceResponse;

public interface GeoFenceAttendanceService {

	ServiceResponse checkIn(String coachId, GeoFenceAttendanceCheckRequest requestDto);

	ServiceResponse checkOut(String coachId, GeoFenceAttendanceCheckRequest requestDto);

	ServiceResponse autoCheckOut(String coachId, AutoCheckOutRequestDto requestDto);

	ServiceResponse verifyCoachLocation(String coachId, String academyId, Double latitude, Double longitude);
}
