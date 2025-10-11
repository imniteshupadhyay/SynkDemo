package com.playmotech.api.core.validation;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.playmotech.api.core.dto.VideoAnalysisDto;
import com.playmotech.api.core.dto.VideoAnalyzerDto;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CourseRepo;
import com.playmotech.api.core.repo.UserProfileRepo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VideoAnalyzerValidation {

	private final UserProfileRepo userProfileRepository;
	private final CourseRepo courseRepository;
	private final AcademyRepo academyRepository;

	private static final String MEDIA_URL_REGEX = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
	private static final String TITLE_REGEX = "^.{5,100}$";

	public boolean validate(VideoAnalyzerDto analyzerDto) {
		return isNotEmpty(analyzerDto.getTitle())
				// && validateMediaUrl(analyzerDto.getMediaUrl())
				&& validateTitle(analyzerDto.getTitle())
				&& validateRelationships(analyzerDto.getPlayerId(), analyzerDto.getCoachId(), analyzerDto.getCourseId(),
						analyzerDto.getAcademyId());
	}

	public boolean validateForUpdate(VideoAnalyzerDto analyzerDto) {
		return validate(analyzerDto) && validateAnalysisComments(analyzerDto.getAnalysis());
	}

	private boolean validateMediaUrl(String mediaUrl) {
		return isValidLengthRegex(mediaUrl, MEDIA_URL_REGEX) && isValidMandatoryLength(mediaUrl, 10, 2048);
	}

	private boolean validateTitle(String title) {
		return isValidLengthRegex(title, TITLE_REGEX);
	}

	private boolean validateRelationships(String playerId, String coachId, String courseId, String academyId) {
		return validateUserId(playerId) && validateUserId(coachId) && validateCourse(courseId)
				&& validateAcademy(academyId);
	}

	public boolean validateUserId(String id) {
		return id == null || userProfileRepository.existsById(id);
	}

	public boolean validateMandatoryUserId(String id) {
		return userProfileRepository.existsById(id);
	}

	public boolean validateCourse(String courseId) {
		return courseId == null || courseRepository.existsById(courseId);
	}

	public boolean validateAcademy(String academyId) {
		return academyId == null || academyRepository.existsById(academyId);
	}

	public boolean validateAnalysisComments(List<VideoAnalysisDto> comments) {
		if (comments == null) {
			return true;
		}

		return comments.stream()
				.allMatch(comment -> isValidLength(comment.getComment(), 1, 500) && (comment.getAnalysedByUser() == null
						|| userProfileRepository.existsById(comment.getAnalysedByUser())));
	}

	public boolean isValidId(String id) {
		return id != null && !id.isBlank();
	}

	// Helper methods from original implementation
	private static boolean isValidLengthRegex(String value, String regex) {
		return value != null && value.matches(regex);
	}

	private static boolean isValidMandatoryLength(String input, int minLength, int maxLength) {
		return input.length() >= minLength && input.length() <= maxLength;
	}

	private static boolean isValidLength(String input, int minLength, int maxLength) {
		return input == null || input.length() >= minLength && input.length() <= maxLength;
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