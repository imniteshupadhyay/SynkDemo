package com.playmotech.api.core.validation;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission.SubmissionStatus;
import com.playmotech.api.core.dto.AssessmentSubmissionDto;
import com.playmotech.api.core.dto.AssessmentSubmissionParameterDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class AssessmentSubmissionValidation {

	private static final String ID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
	private static final String URL_REGEX = "^(https?|ftp)://[^\s/$.?#].[^\s]*$";

	public boolean validate(AssessmentSubmissionDto submissionDto) {
		if (submissionDto == null) {
			log.warn("Submission DTO cannot be null");
			return false;
		}

		return validateSubmissionFields(submissionDto) && validateParameters(submissionDto.getSubmissionParameters());
	}

	private boolean validateSubmissionFields(AssessmentSubmissionDto dto) {
		return isNotEmpty(dto.getAssessmentId(), dto.getAcademyId(), dto.getPlayerId(), dto.getSubmissionStatus())
				&& isValidId(dto.getAssessmentId()) && isValidId(dto.getAcademyId()) && isValidId(dto.getPlayerId())
				&& validateSubmissionStatus(dto.getSubmissionStatus());
	}

	private boolean validateParameters(List<AssessmentSubmissionParameterDto> parameters) {
		if (CollectionUtils.isEmpty(parameters)) {
			log.warn("Submission parameters cannot be empty");
			return false;
		}

		return parameters.stream()
				.allMatch(param -> isNotEmpty(param.getParameterId(), param.getParameterName(),
						param.getParameterType(), param.getParameterWeight(), param.getComparisonType())
						&& isValidId(param.getParameterId()) && param.getParameterWeight() > 0
						&& (param.getVideoUrl() == null || isValidRegex(param.getVideoUrl(), URL_REGEX)));
	}

	private boolean validateSubmissionStatus(SubmissionStatus status) {
		if (status == null) {
			log.warn("Submission status cannot be null");
			return false;
		}
		return true;
	}

	// --- Helper Methods ---

	private static boolean isValidId(String id) {
		return id != null && id.matches(ID_REGEX);
	}

	private static boolean isValidRegex(String value, String regex) {
		return value == null || value.matches(regex);
	}

	private static boolean isNotEmpty(Object... fields) {
		return Arrays.stream(fields).allMatch(field -> {
			if (field instanceof String) {
				return !((String) field).isBlank();
			}
			return field != null;
		});
	}
}
