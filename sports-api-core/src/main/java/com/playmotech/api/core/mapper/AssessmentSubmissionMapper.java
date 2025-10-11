package com.playmotech.api.core.mapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Assessment;
import com.playmotech.api.core.dao_postgres.AssessmentParameter;
import com.playmotech.api.core.dao_postgres.AssessmentParameterConfig;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission.SubmissionStatus;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmissionParameter;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AssessmentSubmissionDto;
import com.playmotech.api.core.dto.AssessmentSubmissionParameterDto;
import com.playmotech.api.core.response.dao.AssessmentSubmissionDao;
import com.playmotech.api.core.response.dao.AssessmentSubmissionParameterDao;

public class AssessmentSubmissionMapper {

	public static AssessmentPlayerSubmission mapDtoToEntity(AssessmentSubmissionDto dto, String userId) {
		if (dto == null)
			return null;

		AssessmentPlayerSubmission entity = new AssessmentPlayerSubmission();
		BeanUtils.copyProperties(dto, entity);

		if (dto.getSubmissionStatus() != null && dto.getSubmissionStatus().equals(SubmissionStatus.SUBMITTED)) {
			entity.setSubmittedOn(Timestamp.valueOf(LocalDateTime.now()));
		}

		entity.setCreatedBy(userId);

		// Set minimal reference entities (only IDs)
		entity.setPlayer(UserProfile.builder().id(dto.getPlayerId()).build());
		entity.setAcademy(Academy.builder().id(dto.getAcademyId()).build());
		entity.setAssessment(Assessment.builder().id(dto.getAssessmentId()).build());
		entity.setRegistration(AssessmentPlayerRegistration.builder().registrationId(dto.getRegistrationId()).build());

		// Map parameters
		if (dto.getSubmissionParameters() != null && !dto.getSubmissionParameters().isEmpty()) {
			List<AssessmentPlayerSubmissionParameter> parameters = dto.getSubmissionParameters().stream()
					.map(paramDto -> mapParamDtoToEntity(paramDto, entity)).collect(Collectors.toList());
			entity.setSubmissionParameters(parameters);
		}

		return entity;
	}

	public static AssessmentPlayerSubmission mapToExistingEntity(AssessmentSubmissionDto dto,
			AssessmentPlayerSubmission entity, String userId) {
		// Copy simple fields except id, parameters, and academyIds
		BeanUtils.copyProperties(dto, entity, "id", "submissionParameters");

		if (dto.getSubmissionStatus() != null && dto.getSubmissionStatus().equals(SubmissionStatus.SUBMITTED)) {
			entity.setSubmittedOn(Timestamp.valueOf(LocalDateTime.now()));
		}

		entity.setUpdatedBy(userId);

		// ✅ Replace parameters (mutate list, don't replace reference)
		if (dto.getSubmissionParameters() != null) {
			if (entity.getSubmissionParameters() == null) {
				entity.setSubmissionParameters(new ArrayList<>());
			} else {
				entity.getSubmissionParameters().clear();
			}

			for (AssessmentSubmissionParameterDto submissionParameterDto : dto.getSubmissionParameters()) {
				AssessmentPlayerSubmissionParameter submissionParameter = mapParamDtoToExistingEntity(
						submissionParameterDto, entity, userId);
				entity.getSubmissionParameters().add(submissionParameter);
			}
		}

		return entity;
	}

	private static AssessmentPlayerSubmissionParameter mapParamDtoToExistingEntity(AssessmentSubmissionParameterDto dto,
			AssessmentPlayerSubmission parent, String userId) {
		if (dto == null)
			return null;

		AssessmentPlayerSubmissionParameter entity = new AssessmentPlayerSubmissionParameter();
		BeanUtils.copyProperties(dto, entity);

		entity.setSubmission(parent);

		if (dto.getParameterConfigId() != null) {
			entity.setParameterConfig(
					AssessmentParameterConfig.builder().parameterId(dto.getParameterConfigId()).build());
		}

		if (dto.getParameterId() != null) {
			entity.setParameter(AssessmentParameter.builder().parameterId(dto.getParameterId()).build());
		}

		return entity;
	}

	public static AssessmentSubmissionDao mapEntityToDao(AssessmentPlayerSubmission entity) {
		if (entity == null)
			return null;

		AssessmentSubmissionDao dao = new AssessmentSubmissionDao();
		BeanUtils.copyProperties(entity, dao);

		dao.setSubmissionId(entity.getSubmissionId());
		dao.setAcademyId(entity.getAcademy() != null ? entity.getAcademy().getId() : null);
		dao.setAcademyName(entity.getAcademy() != null ? entity.getAcademy().getName() : null);
		dao.setPlayerId(entity.getPlayer() != null ? entity.getPlayer().getId() : null);
		dao.setPlayerName(entity.getPlayer() != null ? entity.getPlayer().getDisplayName() : null);
		dao.setPlayerNumber(entity.getPlayer() != null ? entity.getPlayer().getPhoneNumber() : null);
		dao.setRegistrationId(entity.getRegistration() != null ? entity.getRegistration().getRegistrationId() : null);
		dao.setRegistrationNumber(
				entity.getRegistration() != null ? entity.getRegistration().getRegistrationNumber() : null);
		dao.setAssessmentId(entity.getAssessment() != null ? entity.getAssessment().getId() : null);

		if (entity.getSubmissionParameters() != null) {
			List<AssessmentSubmissionParameterDao> paramDaos = entity.getSubmissionParameters().stream()
					.map(AssessmentSubmissionMapper::mapParamEntityToDao).collect(Collectors.toList());
			dao.setParameters(paramDaos);
		}

		return dao;
	}

	private static AssessmentPlayerSubmissionParameter mapParamDtoToEntity(AssessmentSubmissionParameterDto dto,
			AssessmentPlayerSubmission parent) {
		if (dto == null)
			return null;

		AssessmentPlayerSubmissionParameter entity = new AssessmentPlayerSubmissionParameter();
		BeanUtils.copyProperties(dto, entity);

		entity.setSubmission(parent);

		if (dto.getParameterConfigId() != null) {
			entity.setParameterConfig(
					AssessmentParameterConfig.builder().parameterId(dto.getParameterConfigId()).build());
		}

		if (dto.getParameterId() != null) {
			entity.setParameter(AssessmentParameter.builder().parameterId(dto.getParameterId()).build());
		}

		return entity;
	}

	private static AssessmentSubmissionParameterDao mapParamEntityToDao(AssessmentPlayerSubmissionParameter entity) {
		if (entity == null)
			return null;

		AssessmentSubmissionParameterDao dao = new AssessmentSubmissionParameterDao();
		BeanUtils.copyProperties(entity, dao);

		dao.setSubmissionId(entity.getSubmission() != null ? entity.getSubmission().getSubmissionId() : null);
		dao.setParameterId(entity.getParameter() != null ? entity.getParameter().getParameterId() : null);

		return dao;
	}

}
