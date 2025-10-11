package com.playmotech.api.core.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dto.AutoCheckOutRequestDto;
import com.playmotech.api.core.dto.GeoFenceAttendanceCheckRequest;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.GeoFenceAttendanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("geo-fences/attendance")
public class GeoFenceAttendanceController {

	private final GeoFenceAttendanceService geoFenceAttendanceService;

	@PostMapping("check-in")
	public ResponseEntity<ServiceResponse> checkIn(@Valid @RequestBody GeoFenceAttendanceCheckRequest requestDto) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		String coachId = currentUser.getUserId();

		log.info("REST request to check-in coach: {} for academy: {} and program: {}", coachId,
				requestDto.getAcademyId(), requestDto.getProgramId());

		ServiceResponse response = geoFenceAttendanceService.checkIn(coachId, requestDto);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping("check-out")
	public ResponseEntity<ServiceResponse> checkOut(@Valid @RequestBody GeoFenceAttendanceCheckRequest requestDto) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		String coachId = currentUser.getUserId();

		log.info("REST request to check-out coach: {} for academy: {} and program: {}", coachId,
				requestDto.getAcademyId(), requestDto.getProgramId());

		ServiceResponse response = geoFenceAttendanceService.checkOut(coachId, requestDto);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Automatic check-out endpoint Triggered when coach exits a geo-fence
	 */
	@PostMapping("auto-checkout")
	public ResponseEntity<ServiceResponse> autoCheckOut(@Valid @RequestBody AutoCheckOutRequestDto requestDto) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		String coachId = currentUser.getUserId();

		log.info("REST request for auto check-out of coach: {} at location ({}, {})", coachId, requestDto.getLatitude(),
				requestDto.getLongitude());

		ServiceResponse response = geoFenceAttendanceService.autoCheckOut(coachId, requestDto);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Verify if coach's location is within their academy's geo-fence
	 */
	@GetMapping("verify-location")
	public ResponseEntity<ServiceResponse> verifyCoachLocation(
			@RequestParam(name = "academyId", required = false) String academyId,
			@RequestParam(name = "latitude") Double latitude, @RequestParam(name = "longitude") Double longitude) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		String coachId = currentUser.getUserId();

		log.info("REST request to verify location of coach: {} for academy: {} at ({}, {})", coachId, academyId,
				latitude, longitude);

		ServiceResponse response = geoFenceAttendanceService.verifyCoachLocation(coachId, academyId, latitude,
				longitude);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}
}
