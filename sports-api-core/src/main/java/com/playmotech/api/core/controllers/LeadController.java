package com.playmotech.api.core.controllers;

import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dao_postgres.LeadSource;
import com.playmotech.api.core.dao_postgres.Leads;
import com.playmotech.api.core.dto.AddEditLeadDto;
import com.playmotech.api.core.dto.LeadsDto;
import com.playmotech.api.core.dto.PaginatedResponse;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.ILeadService;
import com.playmotech.api.core.services.ILeadSourcesService;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.utils.TimeStampParse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("leads")
public class LeadController {
	private final ILeadService leadService;
	private final ILeadSourcesService leadSourcesService;

	@GetMapping("{id}")
	public ResponseEntity<Response<LeadsDto>> getLeadById(@PathVariable String id) {
		try {
			LeadsDto lead = leadService.getLeadById(id);

			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<LeadsDto>builder().status(HttpStatus.OK.value()).message("ok").body(lead).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<LeadsDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping
	public ResponseEntity<?> getAllList(HttpServletRequest request,
			@RequestParam(defaultValue = "id", required = false) String orderBy,
			@RequestParam(defaultValue = "", required = false) String search,
			@RequestParam(defaultValue = "", required = false) String startDate,
			@RequestParam(defaultValue = "", required = false) String endDate,
			@RequestParam(defaultValue = "", required = false) String academyId,
			@RequestParam(defaultValue = "", required = false) String sport,
			@RequestParam(defaultValue = "", required = false) String name,
			@RequestParam(defaultValue = "", required = false) String phone,
			@RequestParam(defaultValue = "", required = false) String email,
			@RequestParam(defaultValue = "1", required = false) Short currentPage,
			@RequestParam(defaultValue = "10", required = false) Short pageSize,
			@RequestParam(defaultValue = "", required = false) List<String> status,
			@RequestParam(defaultValue = "", required = false) List<String> sports,
			@RequestParam(defaultValue = "", required = false) List<String> academyIds,
			@RequestParam(defaultValue = "", required = false) List<String> ageCategories,
			@RequestParam(defaultValue = "", required = false) List<String> sources,
			@RequestParam(defaultValue = "true", required = false) boolean active,
			@RequestParam(defaultValue = "true", required = false) boolean ascending,
			@RequestParam(defaultValue = "false", required = false) boolean pageable,
			@RequestParam(defaultValue = "false", required = false) boolean export) {

		Timestamp startDateLocal = null;
		Timestamp endDateLocal = null;

		if ((startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
			try {
				startDateLocal = TimeStampParse.parseTimestamp(startDate);
				endDateLocal = TimeStampParse.parseTimestamp(endDate);

				if (startDateLocal == null || endDateLocal == null) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(Response.<PaginatedResponse<LeadsDto>>builder().status(HttpStatus.BAD_REQUEST.value())
									.message("Invalid listing filters").build());
				}
			} catch (DateTimeParseException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Response.<PaginatedResponse<LeadsDto>>builder().status(HttpStatus.BAD_REQUEST.value())
								.message("Invalid listing filters").build());
			}
		}

		Leads leadsFilter = Leads.builder().leadSourceIds(sources).status(status).build();

		GenericFilter filter = GenericFilter.builder().orderBy(orderBy).isPageable(pageable).currentPage(currentPage)
				.pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy).academyIds(academyIds)
				.startDate(startDateLocal).endDate(endDateLocal).sports(sports).ageCategory(ageCategories)
				.notDeleted(active).export(export).leads(leadsFilter).build();

		String domainUrl = request.getHeader("origin");

		Response<?> paginatedLeads = leadService.getAllLeads(filter, domainUrl);

		return ResponseEntity.status(paginatedLeads.getStatus())
				.body(Response.builder().status(paginatedLeads.getStatus()).body(paginatedLeads.getBody())
						.message(paginatedLeads.getMessage()).build());
	}

	@PostMapping
	public ResponseEntity<Response<LeadsDto>> addLead(@RequestBody AddEditLeadDto addLeadDto) {
		try {
			LeadsDto savedLead = leadService.addLead(addLeadDto);
			return ResponseEntity.status(HttpStatus.CREATED).body(Response.<LeadsDto>builder()
					.status(HttpStatus.CREATED.value()).message("success").body(savedLead).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<LeadsDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PutMapping("{id}")
	public ResponseEntity<Response<LeadsDto>> editLead(@PathVariable String id,
			@RequestBody AddEditLeadDto editLeadDto) {
		try {
			LeadsDto updatedLead = leadService.updateLead(id, editLeadDto);

			return ResponseEntity.status(HttpStatus.OK).body(Response.<LeadsDto>builder().status(HttpStatus.OK.value())
					.message("success").body(updatedLead).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<LeadsDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping("{coachId}/{academyId}")
	public ResponseEntity<Response<String>> assignLead(@PathVariable String coachId, @PathVariable String academyId,
			@RequestBody LeadsId leadsId, @RequestParam(defaultValue = "false") boolean reassign) {
		log.info("coachId: {}", coachId);
		log.info("academyId: {}", academyId);
		log.info("reassign: {}", reassign);
		leadsId.leads.forEach(log::info);

		try {
			leadService.assignLeads(coachId, academyId, leadsId.leads, reassign);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<String>builder().status(HttpStatus.OK.value()).body("ok").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<String>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@DeleteMapping("{id}")
	public ResponseEntity<Response<String>> deleteLead(@PathVariable String id) {
		try {
			leadService.inactivateLead(id);

			return ResponseEntity.status(HttpStatus.ACCEPTED)
					.body(Response.<String>builder().status(HttpStatus.ACCEPTED.value()).message("ok").build());
		} catch (ResourceException e) {
			log.error("Failed to inactivate lead", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<String>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping("sources")
	public ResponseEntity<Response<List<LeadSource>>> getAllLeadSource() {
		List<LeadSource> leadSources = leadSourcesService.getAllSources();
		return ResponseEntity.status(HttpStatus.OK)
				.body(Response.<List<LeadSource>>builder().status(HttpStatus.OK.value())
						.message(leadSources.isEmpty() ? "No record found" : "Record fetched successfuly")
						.body(leadSources).build());
	}

	public record LeadsId(List<String> leads) {
	}
}
