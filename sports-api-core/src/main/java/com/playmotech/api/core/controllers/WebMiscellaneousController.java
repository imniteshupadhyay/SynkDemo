package com.playmotech.api.core.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.MiscellaneousService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("web")
public class WebMiscellaneousController {

	private final MiscellaneousService miscellaneousService;

	@GetMapping("age-category/list")
	public ResponseEntity<ServiceResponse> getAlLAgeCategories() {
		ServiceResponse response = miscellaneousService.getAllAgeCategories();
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("gender/list")
	public ResponseEntity<ServiceResponse> getAllGenders() {
		ServiceResponse response = miscellaneousService.getAllGenders();
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("sports/list")
	public ResponseEntity<ServiceResponse> getAllSports(
			@RequestParam(name = "academyId", required = false) String academyId) {

		ServiceResponse response;
		if (academyId != null && !academyId.isBlank()) {
			response = miscellaneousService.getSportsByAcademy(academyId);
		} else {
			response = miscellaneousService.getAllSports();
		}

		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("sports/all")
	public ResponseEntity<ServiceResponse> getAllSportsByAcademies(
			@RequestParam(name = "academyIds", required = true) List<String> academyIds) {

		ServiceResponse response = miscellaneousService.getSportsByAcademies(academyIds);

		return new ResponseEntity<>(response, response.getHttpStatus());
	}

}
