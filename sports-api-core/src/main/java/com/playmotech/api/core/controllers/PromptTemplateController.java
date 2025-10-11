package com.playmotech.api.core.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dao_postgres.VideoAnalytics.SportsType;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.PromptTemplateService;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("prompt-templates")
public class PromptTemplateController {

	private final PromptTemplateService promptTemplateService;

	/**
	 * Get all active prompt templates
	 */
	@GetMapping
	public ResponseEntity<ServiceResponse> getAllActiveTemplates() {
		ServiceResponse response = promptTemplateService.getAllActivePromptTemplates();
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Get active template for specific sports type
	 */
	@GetMapping("/sports")
	public ResponseEntity<ServiceResponse> getTemplateForSportsType(@RequestParam SportsType sportsType) {
		ServiceResponse response = promptTemplateService.getActivePromptTemplate(sportsType);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Get all templates for specific sports type (including inactive)
	 */
	@GetMapping("/sports/all")
	public ResponseEntity<ServiceResponse> getAllTemplatesForSportsType(@RequestParam SportsType sportsType) {
		ServiceResponse response = promptTemplateService.getTemplatesBySportsType(sportsType);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Get template by ID
	 */
	@GetMapping("/by-id")
	public ResponseEntity<ServiceResponse> getTemplateById(@RequestParam Long id) {
		ServiceResponse response = promptTemplateService.getTemplateById(id);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Create new prompt template
	 */
	@PostMapping
	public ResponseEntity<ServiceResponse> createTemplate(@Valid @RequestBody CreatePromptTemplateRequest request) {
		ServiceResponse response = promptTemplateService.createPromptTemplate(request.getSportsType(),
				request.getTemplate(), request.getDescription(), request.getCreatedBy());
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Update existing prompt template
	 */
	@PutMapping
	public ResponseEntity<ServiceResponse> updateTemplate(@Valid @RequestBody UpdatePromptTemplateRequest request) {
		ServiceResponse response = promptTemplateService.updatePromptTemplate(request.getId(), request.getTemplate(),
				request.getDescription(), request.getUpdatedBy());
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Activate prompt template
	 */
	@PutMapping("/activate")
	public ResponseEntity<ServiceResponse> activateTemplate(@RequestParam Long id, @RequestParam String updatedBy) {
		ServiceResponse response = promptTemplateService.activatePromptTemplate(id, updatedBy);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Deactivate prompt template
	 */
	@PutMapping("/deactivate")
	public ResponseEntity<ServiceResponse> deactivateTemplate(@RequestParam Long id, @RequestParam String updatedBy) {
		ServiceResponse response = promptTemplateService.deactivatePromptTemplate(id, updatedBy);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Delete prompt template
	 */
	@DeleteMapping
	public ResponseEntity<ServiceResponse> deleteTemplate(@RequestParam Long id) {
		ServiceResponse response = promptTemplateService.deletePromptTemplate(id);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	/**
	 * Validate template with parameters
	 */
	@PostMapping("/validate")
	public ResponseEntity<ServiceResponse> validateTemplate(@RequestBody ValidateTemplateRequest request) {
		ServiceResponse response = promptTemplateService.validateTemplate(request.getTemplate(), request.getParams());
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	// Request/Response DTOs
	@Data
	public static class CreatePromptTemplateRequest {
		private SportsType sportsType;
		private String template;
		private String description;
		private String createdBy;
	}

	@Data
	public static class UpdatePromptTemplateRequest {
		private Long id;
		private String template;
		private String description;
		private String updatedBy;
	}

	@Data
	public static class ValidateTemplateRequest {
		private String template;
		private Map<String, Object> params;
	}

	@Data
	public static class ValidationResult {
		private boolean valid;
		private String message;

		public ValidationResult(boolean valid, String message) {
			this.valid = valid;
			this.message = message;
		}
	}
}