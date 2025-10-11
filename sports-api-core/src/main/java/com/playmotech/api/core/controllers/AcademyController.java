package com.playmotech.api.core.controllers;

import static com.playmotech.api.core.response.ApiResponse.INVALID_LENGTH_OR_REGEX;
import static com.playmotech.api.core.response.ApiResponse.INVALID_LISTING_FILTERS;
import static com.playmotech.api.core.response.ApiResponse.INVALID_REQUEST;
import static com.playmotech.api.core.response.ResponseBuilder.badRequestEntity;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.PerformanceReportStatus;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Visibility;
import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.AcademyLeadDto;
import com.playmotech.api.core.dto.AddCoachToAcademyRequestDto;
import com.playmotech.api.core.dto.AttendanceDetailDto;
import com.playmotech.api.core.dto.BranchDto;
import com.playmotech.api.core.dto.CoachDetailsDto;
import com.playmotech.api.core.dto.CourseAttendanceDto;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.CoursePaymentDetailsDto;
import com.playmotech.api.core.dto.CourtDto;
import com.playmotech.api.core.dto.CreateAcademyDto;
import com.playmotech.api.core.dto.CreateCourseDto;
import com.playmotech.api.core.dto.CreateGroupDto;
import com.playmotech.api.core.dto.EnquiryDto;
import com.playmotech.api.core.dto.EnquiryRequestDto;
import com.playmotech.api.core.dto.EnrollTraineeInCourseDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.GroupDto;
import com.playmotech.api.core.dto.MessageDto;
import com.playmotech.api.core.dto.PaymentDueDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.TeamDto;
import com.playmotech.api.core.dto.TeamPlayerDto;
import com.playmotech.api.core.dto.ToggleCoachStatusDto;
import com.playmotech.api.core.dto.ToggleTraineeStatusDto;
import com.playmotech.api.core.dto.TraineeAttendanceDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.TraineeDetailsDto;
import com.playmotech.api.core.dto.TraineePaymentDetailsDto;
import com.playmotech.api.core.dto.TraineePerformanceReportDto;
import com.playmotech.api.core.dto.UpdateAcademyDto;
import com.playmotech.api.core.dto.UpdateCourseDto;
import com.playmotech.api.core.dto.UpdateGroupDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IAttendanceService;
import com.playmotech.api.core.services.ICoachService;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.ICourtService;
import com.playmotech.api.core.services.IEnquiryService;
import com.playmotech.api.core.services.IGroupMessageService;
import com.playmotech.api.core.services.IGroupService;
import com.playmotech.api.core.services.IPaymentService;
import com.playmotech.api.core.services.IRolesService;
import com.playmotech.api.core.services.ITeamsService;
import com.playmotech.api.core.services.ITraineeService;
import com.playmotech.api.core.services.MasterService;
import com.playmotech.api.core.services.MiscellaneousService;
import com.playmotech.api.core.services.NewPaymentService;
import com.playmotech.api.core.services.PerformanceService;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel
 **/

@Slf4j
@RestController
@RequestMapping("/academies")
@AllArgsConstructor
public class AcademyController extends BaseController {

	private final IAcademyService academyService;
	private final ITraineeService traineeService;
	private final ICoachService coachService;
	private final ICourseService courseService;
	private final ICourtService courtService;
	private final ITeamsService teamsService;
	private final IAttendanceService attendanceService;
	private final IGroupService groupService;
	private final IGroupMessageService groupMessageService;
	private final IPaymentService paymentService;
	private final IEnquiryService enquiryService;
	private final IRolesService rolesService;
	private final MasterService masterService;
	private final MiscellaneousService miscellaneousService;
	private final PerformanceService performanceService;

	private final NewPaymentService newPaymentService;

