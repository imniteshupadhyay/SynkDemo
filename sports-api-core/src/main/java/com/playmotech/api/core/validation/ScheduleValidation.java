package com.playmotech.api.core.validation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.playmotech.api.core.constants.ScheduleType;
import com.playmotech.api.core.dto.DaywiseActivityDto;
import com.playmotech.api.core.dto.DaywiseActivityMappingDto;
import com.playmotech.api.core.dto.NewScheduleDto;
import com.playmotech.api.core.repo.ActivityRepository;
import com.playmotech.api.core.repo.UserProfileRepo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleValidation {

    private final UserProfileRepo userProfileRepository;
    private final ActivityRepository activityRepository;

    private static final String NAME_REGEX = "^[a-zA-Z0-9 _\\-()]{3,100}$";

    public boolean validate(NewScheduleDto scheduleDto) {
        return isNotEmpty(scheduleDto.getName(), scheduleDto.getDayJson(), scheduleDto.getStartDate(), scheduleDto.getEndDate())
                && validateName(scheduleDto.getName())
                && validateDateRange(scheduleDto.getStartDate(), scheduleDto.getEndDate())
                && validateTimeRange(scheduleDto.getStartTime(), scheduleDto.getEndTime())
                && validateDayJson(scheduleDto.getDayJson(), scheduleDto.getType())
                && validateDaywiseActivities(scheduleDto.getDaywiseActivities(), 
                                             scheduleDto.getStartDate(), 
                                             scheduleDto.getEndDate(),
                                             scheduleDto.getStartTime(),
                                             scheduleDto.getEndTime(),
                                             scheduleDto.getDayJson(),
                                             scheduleDto.getType());
    }

    private boolean validateName(String name) {
        return isValidLengthRegex(name, NAME_REGEX);
    }

    private boolean validateDateRange(LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }

    private boolean validateTimeRange(LocalTime startTime, LocalTime endTime) {
        return startTime == null || endTime == null || !startTime.isAfter(endTime);
    }

    public boolean validateUserId(String id) {
        return id == null || userProfileRepository.existsById(id);
    }

    public boolean validateActivity(Long activityId) {
        return activityId == null || activityRepository.existsById(activityId);
    }
    
    /**
     * Validates a list of activity IDs
     * @param activityIds List of activity IDs to validate
     * @return true if all IDs are valid, false otherwise
     */
    public boolean validateActivities(List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return false;
        }
        
        return activityIds.stream().allMatch(this::validateActivity);
    }

    private boolean validateDaywiseActivities(List<DaywiseActivityDto> activities, 
                                             LocalDate scheduleStartDate, 
                                             LocalDate scheduleEndDate,
                                             LocalTime scheduleStartTime,
                                             LocalTime scheduleEndTime,
                                             String dayJson,
                                             ScheduleType scheduleType) {
        if (activities == null || activities.isEmpty()) {
            return false;
        }

        List<String> allowedDays = parseDayJson(dayJson);
        
        return activities.stream().allMatch(activity -> 
            validateActivityDate(activity.getActivityDate(), scheduleStartDate, scheduleEndDate) &&
            validateActivityDay(activity.getActivityDate(), allowedDays) &&
            validateActivityTime(activity.getStartTime(), activity.getEndTime(), scheduleStartTime, scheduleEndTime) &&
            validateActivityMappings(activity.getActivityMappings(), activity.getStartTime(), activity.getEndTime())
        );
    }

    public boolean validateActivityDate(LocalDate activityDate, LocalDate scheduleStartDate, LocalDate scheduleEndDate) {
        return activityDate != null && 
               !activityDate.isBefore(scheduleStartDate) && 
               !activityDate.isAfter(scheduleEndDate);
    }

    private boolean validateActivityDay(LocalDate activityDate, List<String> allowedDays) {
        if (activityDate == null || allowedDays == null || allowedDays.isEmpty()) {
            return false;
        }
        
        String dayOfWeek = activityDate.getDayOfWeek().name();
        return allowedDays.contains(dayOfWeek);
    }

    private boolean validateActivityTime(LocalTime activityStartTime, LocalTime activityEndTime,
                                        LocalTime scheduleStartTime, LocalTime scheduleEndTime) {
        // First validate that activity times form a valid range
        if (!validateTimeRange(activityStartTime, activityEndTime)) {
            return false;
        }
        
        // If schedule times are null, no additional validation needed
        if (scheduleStartTime == null || scheduleEndTime == null) {
            return true;
        }
        
        // If activity times are null, no additional validation needed
        if (activityStartTime == null || activityEndTime == null) {
            return true;
        }
        
        // Validate that activity time range is within schedule time range
        return !activityStartTime.isBefore(scheduleStartTime) && 
               !activityEndTime.isAfter(scheduleEndTime);
    }

    private boolean validateActivityMappings(List<DaywiseActivityMappingDto> mappings, 
                                            LocalTime activityStartTime, 
                                            LocalTime activityEndTime) {
        if (mappings == null || mappings.isEmpty()) {
            return true;
        }

        return mappings.stream().allMatch(mapping -> 
            validateActivities(mapping.getActivityIds()) &&
            validateMappingTime(mapping.getStartTime(), mapping.getEndTime(), activityStartTime, activityEndTime)
        );
    }

    private boolean validateMappingTime(LocalTime mappingStartTime, LocalTime mappingEndTime,
                                       LocalTime activityStartTime, LocalTime activityEndTime) {
        // First validate that mapping times form a valid range
        if (!validateTimeRange(mappingStartTime, mappingEndTime)) {
            return false;
        }
        
        // If activity times are null, no additional validation needed
        if (activityStartTime == null || activityEndTime == null) {
            return true;
        }
        
        // If mapping times are null, no additional validation needed
        if (mappingStartTime == null || mappingEndTime == null) {
            return true;
        }
        
        // Validate that mapping time range is within activity time range
        return !mappingStartTime.isBefore(activityStartTime) && 
               !mappingEndTime.isAfter(activityEndTime);
    }

    public boolean isValidId(Long id) {
        return id != null;
    }


    // Helper methods
    private static boolean isValidLengthRegex(String value, String regex) {
        return value != null && value.matches(regex);
    }

    private boolean validateDayJson(String dayJson, ScheduleType scheduleType) {
        if (dayJson == null || dayJson.isBlank()) {
            return false;
        }
        
        List<String> days = parseDayJson(dayJson);
        if (days.isEmpty()) {
            return false;
        }
        
        switch (scheduleType) {
            case REGULAR:
                // Any combination of days is valid for REGULAR type
                return days.stream().allMatch(day -> 
                    Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY").contains(day));
            case WEEKENDS:
                // Only SATURDAY and SUNDAY are valid for WEEKEND type
                return days.stream().allMatch(day -> 
                    Arrays.asList("SATURDAY", "SUNDAY").contains(day));
            case WEEKDAYS:
                // Only MONDAY through FRIDAY are valid for WEEKDAYS type
                return days.stream().allMatch(day -> 
                    Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY").contains(day));
            default:
                return false;
        }
    }

    private List<String> parseDayJson(String dayJson) {
        if (dayJson == null || dayJson.isBlank()) {
            return Collections.emptyList();
        }
        
        try {
            // Using Jackson or any JSON library to parse the JSON array
            // This is a simplified example assuming ObjectMapper is available
            // ObjectMapper objectMapper = new ObjectMapper();
            // return objectMapper.readValue(dayJson, new TypeReference<List<String>>(){});
            
            // Simple parsing assuming format is ["DAY1", "DAY2", ...]
            String trimmed = dayJson.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
                return Arrays.stream(trimmed.split(","))
                    .map(day -> day.trim().replace("\"", "").replace("'", ""))
                    .filter(day -> !day.isEmpty())
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static boolean isNotEmpty(Object... fields) {
        return Arrays.stream(fields).allMatch(field -> {
            if (field instanceof String) {
                return !((String) field).isBlank();
            }
            return field != null;
        });
    }
    
    /**
     * Validate a single daywise activity
     * @param activity The daywise activity to validate
     * @param scheduleStartDate The schedule's start date
     * @param scheduleEndDate The schedule's end date
     * @return true if valid, false otherwise
     */
    public boolean validateDaywiseActivity(DaywiseActivityDto activity, LocalDate scheduleStartDate, LocalDate scheduleEndDate) {
        if (activity == null) {
            return false;
        }
        
        // Check required fields
        if (activity.getStartTime() == null || activity.getEndTime() == null || activity.getActivityDate() == null) {
            return false;
        }
        
        // Validate activity date is within schedule date range
        if (!validateActivityDate(activity.getActivityDate(), scheduleStartDate, scheduleEndDate)) {
            return false;
        }
        
        // Validate time range
        if (!validateTimeRange(activity.getStartTime(), activity.getEndTime())) {
            return false;
        }
        
        // Validate mappings if present
        return validateActivityMappings(activity.getActivityMappings(), activity.getStartTime(), activity.getEndTime());
    }

    /**
     * Validate a daywise activity for adding to a schedule
     * @param activity The daywise activity to validate
     * @param scheduleId The schedule ID (for potential lookup if needed)
     * @return true if valid, false otherwise
     */
    public boolean validateAddDaywiseActivity(DaywiseActivityDto activity, Long scheduleId) {
        if (activity == null || !isValidId(scheduleId)) {
            return false;
        }
        
        // Check required fields
        if (activity.getStartTime() == null || 
            activity.getEndTime() == null || 
            activity.getActivityDate() == null) {
            return false;
        }
        
        // Validate time range
        if (!validateTimeRange(activity.getStartTime(), activity.getEndTime())) {
            return false;
        }
        
        // Validate mappings if present
        return validateActivityMappings(activity.getActivityMappings(), activity.getStartTime(), activity.getEndTime());
    }

    /**
     * Validate a daywise activity for update
     * @param activity The daywise activity to validate
     * @return true if valid, false otherwise
     */
    public boolean validateUpdateDaywiseActivity(DaywiseActivityDto activity) {
        if (activity == null || !isValidId(activity.getId())) {
            return false;
        }
        
        // Check required fields
        if (activity.getStartTime() == null || 
            activity.getEndTime() == null || 
            activity.getActivityDate() == null) {
            return false;
        }
        
        // Validate time range
        if (!validateTimeRange(activity.getStartTime(), activity.getEndTime())) {
            return false;
        }
        
        // Validate mappings if present
        return validateActivityMappings(activity.getActivityMappings(), activity.getStartTime(), activity.getEndTime());
    }

    /**
     * Validate activity mappings in detail
     * @param mappings The list of activity mappings to validate
     * @return true if valid, false otherwise
     */
    public boolean validateActivityMappingsDetailed(List<DaywiseActivityMappingDto> mappings) {
        if (mappings == null) {
            return true; // No mappings is valid
        }
        
        for (DaywiseActivityMappingDto mapping : mappings) {
            // Activity IDs are required
            if (mapping.getActivityIds() == null || 
                mapping.getActivityIds().isEmpty() || 
                !validateActivities(mapping.getActivityIds())) {
                return false;
            }
            
            // Start time and end time are required
            if (mapping.getStartTime() == null || mapping.getEndTime() == null) {
                return false;
            }
            
            // Time range must be valid
            if (mapping.getStartTime().isAfter(mapping.getEndTime())) {
                return false;
            }
            
            // For updates, validate ID if present
            if (mapping.getId() != null && mapping.getId() <= 0) {
                return false;
            }
        }
        
        // Check for overlapping time ranges between mappings
        if (!validateNoTimeOverlap(mappings)) {
            return false;
        }
        
        return true;
    }

    /**
     * Validate that no activity mappings have overlapping time ranges
     * @param mappings The list of activity mappings to check
     * @return true if no overlaps, false otherwise
     */
    private boolean validateNoTimeOverlap(List<DaywiseActivityMappingDto> mappings) {
        if (mappings == null || mappings.size() <= 1) {
            return true; // No overlaps possible with 0 or 1 mapping
        }
        
        for (int i = 0; i < mappings.size(); i++) {
            DaywiseActivityMappingDto mapping1 = mappings.get(i);
            
            for (int j = i + 1; j < mappings.size(); j++) {
                DaywiseActivityMappingDto mapping2 = mappings.get(j);
                
                // Check for time overlap
                if (!(mapping1.getEndTime().isBefore(mapping2.getStartTime()) || 
                      mapping1.getStartTime().isAfter(mapping2.getEndTime()))) {
                    return false; // Overlap detected
                }
            }
        }
        
        return true; // No overlaps found
    }

    /**
     * Validate that a daywise activity has time boundaries within a schedule's day
     * @param activityStartTime The activity start time
     * @param activityEndTime The activity end time
     * @param scheduleStartTime The schedule start time (can be null)
     * @param scheduleEndTime The schedule end time (can be null)
     * @return true if valid, false if outside schedule bounds
     */
    public boolean validateActivityTimeWithinSchedule(
            LocalTime activityStartTime, 
            LocalTime activityEndTime,
            LocalTime scheduleStartTime, 
            LocalTime scheduleEndTime) {
        
        // If schedule doesn't define time boundaries, any activity time is valid
        if (scheduleStartTime == null || scheduleEndTime == null) {
            return true;
        }
        
        // Activity must be within schedule time bounds
        return !activityStartTime.isBefore(scheduleStartTime) && 
               !activityEndTime.isAfter(scheduleEndTime);
    }
}