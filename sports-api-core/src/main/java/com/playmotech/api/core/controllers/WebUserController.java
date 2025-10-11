package com.playmotech.api.core.controllers;

import static com.playmotech.api.core.response.ApiResponse.INVALID_REQUEST;
import static com.playmotech.api.core.response.ResponseBuilder.badRequestEntity;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.FileObjectDetails;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileAddEditDto;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.UserService;
import com.playmotech.api.core.utils.GenericFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequestMapping("web/users")
@RequiredArgsConstructor
public class WebUserController {

	private final UserService userService;
	private final UserProfileRepo userProfileRepository;

	@GetMapping
	public ResponseEntity<ServiceResponse> getListWithFilters(HttpServletRequest request,
			@RequestParam(defaultValue = "", required = false) String orderBy,
			@RequestParam(defaultValue = "", required = false) String search,
			@RequestParam(defaultValue = "1", required = false) Short currentPage,
			@RequestParam(defaultValue = "10", required = false) Short pageSize,
			@RequestParam(defaultValue = "true", required = false) boolean ascending,
			@RequestParam(defaultValue = "false", required = false) boolean pageable) {
		// Validate pagination parameters
		if (pageable && (currentPage == null || pageSize == null || currentPage <= 0 || pageSize <= 0)) {
			return badRequestEntity(INVALID_REQUEST);
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		String domainUrl = request.getHeader("origin");
		GenericFilter filter = GenericFilter.builder().isPageable(pageable).userId(currentUserProfile.getId())
				.currentPage(currentPage).pageSize(pageSize).search(search).ascending(ascending).orderBy(orderBy)
				.build();
		ServiceResponse response = userService.getUsersList(filter, currentUserProfile.getId(), domainUrl);

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("{id}")
	public ResponseEntity<ServiceResponse> getUserById(@PathVariable String id) {
		ServiceResponse response = userService.getUserProfileById(id);

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("role")
	public ResponseEntity<ServiceResponse> getUserBasedRole(HttpServletRequest request) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		String domainUrl = request.getHeader("origin");

		ServiceResponse response = userService.getUserBasedRoleActions(currentUserProfile.getId(), domainUrl);

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping("document")
	ResponseEntity<ServiceResponse> uploadDocument(@RequestParam MultipartFile file,
			@RequestParam String documentType) {

		FileObjectDetails obj = new FileObjectDetails();
		obj.setDocumentType(documentType);
		obj.setFile(file);
		ServiceResponse response = userService.fileUpload(obj);

		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping
	public ResponseEntity<ServiceResponse> addUser(@Valid @RequestBody UserProfileAddEditDto userProfileDto) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		ServiceResponse response = userService.addUser(userProfileDto, currentUserProfile.getId());

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping
	public ResponseEntity<ServiceResponse> editUser(@RequestBody UserProfileAddEditDto userProfileDto) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		ServiceResponse response = userService.editUser(userProfileDto, currentUserProfile.getId());

		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@DeleteMapping("{id}")
	public ResponseEntity<ServiceResponse> deleteUser(@PathVariable String id) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentSessionUser = (UserDetail) authentication.getPrincipal();
		UserProfile currentUserProfile = userProfileRepository.findById(currentSessionUser.getUserId()).get();

		ServiceResponse response = userService.deleteUser(id, currentUserProfile.getId());

		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

}
