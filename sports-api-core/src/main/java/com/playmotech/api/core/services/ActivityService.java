package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.ActivityDto;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface ActivityService {

	/**
	 * Creates a new activity
	 *
	 * @param activityDto   The activity data
	 * @param academyDomain
	 * @return Service response
	 */
	ServiceResponse createActivity(ActivityDto activityDto, String academyDomain);

	/**
	 * Updates an existing activity
	 *
	 * @param activityDto The updated activity data
	 * @return Service response
	 */
	ServiceResponse updateActivity(ActivityDto activityDto, String academyDomain);

	/**
	 * Deletes an activity by its ID
	 *
	 * @param id The activity ID
	 * @return Service response
	 */
	ServiceResponse deleteActivity(Long id);

	/**
	 * Gets all activities based on provided filters
	 *
	 * @param filter    The filter criteria
	 * @param domainUrl The academy domain URL
	 * @return Service response containing list of activities
	 */
	ServiceResponse getAllActivities(GenericFilter filter, String domainUrl);

	/**
	 * Gets an activity by its ID
	 *
	 * @param id The activity ID
	 * @return Service response containing the activity
	 */
	ServiceResponse getActivityById(Long id);
}
