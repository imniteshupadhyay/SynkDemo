package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;

/**
 * Created By: deep.patel
 **/
public interface BulkUserService {

	ServiceResponse create(UserProfileDto dto) throws ResourceException;

	ServiceResponse update(UserProfileDto dto) throws ResourceException;

}
