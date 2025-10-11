package com.playmotech.api.core.helper;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.validation.Validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class AcademyHelper {

	private final AcademyRepo academyRepository;
	private final Validation validation;

	public ServiceResponse fetchAcademyById(String academyId) {
		log.info(ApiResponse.VALIDATING_ID.getMessage());

		if (!validation.isValidStringId(academyId)) {
			log.warn(ApiResponse.INVALID_ACADEMY_ID.getMessage());
			return ResponseBuilder.badRequest(ApiResponse.INVALID_ACADEMY_ID);
		}

		Optional<Academy> academy = academyRepository.findByIdAndInactiveFalse(academyId);

		return academy.map(a -> {
			log.info(ApiResponse.ACADEMY_FETCHED.getMessage());
			return ResponseBuilder.success(a, ApiResponse.ACADEMY_FETCHED, HttpStatus.OK);
		}).orElseGet(() -> {
			log.warn(ApiResponse.ACADEMY_NOT_FOUND.getMessage());
			return ResponseBuilder.notFound(ApiResponse.ACADEMY_NOT_FOUND);
		});
	}
}
