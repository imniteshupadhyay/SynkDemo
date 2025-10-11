package com.playmotech.api.core.services.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.LeadStatus;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.dao.LeadsDao;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.LeadSource;
import com.playmotech.api.core.dao_postgres.Leads;
import com.playmotech.api.core.dao_postgres.Trial;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AddEditLeadDto;
import com.playmotech.api.core.dto.LeadsDto;
import com.playmotech.api.core.dto.PaginatedResponse;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.TrialDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.mapper.LeadsMapper;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.LeadRepo;
import com.playmotech.api.core.repo.LeadSourceRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.ILeadService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.services.TrialService;
import com.playmotech.api.core.specification.LeadsSpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.ExcelGenerator;
import com.playmotech.api.core.utils.GenericFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService implements ILeadService {

	private final LeadRepo leadsRepo;
	private final UserProfileRepo userProfileRepo;
	private final AcademyRepo academyRepo;
	private final LeadSourceRepo leadSourceRepo;
	private final IUserProfileService iUserProfileService;
	private final TrialService iTrailService;
	private final CoachAcademyMappingRepo coachAcademyMappingRepo;
	private final LeadsMapper leadsMapper;
	private final IPushNotificationService iPushNotificationService;
	private final AcademyDomainUtil academyDomainUtil;

	@Override
	public LeadsDto getLeadById(String id) throws ResourceException {
		Leads lead = leadsRepo.findById(id)
				.orElseThrow(() -> new ResourceException(ErrorCodes.NOT_FOUND, "No record found"));
		return leadsMapper.toDto(lead);
	}

	@Override
	public Response<?> getAllLeads(GenericFilter filter, String domainUrl) {
		List<LeadsDto> leadsDtoList = new ArrayList<>();
		List<Leads> leadsList = new ArrayList<>();
		Page<Leads> pageableContent = null;
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
			UserProfile currentUser = userProfileRepo.findById(currentSessionUser.getUserId()).get();

			String userRole = academyDomainUtil.getCurrentUserRoleName(domainUrl);
			filter.setUserRole(userRole);

			LeadsSpecification leadsSpec = new LeadsSpecification(filter, currentUser, coachAcademyMappingRepo);

			if (!filter.isExport() && filter.isPageable()) {
				PageRequest pageRequest = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
				pageableContent = leadsRepo.findAll(leadsSpec, pageRequest);

				// pageableContent.getContent().stream().forEach(each -> {
				// LeadsDto leadDto = leadsMapper.toDto(each);
				// leadsDtoList.add(leadDto);
				// });
				leadsDtoList = pageableContent.getContent().stream().map(leadsMapper::toDto)
						.collect(Collectors.toList());
			} else {
				leadsList = leadsRepo.findAll(leadsSpec);
				leadsDtoList = leadsList.stream().map(leadsMapper::toDto).toList();
			}

			if (filter.isExport()) {
				return exportLeadToExcel(leadsDtoList);
			}

			return Response.builder().status(HttpStatus.OK.value())
					.body(new PaginatedResponse<>(leadsDtoList,
							pageableContent != null ? pageableContent.getTotalElements() : leadsDtoList.size(),
							pageableContent != null ? pageableContent.getTotalPages() : 0, filter.getCurrentPage()))
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Something unexpected occured");
			return Response.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(null).build();
		}
	}

	public Response<?> exportLeadToExcel(List<LeadsDto> leadsDtoList) {
		if (leadsDtoList.isEmpty()) {
			log.error("leadsDtoList is empty. Not generating excel");
			return Response.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(null)
					.message("No leads found while generating excel").build();
		}
		try {
			List<LeadsDao> leadsDaoForExcel = leadsMapper.mapDtoListToDaoList(leadsDtoList);
			log.info("leads list size for excel", leadsDaoForExcel.size());
			byte[] excelBytes = ExcelGenerator.generateExcel(leadsDaoForExcel, null);
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("data", Base64.getEncoder().encodeToString(excelBytes));
			String excelName = "Leads_List_".concat(LocalDateTime.now().toString()).concat(".xlsx");
			responseMap.put("fileName", excelName);
			responseMap.put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

			return Response.builder().status(HttpStatus.OK.value()).body(responseMap).message("ok").build();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			log.error("Error while generating leads excel", ioe.getMessage());
			return Response.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(null)
					.message("Unable to generate excel").build();
		}
	}

	@Override
	@Transactional
	public LeadsDto addLead(AddEditLeadDto addLeadDto) throws ResourceException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		UserProfileDto userProfileDto = iUserProfileService.getUserProfileById(currentUser.getUserId());
		UserProfile userProfile = userProfileRepo.findById(userProfileDto.getId()).get();

		Leads newLead = new Leads();

		LeadSource leadSource = getLeadSourceOrDefaultSource(addLeadDto.getLeadSourceId());

		BeanUtils.copyProperties(addLeadDto, newLead);

		newLead.setLeadCreatedBy(userProfile);
		newLead.setLeadStatus(LeadStatus.OPEN);
		newLead.setLeadSource(leadSource);

		return leadsMapper.toDto(leadsRepo.save(newLead));
	}

	@Override
	@Transactional
	public LeadsDto updateLead(String id, AddEditLeadDto editLeadDto) throws ResourceException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		UserProfileDto userProfileDto = iUserProfileService.getUserProfileById(currentUser.getUserId());

		Leads existingLead = leadsRepo.findById(id)
				.orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Lead not found"));

		if (!existingLead.getLeadStatus().equals(LeadStatus.OPEN)) {
			throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Assigned lead cannot be edited");
		}

		LeadSource existingLeadSource = leadSourceRepo.findById(editLeadDto.getLeadSourceId()).get();

		existingLead.setAddressLine1(editLeadDto.getAddressLine1());
		existingLead.setAddressLine2(editLeadDto.getAddressLine2());
		existingLead.setAgeCategory(editLeadDto.getAgeCategory());
		existingLead.setEmailId(editLeadDto.getEmailId());
		existingLead.setGender(editLeadDto.getGender());
		existingLead.setName(editLeadDto.getName());
		existingLead.setPhoneNumber(editLeadDto.getPhoneNumber());
		existingLead.setSports(editLeadDto.getSports());
		existingLead.setLeadSourceReason(editLeadDto.getLeadSourceReason());
		existingLead.setDob(editLeadDto.getDob());

		if (existingLeadSource.getId().equals(editLeadDto.getLeadSourceId())) {
			existingLead.setLeadSource(existingLeadSource);
		} else {
			existingLead.setLeadSource(getLeadSourceOrDefaultSource(editLeadDto.getLeadSourceId()));
		}

		return leadsMapper.toDto(leadsRepo.save(existingLead));
	}

	@Override
	@Transactional
	public void inactivateLead(String id) throws ResourceException {
		Optional<Leads> leadsOptional = leadsRepo.findById(id);

		if (leadsOptional.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Lead not found");
		}

		leadsOptional.get().setInactive(true);
		leadsRepo.save(leadsOptional.get());
	}

	@Override
	@Transactional
	public void assignLeads(String coachId, String academyId, List<String> leads, boolean reassign)
			throws ResourceException {
		try {
			if (leads.isEmpty()) {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Please select at least one lead");
			}

			UserProfile assignedCoach = userProfileRepo.findById(coachId).orElseThrow(
					() -> new ResourceException(ErrorCodes.COACH_USERID_DOESNT_EXIST, "Coach does not exists"));
			Academy assignedAcademy = academyRepo.findById(academyId)
					.orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found"));

			// List<Leads> leadsList = leadsRepo.findAllById(leads);
			List<Leads> leadsList = leadsRepo.findByIdInAndInactiveFalse(leads);

			leadsList.stream().forEach(each -> {
				each.setAssignedCoach(assignedCoach);
				each.setAssignedAcademy(assignedAcademy);
				each.setLeadStatus(LeadStatus.ASSIGNED);
			});
			List<Leads> assignedLeads = leadsRepo.saveAll(leadsList);

			if (reassign) {
				updateTrialsMappedToLeads(coachId, academyId, assignedLeads);
			} else {
				transferLeadsToTrials(coachId, academyId, assignedLeads);
			}

			try {
				String fcmToken = assignedCoach.getAndroidFcmPushToken();

				// Use TransactionSynchronizationManager to execute after successful commit
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						try {
							sendNotificationToCoach(coachId, assignedLeads, fcmToken);
						} catch (Exception e) {
							log.error("Error sending notification after transaction commit: {}", e.getMessage(), e);
							// Notification failure doesn't affect transaction
						}
					}
				});
				sendNotificationToCoach(coachId, assignedLeads, assignedCoach.getAndroidFcmPushToken());
			} catch (Exception e) {
				log.error("Error setting up notification after transaction: {}" + e.getMessage(), e);
			}
		} catch (ResourceException e) {
			log.error("Error while assigning leads: {}", e);
		}
	}

	/**
	 * Method to convert leads to trials
	 *
	 * @param coachId
	 * @param academyId
	 * @param assignedLeads
	 */
	private void transferLeadsToTrials(String coachId, String academyId, List<Leads> assignedLeads)
			throws ResourceException {
		for (Leads each : assignedLeads) {
			TrialDto newTrial = new TrialDto();

			newTrial.setLeadId(each.getId());
			newTrial.setName(each.getName());

			String addressLine = Stream.of(each.getAddressLine1(), each.getAddressLine2()).filter(StringUtils::hasText)
					.collect(Collectors.joining(", "));

			newTrial.setAddress(addressLine.isEmpty() ? null : addressLine);
			newTrial.setTrailDate(null);
			newTrial.setTrailTime(null);
			newTrial.setGender(each.getGender().name());
			newTrial.setSports(each.getSports().name());
			newTrial.setEmail(each.getEmailId());
			newTrial.setPhone(each.getPhoneNumber());
			newTrial.setCoachId(coachId);
			newTrial.setAcademyId(academyId);
			newTrial.setDob(each.getDob());

			ServiceResponse trialResponse = iTrailService.createTrial(newTrial);
			if (!trialResponse.getHttpStatus().is2xxSuccessful()) {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Invalid data provided while assigning.");
			}
		}
	}

	private void updateTrialsMappedToLeads(String coachId, String academyId, List<Leads> assignedLeads) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			UserProfileDto userProfileDto = iUserProfileService.getUserProfileById(currentUser.getUserId());
			UserProfile userProfile = userProfileRepo.findById(userProfileDto.getId()).get();
			log.info("Updating trials for {} reassigned leads", assignedLeads.size());

			// Get all leads id
			List<String> leadIds = assignedLeads.stream().map(Leads::getId).toList();

			// Find existing trials associated with these leads
			List<Trial> existingTrials = iTrailService.findTrialsByLeadIds(leadIds);

			if (existingTrials.isEmpty()) {
				log.warn("No existing trials found for leads being reassigned.");
				return;
			}

			// Get coach and academy entities
			UserProfile coach = userProfileRepo.findById(coachId).orElseThrow(
					() -> new ResourceException(ErrorCodes.COACH_USERID_DOESNT_EXIST, "Coach does not exist"));

			Academy academy = academyRepo.findById(academyId)
					.orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Academy not found"));

			// Update each trial with new coach and academy
			existingTrials.forEach(trial -> {
				trial.setCoach(coach);
				trial.setAcademy(academy);
				trial.setUpdatedOn(LocalDateTime.now());
				trial.setUpdatedBy(userProfile);
			});

			iTrailService.saveAllTrials(existingTrials);

			log.info("Successfully updated {} trials with new coach & academy", existingTrials.size());

		} catch (Exception e) {
			log.error("Error updating trials mapped to leads: {}", e.getMessage(), e);
		}

	}

	/**
	 * Method to send push notification to coach after leads is transferred to
	 * trials
	 *
	 * @param coachId
	 * @param assignedLeads
	 * @param fcmToken
	 */
	private void sendNotificationToCoach(String coachId, List<Leads> assignedLeads, String fcmToken) {
		if (fcmToken != null && !fcmToken.isEmpty() && !assignedLeads.isEmpty()) {
			try {
				String trialGrammar = assignedLeads.size() > 1 ? " new trials." : " new trail.";
				iPushNotificationService.sendMessageToPushToken(fcmToken, NotificationType.LIVE_NOTIFICATION,
						"New Trials Assigned", "You have been assigned " + assignedLeads.size() + trialGrammar,
						"TRIALS_LEADS", CtaType.SCREEN, null);
			} catch (Exception e) {
				log.error("Failed to send push notification to coach: {}", e);
			}
		} else {
			log.warn("No FCM token found for coach: {}", coachId);
		}
	}

	/**
	 * Returns a lead source or a default source - Others
	 */
	private LeadSource getLeadSourceOrDefaultSource(String leadSourceId) {
		return leadSourceRepo.findById(leadSourceId).orElseGet(() -> leadSourceRepo.findByNameLike("Others").get(0));
	}
}
