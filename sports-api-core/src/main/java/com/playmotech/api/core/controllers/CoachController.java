package com.playmotech.api.core.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.Visibility;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.CoachPerformanceReportDto;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.EditTraineePerformanceReportRequestDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.SubmitCoachPerformanceReportRequestDto;
import com.playmotech.api.core.dto.SubmitTraineePerformanceReportRequestDto;
import com.playmotech.api.core.dto.TraineePerformanceReportDto;
import com.playmotech.api.core.dto.TraineePerformanceReportPdfDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.ICoachService;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.ITraineeService;
import com.playmotech.api.core.utils.AcademyDomainUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel
 **/

@Slf4j
@RestController
@RequestMapping("/coaches")
@AllArgsConstructor
public class CoachController extends BaseController {

	private final ICoachService coachService;
	private final ICourseService courseService;
	private final ITraineeService traineeService;
	private final AcademyDomainUtil academyDomainUtil;

	@GetMapping(value = "/academies", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<AcademyDto>>> enrolledAcademies() {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			List<AcademyDto> academyDtos = coachService.getAcademiesByCoachUserId(currentUser.getUserId());

			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<AcademyDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(academyDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<AcademyDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{coachUserId}/academies", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<AcademyDto>>> getAcademiesByCoach(
			@PathVariable("coachUserId") String coachUserId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			List<AcademyDto> academyDtos = coachService.getAcademiesByCoachUserId(coachUserId);

			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<AcademyDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(academyDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<AcademyDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/courses", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<CourseDto>>> getMyCourses(
			@RequestParam(value = "academyId", required = false) String academyId,
			@RequestParam(value = "isActive", required = false) Boolean isActive,
			@RequestParam(value = "visibility", required = false) Visibility visibility,
			@RequestParam(value = "filterTournaments", required = false) Boolean filterTournaments,
			@RequestParam(value = "t", required = false) String searchTxt) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			List<CourseDto> courseDtos = courseService.getCoursesByCoachUserId(currentUser.getUserId(), academyId,
					isActive, visibility, filterTournaments, searchTxt);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<CourseDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(courseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<CourseDto>>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{coachUserId}/courses", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<CourseDto>>> getCourseByCoachId(@PathVariable("coachUserId") String coachUserId,
			@RequestParam(value = "status", required = false) String academyId,
			@RequestParam(value = "isActive", required = false) Boolean isActive,
			@RequestParam(value = "visibility", required = false) Visibility visibility,
			@RequestParam(value = "filterTournaments", required = false) Boolean filterTournaments,
			@RequestParam(value = "t", required = false) String searchTxt) {
		try {
			List<CourseDto> courseDtos = courseService.getCoursesByCoachUserId(coachUserId, academyId, isActive,
					visibility, filterTournaments, searchTxt);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<CourseDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(courseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<CourseDto>>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{coachId}/performance", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<CoachPerformanceReportDto>> submitPerformanceReportCoach(
			@PathVariable("coachId") String coachId,
			@RequestParam(value = "file", required = false) MultipartFile[] mediaFile,
			@Valid SubmitCoachPerformanceReportRequestDto submitCoachPerformanceReportRequestDto) throws IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		try {
//            List<FileObjectDto> fileObjectDtos = new ArrayList<>();
//            if (mediaFile != null) {
//                for (MultipartFile file : mediaFile) {
//                    FileObjectDto fileObjectDto = new FileObjectDto();
//                    fileObjectDto.setOriginalFilename(file.getOriginalFilename());
//                    fileObjectDto.setContent(file.getBytes());
//                    fileObjectDto.setContentType(file.getContentType());
//                    fileObjectDtos.add(fileObjectDto);
//                }
//            }
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<CoachPerformanceReportDto>builder().status(HttpStatus.OK.value()).message("success")
							.body(coachService.submitCoachPerformanceReport(coachId, currentUser.getUserId(),
									submitCoachPerformanceReportRequestDto))
							.build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<CoachPerformanceReportDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/trainees/{traineeId}/performance", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<TraineePerformanceReportDto>> submitPerformanceReport(
			@PathVariable("traineeId") String traineeId,
			@RequestParam(value = "file", required = false) MultipartFile[] mediaFile,
			@Valid SubmitTraineePerformanceReportRequestDto submitTraineePerformanceRequestDto) throws IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		try {
			List<FileObjectDto> fileObjectDtos = new ArrayList<>();
			if (mediaFile != null) {
				for (MultipartFile file : mediaFile) {
					FileObjectDto fileObjectDto = new FileObjectDto();
					fileObjectDto.setOriginalFilename(file.getOriginalFilename());
					fileObjectDto.setContent(file.getBytes());
					fileObjectDto.setContentType(file.getContentType());
					fileObjectDtos.add(fileObjectDto);
				}
			}
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<TraineePerformanceReportDto>builder().status(HttpStatus.OK.value())
							.message("success")
							.body(traineeService.submitTraineePerformanceReport(currentUser.getUserId(), traineeId,
									submitTraineePerformanceRequestDto, fileObjectDtos))
							.build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<TraineePerformanceReportDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/trainees/{traineeId}/performance/{performanceReportId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<TraineePerformanceReportDto>> editPerformanceReport(
			@PathVariable("traineeId") String traineeId,
			@PathVariable("performanceReportId") String performanceReportId,
			@RequestParam(value = "file", required = false) MultipartFile[] mediaFile,
			@Valid EditTraineePerformanceReportRequestDto editTraineePerformanceReportRequestDto) throws IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		try {
			List<FileObjectDto> fileObjectDtos = new ArrayList<>();
			if (mediaFile != null) {
				for (MultipartFile file : mediaFile) {
					FileObjectDto fileObjectDto = new FileObjectDto();
					fileObjectDto.setOriginalFilename(file.getOriginalFilename());
					fileObjectDto.setContent(file.getBytes());
					fileObjectDto.setContentType(file.getContentType());
					fileObjectDtos.add(fileObjectDto);
				}
			}
			traineeService.editTraineePerformanceReport(performanceReportId, currentUser.getUserId(), traineeId,
					editTraineePerformanceReportRequestDto, fileObjectDtos);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<TraineePerformanceReportDto>builder()
					.status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<TraineePerformanceReportDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@DeleteMapping(value = "/trainees/{traineeId}/performance/{performanceReportId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<TraineePerformanceReportDto>> editPerformanceReport(
			@PathVariable("performanceReportId") String performanceReportId) throws IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		try {
			traineeService.deletePerformanceReport(currentUser.getUserId(), performanceReportId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<TraineePerformanceReportDto>builder()
					.status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<TraineePerformanceReportDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/trainees/performance/{reportId}/submit", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<TraineePerformanceReportDto>> submitPerformanceReport(
			@PathVariable("reportId") String reportId) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		try {
			traineeService.submitPerformanceReport(currentUser.getUserId(), reportId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<TraineePerformanceReportDto>builder()
					.status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<TraineePerformanceReportDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

//    @GetMapping(value = "/trainees/performance", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<Response<List<TraineePerformanceDto>>> getPerformanceReport(@RequestParam(value = "status", required = false) PerformanceReportStatus performanceReportStatus) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
//        try {
//            List<TraineePerformanceDto> traineePerformanceDtos = traineeService.getTraineePerformancesByCoachUserId(currentUser.getUserId(), performanceReportStatus);
//            return ResponseEntity.status(HttpStatus.OK).body(Response.<List<TraineePerformanceDto>>builder()
//                    .status(HttpStatus.OK.value()).message("success").body(traineePerformanceDtos).build());
//        } catch (ResourceException e) {
//            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<TraineePerformanceDto>>builder()
//                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
//        }
//    }

	@GetMapping(value = "/trainees/{traineeId}/performance/{performanceReportId}/pdf", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<TraineePerformanceReportPdfDto>> getPerformanceReportPdf(
			@PathVariable("performanceReportId") String performanceReportId) throws IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		try {
			TraineePerformanceReportPdfDto performanceReportPdfDto = traineeService
					.getPerformanceReportPdf(currentUser.getUserId(), performanceReportId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<TraineePerformanceReportPdfDto>builder()
					.status(HttpStatus.OK.value()).message("success").body(performanceReportPdfDto).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<TraineePerformanceReportPdfDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping("/list")
	public ResponseEntity<ServiceResponse> getCoachDetails(HttpServletRequest request,
			@RequestParam(name = "academyId", required = false) String academyId,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "programIds", required = false) List<String> courseIds,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(defaultValue = "false", required = false) boolean export) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			String academyDomain = "";
			if (StringUtils.hasText(academyId)) {
				academyDomain = academyDomainUtil.getAcademyDomain(academyId);
			} else {
				academyDomain = request.getHeader("origin");
			}

			ServiceResponse response = coachService.getList(currentUser.getUserId(), academyIds, courseIds, sports,
					academyDomain, export);
			return new ResponseEntity<>(response, response.getHttpStatus());

		} catch (ResourceException e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
