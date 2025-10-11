package com.playmotech.api.core.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.cache.AcademyConfigCache;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Organisation;
import com.playmotech.api.core.dao_postgres.OrganisationConfig;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.OrgConfigRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.OrgConfigService;
import com.playmotech.api.core.utils.AcademyDomainUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class OrgConfigServiceImpl implements OrgConfigService {

	private final AcademyDomainUtil academyDomainUtil;
	private final OrgConfigRepo configRepo;
	private final AcademyRepo academyRepo;
	private final AcademyConfigCache configCache;

	@Override
	public ServiceResponse setCheckPendingDue(Boolean status) {
		try {
			if (status == null) {
				log.warn("Status is null in setCheckPendingDue request");
				return ResponseBuilder.badRequest("Status cannot be null");
			}

			UserProfile currentUser = academyDomainUtil.getCurrentUser();
			log.info("User {} requested to set checkPendingDue = {}", currentUser.getId(), status);

			// Find academies where current user is a manager
			List<Academy> managerAcademies = academyRepo.findByManagerUserIdAndInactiveFalse(currentUser.getId());
			log.info("Found {} academies managed by user {}", managerAcademies.size(), currentUser.getId());

			if (managerAcademies.isEmpty()) {
				log.warn("User {} is not a owner in any academies", currentUser.getId());
				return ResponseBuilder.badRequest("Not Allowed");
			}

			// Extract orgIds safely (skip academies without org)
			Set<String> orgIds = managerAcademies.stream().filter(ac -> {
				if (ac.getOrg() == null || ac.getOrg().getId() == null) {
					log.warn("Academy {} has no associated organisation, skipping", ac.getId());
					return false;
				}
				return true;
			}).map(ac -> ac.getOrg().getId()).collect(Collectors.toSet());

			if (orgIds.isEmpty()) {
				log.warn("No valid organisation IDs found for user {}", currentUser.getId());
				return ResponseBuilder.badRequest("No valid organisations found");
			}

			log.info("Unique orgIds for update: {}", orgIds);

			// Fetch all existing configs for these orgIds in one go
			List<OrganisationConfig> existingConfigs = configRepo.findByOrgIdInAndKey(orgIds, "checkPendingDue");
			log.info("Loaded {} existing configs for key=checkPendingDue", existingConfigs.size());

			// Map for quick lookup
			Map<String, OrganisationConfig> configMap = existingConfigs.stream()
					.collect(Collectors.toMap(cfg -> cfg.getOrg().getId(), cfg -> cfg));

			List<OrganisationConfig> toSave = new ArrayList<>();

			// Process each orgId
			for (String orgId : orgIds) {
				OrganisationConfig existing = configMap.get(orgId);

				if (existing != null) {
					if (!status.toString().equals(existing.getValue())) {
						log.info("Updating orgId={} from {} to {}", orgId, existing.getValue(), status);
						existing.setValue(status.toString());
						toSave.add(existing);
					} else {
						log.info("Skipping orgId={} (already set to {})", orgId, status);
					}
				} else {
					log.info("Creating new config for orgId={} with value={}", orgId, status);
					OrganisationConfig newConfig = new OrganisationConfig();
					newConfig.setOrg(Organisation.builder().id(orgId).build());
					newConfig.setKey("checkPendingDue");
					newConfig.setValue(status.toString());
					// Will set ID later before saving
					toSave.add(newConfig);
				}
			}

			// Save only changed/new configs
			if (!toSave.isEmpty()) {
				log.info("Saving {} config(s) to database", toSave.size());

				// Get current max ID
				Long maxId = configRepo.findMaxId();
				log.info("Current max ID in organisation_config table: {}", maxId);

				// Set IDs for new configs
				for (OrganisationConfig config : toSave) {
					if (config.getId() == null) {
						maxId++;
						log.info("Setting ID {} for new config (orgId={}, key={})", maxId, config.getOrg().getId(),
								config.getKey());
						config.setId(maxId);
					}
				}

				configRepo.saveAll(toSave);
			} else {
				log.info("No changes detected. Nothing to save.");
			}

			// Invalidating org level cache for checkPendingDue
			log.info("Invalidating org level cache for checkPendingDue");
			toSave.forEach(each -> configCache.invalidateOrgConfig(each.getOrg().getId()));
			log.info("Successfully processed checkPendingDue update for {} org(s)", orgIds.size());
			return ResponseBuilder.success("Configuration updated successfully", HttpStatus.OK);

		} catch (Exception e) {
			log.error("Error updating checkPendingDue configuration", e);
			return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
		}
	}

}
