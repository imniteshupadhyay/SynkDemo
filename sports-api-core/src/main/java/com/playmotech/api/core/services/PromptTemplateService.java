package com.playmotech.api.core.services;

import java.util.Map;

import com.playmotech.api.core.dao_postgres.VideoAnalytics.SportsType;
import com.playmotech.api.core.response.ServiceResponse;

public interface PromptTemplateService {

	/**
	 * Get all active prompt templates
	 */
	ServiceResponse getAllActivePromptTemplates();

	/**
	 * Get active prompt template for a specific sports type
	 */
	ServiceResponse getActivePromptTemplate(SportsType sportsType);

	/**
	 * Get all templates (including inactive) for a specific sports type
	 */
	ServiceResponse getTemplatesBySportsType(SportsType sportsType);

	/**
	 * Get template by ID
	 */
	ServiceResponse getTemplateById(Long id);

	/**
	 * Create new prompt template
	 */
	ServiceResponse createPromptTemplate(SportsType sportsType, String template, String description, String createdBy);

	/**
	 * Update existing prompt template
	 */
	ServiceResponse updatePromptTemplate(Long id, String template, String description, String updatedBy);

	/**
	 * Activate prompt template
	 */
	ServiceResponse activatePromptTemplate(Long id, String updatedBy);

	/**
	 * Deactivate prompt template
	 */
	ServiceResponse deactivatePromptTemplate(Long id, String updatedBy);

	/**
	 * Delete prompt template
	 */
	ServiceResponse deletePromptTemplate(Long id);

	/**
	 * Validate template against parameters
	 */
	ServiceResponse validateTemplate(String template, Map<String, Object> params);
}
