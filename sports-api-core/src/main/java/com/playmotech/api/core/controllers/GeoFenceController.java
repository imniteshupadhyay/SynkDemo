package com.playmotech.api.core.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dto.GeoFenceRequestDto;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.GeoFenceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for GeoFence management Handles create, retrieve, update, and
 * delete operations for geo-fences
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("geo-fences")
public class GeoFenceController {

	private final GeoFenceService geoFenceService;

	@PostMapping
	public ResponseEntity<ServiceResponse> createGeoFence(@Valid @RequestBody GeoFenceRequestDto requestDto) {
		log.info("REST request to create geo-fence for academy: {}", requestDto.getAcademyId());
		ServiceResponse response = geoFenceService.createGeoFence(requestDto);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping
	public ResponseEntity<ServiceResponse> getGeoFences(
			@RequestParam(name = "academyId", required = true) String academyId) {
		log.info("REST request to get all geo-fences for academy: {}", academyId);
		ServiceResponse response = geoFenceService.getGeoFences(academyId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PutMapping
	public ResponseEntity<ServiceResponse> updateGeoFence(@Valid @RequestBody GeoFenceRequestDto requestDto) {
		log.info("REST request to update geo-fence: {} for academy: {}", requestDto.getGeoFenceId(),
				requestDto.getAcademyId());
		ServiceResponse response = geoFenceService.updateGeoFence(requestDto);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@DeleteMapping
	public ResponseEntity<ServiceResponse> deleteGeoFence(@RequestParam(name = "geoFenceId") Long geoFenceId) {
		log.info("REST request to delete geo-fence: {}.", geoFenceId);
		ServiceResponse response = geoFenceService.deleteGeoFence(geoFenceId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}
}
