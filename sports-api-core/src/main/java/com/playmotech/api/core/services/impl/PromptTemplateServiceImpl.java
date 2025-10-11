package com.playmotech.api.core.services.impl;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao_postgres.PromptTemplate;
import com.playmotech.api.core.dao_postgres.VideoAnalytics.SportsType;
import com.playmotech.api.core.repo.PromptTemplateRepository;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.PromptTemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements PromptTemplateService {

	private final PromptTemplateRepository promptTemplateRepository;

	@Override
	public ServiceResponse getAllActivePromptTemplates() {
		try {
			List<PromptTemplate> templates = promptTemplateRepository.findByDeletedFalse();

			if (templates == null || templates.isEmpty()) {
				return ResponseBuilder.error("No active prompt templates found", ErrorCodes.NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			return ResponseBuilder.success(templates, "Retrieved active prompt templates", HttpStatus.OK);
		} catch (Exception e) {
			log.error("Failed to retrieve active prompt templates", e);
			return ResponseBuilder.error("Failed to retrieve prompt templates: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse getActivePromptTemplate(SportsType sportsType) {
		try {
			if (sportsType == null) {
				return ResponseBuilder.error("Sports type is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			PromptTemplate template = promptTemplateRepository.findBySportsTypeAndDeletedFalse(sportsType).orElseThrow(
					() -> new NoSuchElementException("No active prompt template found for sports type: " + sportsType));

			return ResponseBuilder.success(template, "Retrieved active prompt template", HttpStatus.OK);

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to get active prompt template for sportsType: {}", sportsType, e);
			return ResponseBuilder.error("Failed to retrieve prompt template: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse getTemplatesBySportsType(SportsType sportsType) {
		try {
			if (sportsType == null) {
				return ResponseBuilder.error("Sports type is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}
			List<PromptTemplate> templates = promptTemplateRepository.findBySportsType(sportsType);

			if (templates == null || templates.isEmpty()) {
				return ResponseBuilder.error("No prompt templates found", ErrorCodes.NOT_FOUND, HttpStatus.NOT_FOUND);
			}

			return ResponseBuilder.success(templates,
					String.format("Retrieved %d prompt templates for sports type: %s", templates.size(), sportsType),
					HttpStatus.OK);
		} catch (Exception e) {
			log.error("Failed to get prompt templates for sportsType: {}", sportsType, e);
			return ResponseBuilder.error("Failed to retrieve prompt templates: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse getTemplateById(Long id) {
		try {
			if (id == null) {
				return ResponseBuilder.error("Template ID is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			PromptTemplate template = promptTemplateRepository.findById(id)
					.orElseThrow(() -> new NoSuchElementException("Prompt template not found with ID: " + id));

			return ResponseBuilder.success(template, "Retrieved prompt template", HttpStatus.OK);

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to get prompt template with ID: {}", id, e);
			return ResponseBuilder.error("Failed to retrieve prompt template: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse createPromptTemplate(SportsType sportsType, String template, String description,
			String createdBy) {
		try {
			if (sportsType == null || template == null || template.trim().isEmpty()) {
				return ResponseBuilder.error("Sports type and template content are required",
						ErrorCodes.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
			}

			PromptTemplate entity = new PromptTemplate();
			entity.setSportsType(sportsType);
			entity.setTemplate(template);
			entity.setDescription(description);
			entity.setVersion(1);
			entity.setCreatedBy(createdBy);
			entity.setDeleted(false);

			PromptTemplate saved = promptTemplateRepository.save(entity);
			log.info("Created prompt template with ID: {} for sportsType: {}", saved.getId(), sportsType);

			return ResponseBuilder.success(saved, "Prompt template created successfully", HttpStatus.CREATED);

		} catch (Exception e) {
			log.error("Failed to create prompt template", e);
			return ResponseBuilder.error("Failed to create prompt template: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse updatePromptTemplate(Long id, String template, String description, String updatedBy) {
		try {
			PromptTemplate existing = promptTemplateRepository.findById(id)
					.orElseThrow(() -> new NoSuchElementException("Prompt template not found with ID: " + id));

			existing.setTemplate(template);
			existing.setDescription(description);
			existing.setUpdatedBy(updatedBy);
			if (existing.getVersion() == null) {
				existing.setVersion(1);
			} else {
				existing.setVersion(existing.getVersion() + 1);
			}

			PromptTemplate saved = promptTemplateRepository.save(existing);
			log.info("Updated prompt template with ID: {}", id);

			return ResponseBuilder.success(saved, "Prompt template updated successfully", HttpStatus.OK);

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to update prompt template with ID: {}", id, e);
			return ResponseBuilder.error("Failed to update prompt template: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse activatePromptTemplate(Long id, String updatedBy) {
		return toggleTemplateActiveStatus(id, updatedBy, true);
	}

	@Override
	public ServiceResponse deactivatePromptTemplate(Long id, String updatedBy) {
		return toggleTemplateActiveStatus(id, updatedBy, false);
	}

	private ServiceResponse toggleTemplateActiveStatus(Long id, String updatedBy, boolean deleted) {
		try {
			PromptTemplate existing = promptTemplateRepository.findById(id)
					.orElseThrow(() -> new NoSuchElementException("Prompt template not found with ID: " + id));

			existing.setDeleted(deleted);
			existing.setUpdatedBy(updatedBy);
			promptTemplateRepository.save(existing);

			String action = deleted ? "deactivated" : "activated";
			log.info("Prompt template with ID: {} {}", id, action);
			return ResponseBuilder.success(null, "Prompt template " + action + " successfully", HttpStatus.OK);

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to toggle active status for prompt template ID: {}", id, e);
			return ResponseBuilder.error("Failed to update active status: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse deletePromptTemplate(Long id) {
		try {
			PromptTemplate existing = promptTemplateRepository.findById(id)
					.orElseThrow(() -> new NoSuchElementException("Prompt template not found with ID: " + id));

			promptTemplateRepository.delete(existing);
			log.info("Deleted prompt template with ID: {}", id);

			return ResponseBuilder.success(null, "Prompt template deleted successfully", HttpStatus.OK);

		} catch (NoSuchElementException e) {
			return ResponseBuilder.error(e.getMessage(), ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Failed to delete prompt template with ID: {}", id, e);
			return ResponseBuilder.error("Failed to delete prompt template: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse validateTemplate(String template, Map<String, Object> params) {
		try {
			if (template == null || template.trim().isEmpty()) {
				return ResponseBuilder.error("Template content is required", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			boolean isValid = params != null && !template.matches("\\{\\{[^}]+}}");
			if (!isValid) {
				return ResponseBuilder.error("Template contains unresolved placeholders",
						ErrorCodes.RESOURCE_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
			}

			return ResponseBuilder.success(true, "Template is valid", HttpStatus.OK);

		} catch (Exception e) {
			log.error("Failed to validate prompt template", e);
			return ResponseBuilder.error("Failed to validate template: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
