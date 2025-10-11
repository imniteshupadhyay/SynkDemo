package com.playmotech.api.core.services;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface ProgramService {

	ServiceResponse getProgramsByAcademyId(String academyId);

	ServiceResponse getPrograms(GenericFilter filter);

}
