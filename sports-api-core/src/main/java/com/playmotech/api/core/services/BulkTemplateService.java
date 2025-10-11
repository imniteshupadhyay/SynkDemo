package com.playmotech.api.core.services;

import com.playmotech.api.core.response.ServiceResponse;

public interface BulkTemplateService {

	ServiceResponse downloadCoachTemplate(String path, String academyId, boolean edit);

	ServiceResponse downloadPlayerTemplate(String path, String academyId, boolean edit);

	ServiceResponse downloadProgramTemplate(String academyId, String path);

	ServiceResponse downloadProgramPlayerTemplate(String userId, String academyId, String path);

	ServiceResponse downloadProgramCoachTemplate(String userId, String academyId, String path);

	ServiceResponse downloadProgramEnrollmentTemplate(String academyId, String path);

}
