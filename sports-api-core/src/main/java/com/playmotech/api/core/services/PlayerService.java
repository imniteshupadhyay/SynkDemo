package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.UserAddEditDto;
import com.playmotech.api.core.response.ServiceResponse;

public interface PlayerService {

	ServiceResponse addPlayer(UserAddEditDto playerAddEditDto, String userId);

	ServiceResponse deletePlayer(String id, String userId);

	ServiceResponse editPlayer(UserAddEditDto playerAddEditDto, String userId);

	ServiceResponse addCoach(UserAddEditDto userDto, String userId);

	ServiceResponse editCoach(UserAddEditDto userProfileDto, String userId);

	ServiceResponse getPlayerById(String identifier, String userId, String academyDomain);

	ServiceResponse getPlayerKpiById(String identifier, String userId, String academyDomain);

	ServiceResponse getCoachKpiById(String identifier, String userId, String academyDomain);

}
