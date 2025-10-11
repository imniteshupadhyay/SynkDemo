package com.playmotech.api.core.controllers;

import static com.playmotech.api.core.response.ApiResponse.INVALID_REQUEST;
import static com.playmotech.api.core.response.ResponseBuilder.badRequestEntity;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.constants.RankLevel;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.AssessmentResultService;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("assessments/results")
public class AssessmentResultsController {

	private final AssessmentResultService resultService;

	@GetMapping("kpis")
	public ResponseEntity<ServiceResponse> getKpis(@RequestParam String assessmentId) {
		ServiceResponse response = resultService.getKpis(assessmentId);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("leaderboard")
	public ResponseEntity<ServiceResponse> getResults(@RequestParam String assessmentId) {
		ServiceResponse response = resultService.getResults(assessmentId);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("leaderboard/global")
	public ResponseEntity<ServiceResponse> getGlobalResults(HttpServletRequest request,
			@RequestParam(name = "sport", required = false) String sport,
			@RequestParam(name = "ranking", defaultValue = "GLOBAL", required = false) RankLevel ranking,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories,
			@RequestParam(name = "gender", required = false) List<String> genders,
			@RequestParam(name = "orderBy", defaultValue = "assessmentRank", required = false) String orderBy,
			@RequestParam(name = "search", defaultValue = "", required = false) String search,
			@RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
			@RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
			@RequestParam(name = "ascending", defaultValue = "true", required = false) boolean ascending,
			@RequestParam(name = "pageable", defaultValue = "true", required = false) boolean pageable) {

		if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
			return badRequestEntity(INVALID_REQUEST);
		}

		String domainUrl = request.getHeader("origin");

		GenericFilter filter = GenericFilter.builder().domainUrl(domainUrl).ranking(ranking).isPageable(pageable)
				.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
				.academyIds(academyIds).sport(sport).ageCategory(ageCategories).gender(genders).build();

		ServiceResponse response = resultService.getGlobalResults(filter);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@PostMapping("calculate")
	public ResponseEntity<ServiceResponse> calculateScores(@RequestParam String assessmentId) {
		ServiceResponse response = resultService.calculateScores(assessmentId);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@PostMapping("calculate/global")
	public ResponseEntity<ServiceResponse> calculateGlobalScores() {
		ServiceResponse response = resultService.calculateGlobalScores();
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

}
