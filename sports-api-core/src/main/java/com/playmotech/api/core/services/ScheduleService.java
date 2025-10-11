package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.DaywiseActivityDto;
import com.playmotech.api.core.dto.DaywiseActivityMappingDto;
import com.playmotech.api.core.dto.NewScheduleDto;
import com.playmotech.api.core.dto.ScheduleFileUploadRequest;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface ScheduleService {

	ServiceResponse getScheduleList(GenericFilter filter, String domainUrl);

	ServiceResponse getSchedule(Long id);

	ServiceResponse addSchedule(NewScheduleDto scheduleDto, String academyDomain);

	ServiceResponse updateSchedule(NewScheduleDto scheduleDto, String academyDomain);

	ServiceResponse deleteSchedule(Long id);

	ServiceResponse deleteDaywiseActivity(Long id);

	ServiceResponse updateDaywiseActivity(DaywiseActivityDto activityDto, String academyDomain);

	ServiceResponse addDaywiseActivity(Long scheduleId, DaywiseActivityDto activityDto, String academyDomain);

	ServiceResponse addDaywiseActivityMapping(Long daywiseActivityId, DaywiseActivityMappingDto mappingDto,
			String academyDomain);

	ServiceResponse updateDaywiseActivityMapping(DaywiseActivityMappingDto mappingDto, String academyDomain);

	ServiceResponse deleteDaywiseActivityMapping(Long id);

	ServiceResponse uploadScheduleFile(ScheduleFileUploadRequest request,
			String academyDomain) throws ResourceException;

}
