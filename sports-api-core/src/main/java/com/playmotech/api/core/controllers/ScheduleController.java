package com.playmotech.api.core.controllers;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.api.core.constants.ScheduleType;
import com.playmotech.api.core.dto.DaywiseActivityDto;
import com.playmotech.api.core.dto.DaywiseActivityMappingDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.NewScheduleDto;
import com.playmotech.api.core.dto.ScheduleFileUploadRequest;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.ScheduleService;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final AcademyDomainUtil academyDomainUtil;

    @GetMapping("/list")
    public ResponseEntity<ServiceResponse> getScheduleList(
            HttpServletRequest request,
            @RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
            @RequestParam(name = "orderBy", defaultValue = "insertedOn", required = false) String orderBy,
            @RequestParam(name = "academyId", required = false) String academyId,
            @RequestParam(name = "search", defaultValue = "", required = false) String search,
            @RequestParam(name = "type", required = false) ScheduleType type,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
            @RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
            @RequestParam(name = "ascending", defaultValue = "true", required = false) boolean ascending,
            @RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetail currentUser = (UserDetail) authentication.getPrincipal();

            // Validate pagination parameters
            if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
                return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_REQUEST);
            }

            // Validate orderBy parameter
            String[] allowedOrderByValues = { "name", "startDate", "insertedOn" };
            if (!Arrays.asList(allowedOrderByValues).contains(orderBy)) {
                return ResponseBuilder.badRequestEntity(
                        ApiResponse.INVALID_LISTING_FILTERS.getMessage() + Arrays.toString(allowedOrderByValues));
            }

            Timestamp startDateLocal = null;
            Timestamp endDateLocal = null;
            if (startDate != null && endDate != null) {
                try {
                    startDateLocal = parseTimestamp(startDate);
                    endDateLocal = parseTimestamp(endDate);
                    if (startDate != null && endDate != null
                            && startDateLocal.toLocalDateTime().isAfter(endDateLocal.toLocalDateTime())) {
                        return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_LISTING_FILTERS);
                    }
                } catch (DateTimeParseException e) {
                    return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_REQUEST);
                }
            }

            GenericFilter filter = GenericFilter.builder()
                    .userId(currentUser.getUserId())
                    .isPageable(pageable)
                    .currentPage(currentPage)
                    .pageSize(pageSize)
                    .search(search)
                    .ascending(ascending)
                    .orderBy(orderBy)
                    .startDate(startDateLocal)
                    .endDate(endDateLocal)
                    .academyId(academyId)
                    .notDeleted(active)
                    .userId(currentUser.getUserId())
                    .build();

            String academyDomain = "";
            if (StringUtils.hasText(academyId)) {
                academyDomain = academyDomainUtil.getAcademyDomain(academyId);
            } else {
                academyDomain = request.getHeader("origin");
            }
            ServiceResponse response = scheduleService.getScheduleList(filter, academyDomain);
            return new ResponseEntity<>(response, response.getHttpStatus());
        } catch (ResourceException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<ServiceResponse> getSchedule(@RequestParam(name = "id", required = true) Long id) {
        ServiceResponse response = scheduleService.getSchedule(id);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> addSchedule(HttpServletRequest request,
            @RequestParam(name = "academyId", required = false) String academyId,
            @RequestBody NewScheduleDto scheduleDto) {
        try {
            String academyDomain = "";
            if (StringUtils.hasText(academyId)) {
                academyDomain = academyDomainUtil.getAcademyDomain(academyId);
            } else {
                academyDomain = request.getHeader("origin");
            }
            ServiceResponse response = scheduleService.addSchedule(scheduleDto, academyDomain);
            return new ResponseEntity<>(response, response.getHttpStatus());
        } catch (ResourceException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping
    public ResponseEntity<ServiceResponse> updateSchedule(HttpServletRequest request,
            @RequestParam(name = "academyId", required = false) String academyId,
            @RequestBody NewScheduleDto scheduleDto) {
        try {
            String academyDomain = "";
            if (StringUtils.hasText(academyId)) {
                academyDomain = academyDomainUtil.getAcademyDomain(academyId);
            } else {
                academyDomain = request.getHeader("origin");
            }
            ServiceResponse response = scheduleService.updateSchedule(scheduleDto, academyDomain);
            return new ResponseEntity<>(response, response.getHttpStatus());
        } catch (ResourceException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping
    public ResponseEntity<ServiceResponse> deleteSchedule(@RequestParam(name = "id", required = true) Long id) {
        ServiceResponse response = scheduleService.deleteSchedule(id);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    /**
     * Add a new daywise activity to an existing schedule
     */
    @PostMapping("/daywise-activity")
    public ResponseEntity<ServiceResponse> addDaywiseActivity(
            HttpServletRequest request,
            @RequestParam(name = "scheduleId", required = true) Long scheduleId,
            @RequestParam(name = "academyId", required = false) String academyId,
            @RequestBody DaywiseActivityDto daywiseActivityDto) {

        try {
            String academyDomain = "";
            if (StringUtils.hasText(academyId)) {
                academyDomain = academyDomainUtil.getAcademyDomain(academyId);
            } else {
                academyDomain = request.getHeader("origin");
            }

            ServiceResponse response = scheduleService.addDaywiseActivity(scheduleId, daywiseActivityDto,
                    academyDomain);
            return new ResponseEntity<>(response, response.getHttpStatus());
        } catch (ResourceException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing daywise activity
     */
    @PatchMapping("/daywise-activity")
    public ResponseEntity<ServiceResponse> updateDaywiseActivity(
            HttpServletRequest request,
            @RequestParam(name = "academyId", required = false) String academyId,
            @RequestBody DaywiseActivityDto daywiseActivityDto) {

        try {
            // Validate required ID
            if (daywiseActivityDto.getId() == null) {
                return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_REQUEST);
            }

            String academyDomain = "";
            if (StringUtils.hasText(academyId)) {
                academyDomain = academyDomainUtil.getAcademyDomain(academyId);
            } else {
                academyDomain = request.getHeader("origin");
            }

            ServiceResponse response = scheduleService.updateDaywiseActivity(daywiseActivityDto, academyDomain);
            return new ResponseEntity<>(response, response.getHttpStatus());
        } catch (ResourceException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a daywise activity by ID
     */
    @DeleteMapping("/daywise-activity")
    public ResponseEntity<ServiceResponse> deleteDaywiseActivity(
            @RequestParam(name = "id", required = true) Long id) {

        ServiceResponse response = scheduleService.deleteDaywiseActivity(id);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    /**
     * Add a new daywise activity mapping to an existing daywise activity
     */
    @PostMapping("/daywise-activity-mapping")
    public ResponseEntity<ServiceResponse> addDaywiseActivityMapping(
            HttpServletRequest request,
            @RequestParam(name = "daywiseActivityId", required = true) Long daywiseActivityId,
            @RequestParam(name = "academyId", required = false) String academyId,
            @RequestBody DaywiseActivityMappingDto daywiseActivityMappingDto) {

        try {
            String academyDomain = "";
            if (StringUtils.hasText(academyId) && !academyId.equalsIgnoreCase("null")) {
                academyDomain = academyDomainUtil.getAcademyDomain(academyId);
            } else {
                academyDomain = request.getHeader("origin");
            }

            ServiceResponse response = scheduleService.addDaywiseActivityMapping(daywiseActivityId,
                    daywiseActivityMappingDto, academyDomain);
            return new ResponseEntity<>(response, response.getHttpStatus());
        } catch (ResourceException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing daywise activity mapping
     */
    @PatchMapping("/daywise-activity-mapping")
    public ResponseEntity<ServiceResponse> updateDaywiseActivityMapping(
            HttpServletRequest request,
            @RequestParam(name = "academyId", required = false) String academyId,
            @RequestBody DaywiseActivityMappingDto daywiseActivityMappingDto) {

        try {
            // Validate required ID
            if (daywiseActivityMappingDto.getId() == null) {
                return ResponseBuilder.badRequestEntity(ApiResponse.INVALID_REQUEST);
            }

            String academyDomain = "";
            if (StringUtils.hasText(academyId)) {
                academyDomain = academyDomainUtil.getAcademyDomain(academyId);
            } else {
                academyDomain = request.getHeader("origin");
            }

            ServiceResponse response = scheduleService.updateDaywiseActivityMapping(daywiseActivityMappingDto,
                    academyDomain);
            return new ResponseEntity<>(response, response.getHttpStatus());
        } catch (ResourceException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a daywise activity mapping by ID
     */
    @DeleteMapping("/daywise-activity-mapping")
    public ResponseEntity<ServiceResponse> deleteDaywiseActivityMapping(
            @RequestParam(name = "id", required = true) Long id) {

        ServiceResponse response = scheduleService.deleteDaywiseActivityMapping(id);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    /**
     * Upload schedule file to S3 and map with programs
     */
    @PostMapping(value = "/upload")
    public ResponseEntity<ServiceResponse> uploadScheduleFile(HttpServletRequest request,
            @RequestPart(name = "file") MultipartFile file,
            @RequestPart(name = "programsMapping") String programsMappingJson) {
        try {
            String academyDomain = request.getHeader("origin");

            FileObjectDto fileObjectDto = new FileObjectDto();
            fileObjectDto.setOriginalFilename(file.getOriginalFilename());
            fileObjectDto.setContent(file.getBytes());
            fileObjectDto.setContentType(file.getContentType());

            // Parse JSON string to Map
            Map<String, List<String>> programsMapping = new ObjectMapper().readValue(
                    programsMappingJson, new TypeReference<Map<String, List<String>>>() {
                    });

            ScheduleFileUploadRequest uploadRequest = new ScheduleFileUploadRequest(fileObjectDto, programsMapping);
            ServiceResponse response = scheduleService.uploadScheduleFile(uploadRequest, academyDomain);
            return new ResponseEntity<>(response, response.getHttpStatus());
        } catch (ResourceException e) {
            return new ResponseEntity<ServiceResponse>(
                    ResponseBuilder.error(e.getMessage(), HttpStatus.valueOf(e.getErrorCodes().getHttpStatusCode())),
                    HttpStatus.valueOf(e.getErrorCodes().getHttpStatusCode()));
        } catch (IOException ioe) {
            log.error("Failed to upload the file to S3: {}", ioe);
            return new ResponseEntity<ServiceResponse>(
                    ResponseBuilder.error(ioe.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Timestamp parseTimestamp(String timestampStr) throws DateTimeParseException {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return null;
        }
        return Timestamp
                .valueOf(LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
    }
}
