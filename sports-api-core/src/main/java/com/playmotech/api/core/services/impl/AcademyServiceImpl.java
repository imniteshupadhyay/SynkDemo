package com.playmotech.api.core.services.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.helper.UserProfileHelper;
import com.playmotech.api.core.mapper.AcademyMapper;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.AcademyDao;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.EnumUtil.RoleType;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class AcademyServiceImpl implements com.playmotech.api.core.services.AcademyService {

	private final AcademyRepo academyRepository;
	private final UserProfileHelper userProfileHelper;
	private final AcademyDomainUtil academyDomainUtil;

	@Override
	public ServiceResponse getAllAcademies(String domainUrl) {

		try {
			log.info("Fetching all academies...");

//		ServiceResponse profileServiceResponse = userProfileHelper.fetchUserProfileById(userId);
//
//		if (!profileServiceResponse.getHttpStatus().is2xxSuccessful()) {
//			return profileServiceResponse;
//		}

//		UserProfile userProfile = (UserProfile) profileServiceResponse.getBody();

			String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);

			UserProfile currentUser = academyDomainUtil.getCurrentUser();

			String userProfileId = userRole.equals(RoleType.COACH.getRole()) ? currentUser.getId() : null;

			List<Academy> academies = Collections.emptyList();
			if (userRole.equals(RoleType.SUPER_ADMIN.getRole())) {
				academies = academyRepository.findByInactiveFalse();
			} else if (userRole.equals(RoleType.ACADEMY_OWNER.getRole())) {
				academies = academyRepository.findByInactiveFalseAndManagerUserId(currentUser.getId());
			} else {
				academies = academyRepository.findByUserIdAndInactiveFalse(currentUser.getId(), domainUrl);
			}

			if (academies.isEmpty()) {
				log.warn(ApiResponse.ACADEMY_NOT_FOUND.getMessage());
				return ResponseBuilder.success(ApiResponse.ACADEMY_NOT_FOUND, HttpStatus.OK);
			}

			List<AcademyDao> academyDaos = AcademyMapper.mapListToDaoList(academies, userProfileId);

			log.info(ApiResponse.ACADEMY_LIST_FETCHED.getMessage());
			return ResponseBuilder.success(academyDaos, ApiResponse.ACADEMY_LIST_FETCHED, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error fetching academy list", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_ACADEMY);
		}
	}

}
