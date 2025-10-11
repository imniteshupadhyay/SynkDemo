package com.playmotech.api.core.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CertificateRequest;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.CertificateService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequestMapping("web/certificate")
@RequiredArgsConstructor
public class WebCertificateController {

	private final CertificateService certificateService;

	private final AcademyDomainUtil academyDomainUtil;

	@Autowired
	private ObjectMapper objectMapper;

	@GetMapping("players")
	public ResponseEntity<ServiceResponse> getPlayers(HttpServletRequest request,
			@RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
			@RequestParam(name = "academyId", required = false) String academyId,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "programIds", required = false) List<String> programIds) {
		try {

			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = request.getHeader("origin");
			}

			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			// Build filter object
			GenericFilter filter = GenericFilter.builder().userId(currentUser.getId()).notDeleted(active)
					.academyId(academyId).academyIds(academyIds).programIds(programIds).domainUrl(academyDomain)
					.build();

			ServiceResponse response = certificateService.getPlayers(filter);
			return ResponseEntity.status(response.getHttpStatus()).body(response);

		} catch (ResourceException e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("list")
	public ResponseEntity<ServiceResponse> getCertificates(
			@RequestParam(name = "userId", required = false) String userId) {
		// Build filter object
		GenericFilter filter = GenericFilter.builder().userId(userId).build();
		ServiceResponse response = certificateService.getCertificates(filter);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping(value = "store")
	public ResponseEntity<ServiceResponse> storeCertificates(@RequestPart("request") String requestJson,
			@RequestPart(value = "file", required = false) MultipartFile file) {

		try {
			Map<String, Object> requestMap = objectMapper.readValue(requestJson,
					new TypeReference<Map<String, Object>>() {
					});
			CertificateRequest request = objectMapper.convertValue(requestMap, CertificateRequest.class);

			ServiceResponse response = certificateService.storeCertificates(request, file);
			return ResponseEntity.status(response.getHttpStatus()).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}
}