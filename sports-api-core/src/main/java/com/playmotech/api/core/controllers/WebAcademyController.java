package com.playmotech.api.core.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.AcademyService;
import com.playmotech.api.core.services.IAcademyService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequestMapping("web/academy")
@RequiredArgsConstructor
public class WebAcademyController {

	private final AcademyService academyService;
	private final IAcademyService iAcademyService;

	@GetMapping("list")
	public ResponseEntity<ServiceResponse> getAllAcademies(HttpServletRequest request) {
		String domainUrl = request.getHeader("origin");
		ServiceResponse response = academyService.getAllAcademies(domainUrl);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping("config")
	public ResponseEntity<Response<Map<String, String>>> getOrgConfig(HttpServletRequest request)
			throws ResourceException {
		try {
			Map<String, String> orgConfig = iAcademyService.getOrgConfigByDomainUrl(request);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<Map<String, String>>builder()
							.status(HttpStatus.OK.value())
							.message("Organization configuration retrieved successfully")
							.body(orgConfig)
							.build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<Map<String, String>>builder()
							.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}
}