	@GetMapping("sports")
	public ResponseEntity<ServiceResponse> getAllSports(@RequestParam(required = false) String academyId) {

		ServiceResponse response;
		if (academyId != null && !academyId.isBlank()) {
			response = miscellaneousService.getSportsByAcademy(academyId);
		} else {
			response = miscellaneousService.getAllSports();
		}

		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("age-categories")
	public ResponseEntity<ServiceResponse> getAlLAgeCategories() {
		ServiceResponse response = miscellaneousService.getAllAgeCategories();
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("genders")
	public ResponseEntity<ServiceResponse> getAllGenders() {
		ServiceResponse response = miscellaneousService.getAllGenders();
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@PostMapping(value = "/lead", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> createLead(@RequestBody @Valid AcademyLeadDto academyLeadDto) {
		academyService.addLead(academyLeadDto);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(Response.<UserProfileDto>builder().status(HttpStatus.CREATED.value()).message("success").build());
	}

	@GetMapping("master")
	public ResponseEntity<ServiceResponse> getMasterData(
			@RequestParam(name = "active", required = false) String category) {
		ServiceResponse response = masterService.getCategoryStructure(category);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<AcademyDto>> addAcademy(@RequestBody @Valid CreateAcademyDto academyDto,
			@RequestParam(value = "sendotp", defaultValue = "false") Boolean sendOtp) {
		try {
			AcademyDto updatedAcademyDto = academyService.registerAcademy(academyDto, sendOtp);

			AddCoachToAcademyRequestDto addCoachToAcademyRequestDto = new AddCoachToAcademyRequestDto();
			addCoachToAcademyRequestDto.setCoachUserId(updatedAcademyDto.getManagerUserId());
			addCoachToAcademyRequestDto.setDesignation("Manager");
			addCoachToAcademyRequestDto.setExperienceInMonths(0);

			if (academyDto.getRoleId() != null) {
				addCoachToAcademyRequestDto.setRoleId(academyDto.getRoleId());
			} else {
				Roles role = rolesService.getRoleByName("owner");
				addCoachToAcademyRequestDto.setRoleId(role.getId().intValue());
			}

			coachService.addCoachToAcademy(List.of(addCoachToAcademyRequestDto), updatedAcademyDto.getId());

			return ResponseEntity.status(HttpStatus.CREATED).body(Response.<AcademyDto>builder()
					.status(HttpStatus.CREATED.value()).message("success").body(updatedAcademyDto).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	// @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces =
	// MediaType.APPLICATION_JSON_VALUE)
	// public ResponseEntity<Response<AcademyDto>> addAcademyBE(@RequestBody @Valid
	// AcademyDto academyDto) {
	// try {
	// AcademyDto updatedAcademyDto = academyService.registerAcademy(academyDto);
	// return
	// ResponseEntity.status(HttpStatus.CREATED).body(Response.<AcademyDto>builder()
	// .status(HttpStatus.CREATED.value()).message("success").body(updatedAcademyDto).build());
	// } catch (ResourceException e) {
	// return
	// ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
	// .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
	// }
	// }

	@PutMapping(value = "/{academyId}/picture", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<String>> updatePicture(@PathVariable("academyId") String academyId,
			@RequestParam(value = "file", required = false) MultipartFile mediaFile) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			if (mediaFile == null) {
				throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED, "file is required.");
			}
			FileObjectDto fileObjectDto = new FileObjectDto();
			fileObjectDto.setOriginalFilename(mediaFile.getOriginalFilename());
			fileObjectDto.setContent(mediaFile.getBytes());
			fileObjectDto.setContentType(mediaFile.getContentType());
			String academyPicture = academyService.updateAcademyPicture(academyId, currentUser.getUserId(),
					fileObjectDto);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<String>builder().status(HttpStatus.OK.value())
					.message("success").body(academyPicture).build());
		} catch (ResourceException e) {
			log.error("Failed to save Academy Profile picture.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<String>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		} catch (IOException e) {
			log.error("Failed to save Academy Profile picture", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.<String>builder()
					.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/{academyId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<AcademyDto>> updateAcademy(@PathVariable("academyId") String academyId,
			@Valid @RequestBody UpdateAcademyDto updateAcademyDto) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<AcademyDto>builder().status(HttpStatus.OK.value()).message("success")
							.body(academyService.updateAcademy(academyId, currentUser.getUserId(), updateAcademyDto))
							.build());
		} catch (ResourceException e) {
			log.error("Failed to update academy.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<AcademyDto>>> getAcademies() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		List<AcademyDto> academyDtos = academyService.getAcademyByManagerUserId(currentUser.getUserId());
		return ResponseEntity.status(HttpStatus.OK).body(Response.<List<AcademyDto>>builder()
				.status(HttpStatus.OK.value()).message("success").body(academyDtos).build());
	}

	@GetMapping(value = "/{academyId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<AcademyDto>> addAcademy(@PathVariable("academyId") String academyId) {
		try {
			AcademyDto academyDto = academyService.getAcademyById(academyId);
			return ResponseEntity.status(HttpStatus.CREATED).body(Response.<AcademyDto>builder()
					.status(HttpStatus.CREATED.value()).message("success").body(academyDto).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@DeleteMapping(value = "/{academyId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<AcademyDto>> deleteAcademy(@PathVariable("academyId") String academyId) {
		try {
			boolean success = academyService.deleteAcademy(academyId);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<AcademyDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/branches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<BranchDto>> addBranch(@PathVariable("academyId") String academyId,
			@RequestBody @Valid BranchDto branchDto) {

		try {
			BranchDto updateBranchDto = academyService.addBranchToAcademy(academyId, branchDto);
			return ResponseEntity.status(HttpStatus.CREATED).body(Response.<BranchDto>builder()
					.status(HttpStatus.CREATED.value()).message("success").body(updateBranchDto).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<BranchDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@DeleteMapping(value = "/{academyId}/branches/{branchId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<AcademyDto>> deleteBranch(@PathVariable("academyId") String academyId,
			@PathVariable("branchId") String branchId) {
		try {
			boolean success = academyService.deleteBranchFromAcademy(academyId, branchId);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<AcademyDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/trainees", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> addTrainees(@PathVariable("academyId") String academyId,
			@RequestBody List<String> traineeUserIds) {
		try {
			traineeService.addTraineesToAcademy(traineeUserIds, academyId);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Response.<BranchDto>builder().status(HttpStatus.CREATED.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(
					Response.builder().status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/trainees/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> toggleTraineeStatus(@PathVariable("academyId") String academyId,
			@RequestBody ToggleTraineeStatusDto toggleTraineeStatusDto) {
		try {
			traineeService.toggleTraineeStatusInAcademy(academyId, toggleTraineeStatusDto);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<BranchDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@DeleteMapping(value = "/{academyId}/trainees", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> deleteTrainees(@PathVariable("academyId") String academyId,
			@RequestBody List<String> traineeUserIds) {
		try {
			traineeService.removeTraineesFromAcademy(traineeUserIds, academyId);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<BranchDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(
					Response.builder().status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/trainees", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<TraineeDetailsDto>>> getTrainees(@PathVariable("academyId") String academyId,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "phone", required = false) String phone,
			@RequestParam(name = "t", required = false) String searchTxt) {
		try {
			List<TraineeDetailsDto> trainees = traineeService.getTraineesByAcademyIdAndNameAndPhoneNumber(academyId,
					name, phone, searchTxt);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<TraineeDetailsDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(trainees).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<TraineeDetailsDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/trainees/registered", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<UserProfileMinDto>>> getRegisteredTraineesDetails(
			@PathVariable("academyId") String academyId) {
		try {
			List<UserProfileMinDto> trainees = traineeService.getRegisteredUsers(academyId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<UserProfileMinDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(trainees).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<UserProfileMinDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/trainees/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<TraineeDetailsDto>> getTrainees(@PathVariable("academyId") String academyId,
			@PathVariable("userId") String userId) {
		try {
			TraineeDetailsDto trainee = traineeService.getTraineeById(academyId, userId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<TraineeDetailsDto>builder()
					.status(HttpStatus.OK.value()).message("success").body(trainee).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<TraineeDetailsDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/trainees/{traineeId}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<TraineePaymentDetailsDto>>> getTraineePayments(
			@PathVariable("academyId") String academyId, @PathVariable("traineeId") String traineeId,
			@RequestParam(value = "paymentCategory", defaultValue = "REGISTRATION_FEE", required = false) PaymentCategory paymentCategory) {
		try {

			// TODO- New Payment Changes
			// List<TraineePaymentDetailsDto> traineePaymentDetailsDtos =
			// traineeService.getTraineePayments(academyId,
			// traineeId, paymentCategory);
			List<TraineePaymentDetailsDto> traineePaymentDetailsDtos = traineeService.getNewTraineePayments(academyId,
					traineeId, paymentCategory);

			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<TraineePaymentDetailsDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(traineePaymentDetailsDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<TraineePaymentDetailsDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/trainees/{traineeId}/paymentss", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<TraineePaymentDetailsDto>>> getNewTraineePayments(
			@PathVariable("academyId") String academyId, @PathVariable("traineeId") String traineeId,
			@RequestParam(value = "paymentCategory", defaultValue = "REGISTRATION_FEE", required = false) PaymentCategory paymentCategory) {
		try {
			List<TraineePaymentDetailsDto> traineePaymentDetailsDtos = traineeService.getNewTraineePayments(academyId,
					traineeId, paymentCategory);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<TraineePaymentDetailsDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(traineePaymentDetailsDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<TraineePaymentDetailsDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/payments/dues", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<PaymentDueDto>>> getDuePayments(@PathVariable("academyId") String academyId,
			@RequestParam(value = "paymentCategory", defaultValue = "REGISTRATION_FEE", required = false) PaymentCategory paymentCategory) {
		try {

			// TODO- New Payment Changes
			// List<PaymentDueDto> payments = paymentService.getPaymentDueDto(academyId);
			List<PaymentDueDto> payments = newPaymentService.getPaymentDueDto(academyId, paymentCategory);

			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<PaymentDueDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(payments).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<PaymentDueDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/paymentss/dues", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<PaymentDueDto>>> getNewDuePayments(@PathVariable("academyId") String academyId,
			@RequestParam(value = "paymentCategory", defaultValue = "REGISTRATION_FEE", required = false) PaymentCategory paymentCategory) {
		try {
			List<PaymentDueDto> payments = newPaymentService.getPaymentDueDto(academyId, paymentCategory);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<PaymentDueDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(payments).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<PaymentDueDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/coaches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> addCoaches(@PathVariable("academyId") String academyId,
			@RequestBody List<AddCoachToAcademyRequestDto> coaches) {
		try {
			coachService.addCoachToAcademy(coaches, academyId);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Response.<BranchDto>builder().status(HttpStatus.CREATED.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/{academyId}/coaches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> updateCoaches(@PathVariable("academyId") String academyId,
			@RequestBody List<AddCoachToAcademyRequestDto> coaches) {
		try {
			coachService.updateCoachInAcademy(coaches, academyId);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<BranchDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/coaches/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> toggleCoachStatus(@PathVariable("academyId") String academyId,
			@RequestBody ToggleCoachStatusDto toggleCoachStatusDto) {
		try {
			coachService.toggleCoachesStatusInAcademy(academyId, toggleCoachStatusDto);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<BranchDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@DeleteMapping(value = "/{academyId}/coaches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> deleteCoachesFromAcademy(@PathVariable("academyId") String academyId,
			@RequestBody List<String> coaches) {
		try {
			coachService.removeCoachesFromAcademy(coaches, academyId);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<BranchDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<AcademyDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/coaches", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<CoachDetailsDto>>> getCoaches(@PathVariable("academyId") String academyId,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "phone", required = false) String phone,
			@RequestParam(name = "t", required = false) String searchTxt,
			@RequestParam(name = "coach", required = false) boolean coach) {
		try {
			List<CoachDetailsDto> coaches = coachService.getCoachesByAcademyIdAndNameAndPhoneNumber(academyId, name,
					phone, searchTxt, coach);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<CoachDetailsDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(coaches).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<CoachDetailsDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/coaches/trainees/performance", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<TraineePerformanceReportDto>>> getPerformanceReport(
			@PathVariable("academyId") String academyId,
			@RequestParam(value = "status", required = false) PerformanceReportStatus performanceReportStatus,
			@RequestParam(value = "courseId", required = false) String courseId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		try {
			// Here coachUserId can be managerUserId or coachUserId only
			List<TraineePerformanceReportDto> traineePerformanceDtos = traineeService
					.getTraineePerformancesByCoachUserIdAndAcademyId(academyId, currentUser.getUserId(),
							performanceReportStatus, courseId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<TraineePerformanceReportDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(traineePerformanceDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<TraineePerformanceReportDto>>builder()
							.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/coaches/trainees/{traineeId}/performance", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<TraineePerformanceReportDto>>> getPerformanceReport(
			@PathVariable("academyId") String academyId, @PathVariable("traineeId") String traineeId,
			@RequestParam(value = "status", required = false) PerformanceReportStatus performanceReportStatus,
			@RequestParam(value = "courseId", required = false) String courseId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		try {
			// Here coachUserId can be either academy owner or coach
			List<TraineePerformanceReportDto> traineePerformanceDtos = traineeService
					.getTraineePerformancesByCoachUserIdAndTraineeIdAndAcademyId(academyId, currentUser.getUserId(),
							traineeId, performanceReportStatus, courseId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<TraineePerformanceReportDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(traineePerformanceDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<TraineePerformanceReportDto>>builder()
							.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping("performance/program-feedback/list")
	public ResponseEntity<ServiceResponse> getCoachPerformanceList(
			@RequestParam(name = "orderBy", defaultValue = "createdOn", required = false) String orderBy,
			@RequestParam(name = "search", defaultValue = "", required = false) String search,
			@RequestParam(name = "startDate", required = false) String startDate,
			@RequestParam(name = "endDate", required = false) String endDate,
			@RequestParam(name = "currentPage", defaultValue = "1", required = false) Short currentPage,
			@RequestParam(name = "pageSize", defaultValue = "10", required = false) Short pageSize,
			@RequestParam(name = "ascending", defaultValue = "true", required = false) boolean ascending,
			@RequestParam(name = "pageable", defaultValue = "false", required = false) boolean pageable,
			@RequestParam(name = "active", defaultValue = "true", required = false) boolean active,
			@RequestParam(name = "userId", required = false) String userId,
			@RequestParam(name = "academyIds", required = false) List<String> academyIds,
			@RequestParam(name = "programIds", required = false) List<String> programIds,
			@RequestParam(name = "playerIds", required = false) List<String> playerIds,
			@RequestParam(name = "coachIds", required = false) List<String> coachIds,
			@RequestParam(name = "sports", required = false) List<String> sports,
			@RequestParam(name = "ageCategories", required = false) List<String> ageCategories,
			@RequestParam(name = "app", defaultValue = "true", required = false) boolean app) {

		// Validate pagination parameters
		if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
			return badRequestEntity(INVALID_REQUEST);
		}

		// Validate orderBy parameter
		String[] allowedOrderByValues = { "playerName", "reportTitle", "coachName", "reportStatus", "academy", "branch",
				"program", "createdOn" };
		if (!Arrays.asList(allowedOrderByValues).contains(orderBy)) {
			return badRequestEntity(INVALID_LISTING_FILTERS.getMessage() + Arrays.toString(allowedOrderByValues));
		}

		Timestamp startDateLocal = null;
		Timestamp endDateLocal = null;

		if ((startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
			try {
				startDateLocal = parseTimestamp(startDate);
				endDateLocal = parseTimestamp(endDate);
				if (startDateLocal == null || endDateLocal == null
						|| startDateLocal.toLocalDateTime().isAfter(endDateLocal.toLocalDateTime())) {
					return ResponseBuilder.badRequestEntity(INVALID_LISTING_FILTERS.message);
				}
			} catch (DateTimeParseException e) {
				return ResponseBuilder.badRequestEntity(INVALID_LENGTH_OR_REGEX.message + "yyyy-MM-dd HH:mm:ss.SSSSSS");
			}
		}

		// Build filter object
		GenericFilter filter = GenericFilter.builder().userId(userId).isPageable(pageable).currentPage(currentPage)
				.pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy).notDeleted(active)
				.playerIds(playerIds).coachIds(coachIds).academyIds(academyIds).endDate(endDateLocal)
				.startDate(startDateLocal).app(app).programIds(programIds).sports(sports).ageCategory(ageCategories)
				.build();

		ServiceResponse response = performanceService.getCoachPerformanceList(filter);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	private static Timestamp parseTimestamp(String timestampStr) {
		if (timestampStr == null || timestampStr.isEmpty()) {
			return null;
		}
		try {
			return Timestamp.valueOf(
					LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	@PostMapping(value = "/{academyId}/courses", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<CourseDto>> addCourse(@PathVariable("academyId") String academyId,
			@Valid @RequestBody CreateCourseDto createCourseDto) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			CourseDto courseDto1 = courseService.createCourse(academyId, currentUser.getUserId(), createCourseDto,
					false);
			return ResponseEntity.status(HttpStatus.CREATED).body(Response.<CourseDto>builder()
					.status(HttpStatus.CREATED.value()).message("success").body(courseDto1).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourseDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/courses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Response<CourseDto>> addCourseWithFile(@PathVariable("academyId") String academyId,
			@RequestPart("course") @Valid CreateCourseDto createCourseDto,
			@RequestPart(value = "file", required = false) MultipartFile file) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			CourseDto courseDto1 = courseService.createCourseWithFile(academyId, currentUser.getUserId(),
					createCourseDto, false, file);
			return ResponseEntity.status(HttpStatus.CREATED).body(Response.<CourseDto>builder()
					.status(HttpStatus.CREATED.value()).message("success").body(courseDto1).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourseDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/{academyId}/courses/{courseId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<CourseDto>> updateCourse(@PathVariable("academyId") String academyId,
			@PathVariable("courseId") String courseId, @Valid @RequestBody UpdateCourseDto updateCourseDto) {
		try {
			CourseDto courseDto1 = courseService.updateCourse(academyId, courseId, updateCourseDto);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<CourseDto>builder().status(HttpStatus.OK.value())
					.message("success").body(courseDto1).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourseDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/{academyId}/courses/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Response<CourseDto>> updateCourseWithFile(@PathVariable("academyId") String academyId,
			@PathVariable("courseId") String courseId, @RequestPart("course") @Valid UpdateCourseDto updateCourseDto,
			@RequestPart(value = "file", required = false) MultipartFile file) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			CourseDto courseDto = courseService.updateCourseWithFile(currentUser.getUserId(), academyId, courseId,
					updateCourseDto, file);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<CourseDto>builder().status(HttpStatus.OK.value())
					.message("success").body(courseDto).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourseDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/{academyId}/courses/{courseId}/picture", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<String>> updateCoursePicture(@PathVariable("academyId") String academyId,
			@PathVariable("courseId") String courseId,
			@RequestParam(value = "file", required = false) MultipartFile mediaFile) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			if (mediaFile == null) {
				throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED, "file is required.");
			}
			FileObjectDto fileObjectDto = new FileObjectDto();
			fileObjectDto.setOriginalFilename(mediaFile.getOriginalFilename());
			fileObjectDto.setContent(mediaFile.getBytes());
			fileObjectDto.setContentType(mediaFile.getContentType());
			String coursePicture = courseService.updateCoursePicture(academyId, courseId, currentUser.getUserId(),
					fileObjectDto);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<String>builder().status(HttpStatus.OK.value())
					.message("success").body(coursePicture).build());
		} catch (ResourceException e) {
			log.error("Failed to save Course Profile picture.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<String>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		} catch (IOException e) {
			log.error("Failed to save Course Profile picture", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.<String>builder()
					.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/courses", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<CourseDto>>> getCourseByTraineeId(@PathVariable("academyId") String academyId,
			@RequestParam(value = "traineeUserId", required = false) String traineeId,
			@RequestParam(name = "sport", required = false) Sports sport,
			@RequestParam(value = "isActive", defaultValue = "true") Boolean isActive,
			@RequestParam(name = "visibility", required = false) Visibility visibility,
			@RequestParam(value = "filterTournaments", required = false) Boolean filterTournaments,
			@RequestParam(value = "excludeEnrolledPrograms", required = false) Boolean excludeEnrolledCourses,
			@RequestParam(value = "t", required = false) String searchTxt,
			@RequestParam(value = "orgId", required = false, defaultValue = "") String orgId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			List<CourseDto> coursesDto = courseService.getCoursesByAcademyId(currentUser.getUserId(), academyId,
					traineeId, sport, isActive, visibility, filterTournaments, excludeEnrolledCourses, searchTxt,
					StringUtils.hasText(orgId) ? orgId : null);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<CourseDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(coursesDto).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<CourseDto>>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/courses/{courseId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<CourseDto>> getCourse(@PathVariable("academyId") String academyId,
			@PathVariable("courseId") String courseId) {
		try {
			CourseDto courseDto = courseService.getCourse(academyId, courseId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<CourseDto>builder().status(HttpStatus.OK.value())
					.message("success").body(courseDto).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourseDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/courses/{courseId}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<CoursePaymentDetailsDto>>> getCoursePayments(
			@PathVariable("academyId") String academyId, @PathVariable("courseId") String courseId,
			@RequestParam(value = "paymentCategory", defaultValue = "COURSE_FEE", required = false) PaymentCategory paymentCategory) {
		try {

			// TODO- New Payment Changes
			// List<CoursePaymentDetailsDto> coursePaymentDetailsDtos =
			// paymentService.getCoursePaymentDetails(academyId,
			// courseId, paymentCategory);

			List<CoursePaymentDetailsDto> coursePaymentDetailsDtos = newPaymentService
					.getCoursePaymentDetails(academyId, courseId, paymentCategory);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<CoursePaymentDetailsDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(coursePaymentDetailsDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<CoursePaymentDetailsDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/courses/{courseId}/paymentss", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<CoursePaymentDetailsDto>>> getCoursePaymentss(
			@PathVariable("academyId") String academyId, @PathVariable("courseId") String courseId,
			@RequestParam(value = "paymentCategory", defaultValue = "COURSE_FEE", required = false) PaymentCategory paymentCategory) {
		try {
			List<CoursePaymentDetailsDto> coursePaymentDetailsDtos = newPaymentService
					.getCoursePaymentDetails(academyId, courseId, paymentCategory);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<CoursePaymentDetailsDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(coursePaymentDetailsDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<CoursePaymentDetailsDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/courses/{courseId}/trainees", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<CourseDto>> enrollTraineeInCourse(@PathVariable("academyId") String academyId,
			@PathVariable("courseId") String courseId, @RequestBody EnrollTraineeInCourseDto enrollTraineeInCourseDto) {
		try {
			// TODO: UI is sending dueDate as null for payment type FULL. As discussed with
			// Sanjay, fixing this via backend as on 09-10-2025
			if (PaymentSchedule.FULL.equals(enrollTraineeInCourseDto.getPaymentSchedule())
					&& enrollTraineeInCourseDto.getDueDate() == null) {
				log.error("Received due date as: {}", enrollTraineeInCourseDto.getDueDate());
				log.error("Setting due date as joining date. Joining Date is {}",
						enrollTraineeInCourseDto.getJoiningDate().toString());
				enrollTraineeInCourseDto.setDueDate(enrollTraineeInCourseDto.getJoiningDate());
			}
			courseService.enrollTraineesInCourseWithPaymentOptions(academyId, courseId, enrollTraineeInCourseDto);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Response.<CourseDto>builder().status(HttpStatus.CREATED.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourseDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/{academyId}/courses/{courseId}/trainees/{enrollmentId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<CourseDto>> updateTraineeEnrollment(@PathVariable("academyId") String academyId,
			@PathVariable("courseId") String courseId, @PathVariable("enrollmentId") String enrollmentId,
			@RequestBody EnrollTraineeInCourseDto updateTraineeEnrollmentDto) {
		try {
			courseService.updateTraineeEnrollment(academyId, courseId, enrollmentId, updateTraineeEnrollmentDto);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<CourseDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourseDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/courses/{courseId}/coaches", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<UserProfileMinDto>>> getCourseCoaches(
			@PathVariable("academyId") String academyId, @PathVariable("courseId") String courseId) {
		List<UserProfileMinDto> coaches = courseService.getCoachesByCourse(courseId);
		return ResponseEntity.status(HttpStatus.OK).body(Response.<List<UserProfileMinDto>>builder()
				.status(HttpStatus.OK.value()).message("success").body(coaches).build());
	}

	@GetMapping(value = "/{academyId}/courses/{courseId}/trainees", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<TraineeCourseEnrollmentDto>>> enrollTraineeInCourse(
			@PathVariable("academyId") String academyId, @PathVariable("courseId") String courseId) {
		try {
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<List<TraineeCourseEnrollmentDto>>builder().status(HttpStatus.OK.value())
							.message("success").body(courseService.getEnrolledTrainees(academyId, courseId)).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<TraineeCourseEnrollmentDto>>builder()
							.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/courses/{courseId}/attendance", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> markAttendance(@PathVariable("academyId") String academyId,
			@PathVariable("courseId") String courseId, @RequestParam("date") String date,
			// @RequestBody Map<String, Boolean> attendance) {
			@RequestBody Map<String, AttendanceDetailDto> attendance) {

		ServiceResponse serviceResponse = attendanceService.markAttendance(academyId, courseId, date, attendance);
		return ResponseEntity.status(serviceResponse.getHttpStatus()).body(serviceResponse);
	}

	@PostMapping(value = "/{academyId}/courses/{courseId}/attendance/v2", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> markAttendanceV2(@PathVariable("academyId") String academyId,
			@PathVariable("courseId") String courseId, @RequestParam("date") String date,
			@RequestBody Map<String, AttendanceDetailDto> attendance) {

		ServiceResponse serviceResponse = attendanceService.markAttendanceV2(academyId, courseId, date, attendance);
		return ResponseEntity.status(serviceResponse.getHttpStatus()).body(serviceResponse);
	}

	@GetMapping(value = "/{academyId}/courses/{courseId}/attendance", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<TraineeAttendanceDto>>> getAttendance(
			@PathVariable("academyId") String academyId, @PathVariable("courseId") String courseId,
			@RequestParam("date") String date) {
		try {
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<List<TraineeAttendanceDto>>builder().status(HttpStatus.OK.value())
							.message("success").body(attendanceService.getAttendance(academyId, courseId, date))
							.build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<TraineeAttendanceDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/trainees/{traineeId}/attendance", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<CourseAttendanceDto>>> getAttendanceByTrainee(
			@PathVariable("academyId") String academyId, @PathVariable("traineeId") String traineeId) {
		try {
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<List<CourseAttendanceDto>>builder().status(HttpStatus.OK.value()).message("success")
							.body(attendanceService.getAttendanceByTrainee(academyId, traineeId)).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<CourseAttendanceDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/courts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<CourtDto>> addCourse(@PathVariable("academyId") String academyId,
			@RequestParam(name = "sports", defaultValue = "BADMINTON", required = true) Sports sport,
			@Valid @RequestBody CourtDto courtDto) {
		try {
			courtDto.setSports(sport);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Response.<CourtDto>builder().status(HttpStatus.CREATED.value()).message("success")
							.body(courtService.addCourt(academyId, courtDto)).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourtDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/courts", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<CourtDto>>> getCourts(@PathVariable("academyId") String academyId,
			@RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
			@RequestParam(value = "t", required = false) String searchTxt) {
		try {
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<List<CourtDto>>builder().status(HttpStatus.OK.value()).message("success")
							.body(courtService.getCourts(academyId, searchTxt, sport)).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<CourtDto>>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/courts/{courtId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<CourtDto>> getCourt(@PathVariable("academyId") String academyId,
			@PathVariable("courtId") String courtId) {
		try {
			return ResponseEntity.status(HttpStatus.OK).body(Response.<CourtDto>builder().status(HttpStatus.OK.value())
					.message("success").body(courtService.getCourt(academyId, courtId)).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourtDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@DeleteMapping(value = "/{academyId}/courts/{courtId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<CourtDto>> deleteCourt(@PathVariable("academyId") String academyId,
			@PathVariable("courtId") String courtId) {
		try {
			courtService.deleteCourt(academyId, courtId);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<CourtDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CourtDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/teams", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<TeamDto>> addTeam(
			@RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
			@PathVariable("academyId") String academyId, @Valid @RequestBody TeamDto teamDto) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Response.<TeamDto>builder().status(HttpStatus.CREATED.value()).message("success")
							.body(teamsService.addTeam(currentUser.getUserId(), academyId, teamDto, sport)).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<TeamDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/teams", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<TeamDto>>> getTeams(@PathVariable("academyId") String academyId,
			@RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
			@RequestParam(value = "badmintonTournamentId", required = false) String tournamentId,
			@RequestParam(value = "t", required = false) String searchTxt) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response
							.<List<TeamDto>>builder().status(HttpStatus.OK.value()).message("success").body(teamsService
									.getTeams(currentUser.getUserId(), academyId, tournamentId, searchTxt, sport))
							.build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<TeamDto>>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@DeleteMapping(value = "/{academyId}/teams/{teamId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<TeamDto>> deleteTeam(@PathVariable("academyId") String academyId,
			@PathVariable("teamId") String teamId, @RequestParam(defaultValue = "BADMINTON") Sports sport,
			@RequestParam(value = "disassociateTournament", required = false, defaultValue = "false") Boolean disassociateTournament,
			@RequestParam(value = "badmintonTournamentId", required = false) String badmintonTournamentId) {
		try {
			teamsService.deleteTeam(academyId, teamId, disassociateTournament, badmintonTournamentId, sport);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<TeamDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<TeamDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/groups", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<GroupDto>> createGroup(@PathVariable("academyId") String academyId,
			@Valid @RequestBody CreateGroupDto createGroupDto) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Response.<GroupDto>builder().status(HttpStatus.CREATED.value()).message("success")
							.body(groupService.createGroup(academyId, currentUser.getUserId(), createGroupDto))
							.build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<GroupDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/groups", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<GroupDto>>> getGroups(@PathVariable("academyId") String academyId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<List<GroupDto>>builder().status(HttpStatus.OK.value()).message("success")
							.body(groupService.getGroups(academyId, currentUser.getUserId())).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<GroupDto>>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/{academyId}/groups/{groupId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<GroupDto>> deleteGroup(@PathVariable("academyId") String academyId,
			@PathVariable("groupId") String groupId, @RequestBody UpdateGroupDto updateGroupDto) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			groupService.updateGroup(currentUser.getUserId(), academyId, groupId, updateGroupDto);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<GroupDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<GroupDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@DeleteMapping(value = "/{academyId}/groups/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<GroupDto>> deleteGroup(@PathVariable("academyId") String academyId,
			@PathVariable("groupId") String groupId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			groupService.deleteGroup(currentUser.getUserId(), academyId, groupId);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<GroupDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<GroupDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@DeleteMapping(value = "/{academyId}/groups", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<GroupDto>> deleteGroup(@PathVariable("academyId") String academyId,
			@RequestBody List<String> groupIds) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			groupService.deleteGroups(currentUser.getUserId(), academyId, groupIds);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<GroupDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<GroupDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/groups/{groupId}/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<GroupDto>> addMembersToGroup(@PathVariable("academyId") String academyId,
			@PathVariable("groupId") String groupId, @RequestBody List<String> members) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			groupService.addMembersToGroup(currentUser.getUserId(), academyId, groupId, members);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<GroupDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<GroupDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/groups/{groupId}/remove", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<GroupDto>> removeMembersFromGroup(@PathVariable("academyId") String academyId,
			@PathVariable("groupId") String groupId, @RequestBody List<String> members) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			groupService.removeMembersToGroup(currentUser.getUserId(), academyId, groupId, members);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<GroupDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<GroupDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/{academyId}/groups/{groupId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<MessageDto>> sendMessageWithMedia(@PathVariable("academyId") String academyId,
			@PathVariable("groupId") String groupId,
			@RequestParam(value = "file", required = false) MultipartFile mediaFile, @Valid MessageDto messageDto) {
		try {
			FileObjectDto fileObjectDto = null;
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			if (mediaFile == null && StringUtils.isEmpty(messageDto.getMessage())) {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Message or media file is required");
			}
			if (mediaFile != null) {
				fileObjectDto = new FileObjectDto();
				fileObjectDto.setOriginalFilename(mediaFile.getOriginalFilename());
				fileObjectDto.setContent(mediaFile.getBytes());
				fileObjectDto.setContentType(mediaFile.getContentType());
			}
			MessageDto messageDto1 = groupMessageService.sendMessage(currentUser.getUserId(), academyId, groupId,
					messageDto, fileObjectDto);
			return ResponseEntity.status(HttpStatus.CREATED).body(Response.<MessageDto>builder()
					.status(HttpStatus.CREATED.value()).message("success").body(messageDto1).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<MessageDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.<MessageDto>builder()
					.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/groups/{groupId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> getGroupMessages(@PathVariable("academyId") String academyId,
			@PathVariable("groupId") String groupId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.builder().status(HttpStatus.OK.value()).message("success")
							.body(groupMessageService.getGroupMessages(currentUser.getUserId(), academyId, groupId))
							.build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(
					Response.builder().status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/groups/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> getGroupWithMessage(@PathVariable("academyId") String academyId,
			@PathVariable("groupId") String groupId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			return ResponseEntity.status(HttpStatus.OK).body(Response.builder().status(HttpStatus.OK.value())
					.message("success")
					.body(groupMessageService.getGroupDetailsWithMessages(currentUser.getUserId(), academyId, groupId))
					.build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(
					Response.builder().status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/{academyId}/enquiries/{enquiryId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<EnquiryDto>> updateEnquiry(@PathVariable("academyId") String academyId,
			@PathVariable("enquiryId") String enquiryId, @RequestBody EnquiryRequestDto enquiryRequestDto) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		try {
			EnquiryDto updatedEnquiry = enquiryService.updateEnquiry(academyId, enquiryId, enquiryRequestDto,
					currentUser);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<EnquiryDto>builder().status(HttpStatus.OK.value())
							.message("Enquiry updated successfully").body(updatedEnquiry).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<EnquiryDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/{academyId}/enquiries", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<EnquiryDto>>> getAllEnquiriesForUser(
			@PathVariable("academyId") String academyId,
			@RequestParam(value = "status", required = false) String status) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		try {
			List<EnquiryDto> enquiries = enquiryService.getAllEnquiriesForUser(academyId, currentUser.getUserId(),
					org.apache.commons.lang3.StringUtils.defaultIfBlank(status,
							org.apache.commons.lang3.StringUtils.EMPTY));
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<EnquiryDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(enquiries).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<EnquiryDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@DeleteMapping(value = "/{academyId}/enquiries/{enquiryId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<Void>> deleteEnquiry(@PathVariable("academyId") String academyId,
			@PathVariable("enquiryId") String enquiryId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		try {
			enquiryService.deleteEnquiry(academyId, enquiryId, currentUser);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<Void>builder().status(HttpStatus.OK.value())
					.message("Enquiry deleted successfully").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<Void>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	/**
	 * Add new players to an existing team. This end point allows team members to
	 * add more players to their team
	 *
	 * @param academyId Academy ID
	 * @param teamId    Team ID
	 * @param players   Request containing list of players to add
	 * @return Updated team with new players added
	 */
	@PutMapping("{academyId}/{teamId}/players")
	public ResponseEntity<Response<TeamDto>> addPlayersToTeam(@PathVariable String academyId,
			@PathVariable String teamId,
			@RequestParam(name = "sports", defaultValue = "BADMINTON", required = false) Sports sport,
			@RequestBody List<TeamPlayerDto> players) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			TeamDto updatedTeam = teamsService.addPlayersToTeam(currentUser.getUserId(), academyId, teamId, players,
					sport);

			return ResponseEntity.status(HttpStatus.OK).body(Response.<TeamDto>builder().status(HttpStatus.OK.value())
					.message("success").body(updatedTeam).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<TeamDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}
}
