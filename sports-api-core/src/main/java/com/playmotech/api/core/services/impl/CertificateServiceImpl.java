package com.playmotech.api.core.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Certificate;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.TraineeAcademyMapping;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CertificateRequest;
import com.playmotech.api.core.dto.UserCertificateDto;
import com.playmotech.api.core.repo.CertificateRepository;
import com.playmotech.api.core.repo.TraineeAcademyMappingRepo;
import com.playmotech.api.core.repo.TraineeCourseEnrollmentRepo;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.CertificateDao;
import com.playmotech.api.core.response.dao.UserProfileDao;
import com.playmotech.api.core.services.CertificateService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.utils.GenericFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

	private final TraineeAcademyMappingRepo traineeAcademyMappingRepo;
	private final TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo;
	private final UserProfileRepo userProfileRepo;
	private final CertificateRepository certificateRepository;
	public final IStorageService storageService;

	@Value("${certificate.s3.bucket}")
	public String certificateBucket;

	@Value("${certificate.base.url}")
	public String certificateBaseUrl;

	/**
	 * Extracts all files from ZIP archive, handling nested ZIPs and PDFs
	 */
	private Map<String, byte[]> extractAllFilesFromZip(MultipartFile zipFile) throws IOException {
		Map<String, byte[]> allFiles = new HashMap<>();

		try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
			ZipEntry entry;

			while ((entry = zipInputStream.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					String fileName = entry.getName().toLowerCase();

					// Read file content
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024];
					int length;

					while ((length = zipInputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, length);
					}

					byte[] fileContent = outputStream.toByteArray();

					if (fileName.endsWith(".pdf")) {
						allFiles.put(entry.getName(), fileContent);
						log.info("Extracted PDF: {} (size: {} bytes)", entry.getName(), fileContent.length);
					} else if (fileName.endsWith(".zip")) {
						// Handle nested ZIP file
						log.info("Found nested ZIP file: {}, extracting contents...", entry.getName());
						Map<String, byte[]> nestedFiles = extractPdfsFromNestedZip(fileContent);
						allFiles.putAll(nestedFiles);
					}
				}
				zipInputStream.closeEntry();
			}
		}

		log.info("Extracted {} total files from ZIP (including nested)", allFiles.size());
		return allFiles;
	}

	/**
	 * Extracts PDFs from nested ZIP file
	 */
	private Map<String, byte[]> extractPdfsFromNestedZip(byte[] zipData) throws IOException {
		Map<String, byte[]> pdfFiles = new HashMap<>();

		try (ZipInputStream nestedZipInputStream = new ZipInputStream(new java.io.ByteArrayInputStream(zipData))) {
			ZipEntry entry;

			while ((entry = nestedZipInputStream.getNextEntry()) != null) {
				if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".pdf")) {
					// Read PDF content
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024];
					int length;

					while ((length = nestedZipInputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, length);
					}

					byte[] pdfContent = outputStream.toByteArray();
					pdfFiles.put(entry.getName(), pdfContent);
					log.info("Extracted nested PDF: {} (size: {} bytes)", entry.getName(), pdfContent.length);
				}
				nestedZipInputStream.closeEntry();
			}
		}

		return pdfFiles;
	}

	/**
	 * Finds matching PDF for user based on userId (UUID) using the naming
	 * convention Expected format: Certificate_userId_date.pdf
	 */
	private byte[] findPdfForUser(Map<String, byte[]> pdfFiles, String userId) {
		String userIdLower = userId.toLowerCase();

		// Primary search: Look for exact naming convention Certificate_userId_date.pdf
		for (Map.Entry<String, byte[]> entry : pdfFiles.entrySet()) {
			String fileName = entry.getKey().toLowerCase();
			String fileNameOnly = fileName;

			// Remove path if present
			if (fileName.contains("/")) {
				fileNameOnly = fileName.substring(fileName.lastIndexOf("/") + 1);
			}
			if (fileName.contains("\\")) {
				fileNameOnly = fileName.substring(fileName.lastIndexOf("\\") + 1);
			}

			// Check for exact naming convention: Certificate_userId_date.pdf
			if (fileNameOnly.matches("certificate_" + userIdLower + "_\\d{8}\\.pdf")) {
				log.info("Found exact match for userId {}: {}", userId, entry.getKey());
				return entry.getValue();
			}
		}

		// Secondary search: Look for userId anywhere in filename
		for (Map.Entry<String, byte[]> entry : pdfFiles.entrySet()) {
			String fileName = entry.getKey().toLowerCase();

			if (fileName.contains(userIdLower)) {
				log.info("Found partial match for userId {}: {}", userId, entry.getKey());
				return entry.getValue();
			}
		}

		// Tertiary search: Look for various naming patterns
		for (Map.Entry<String, byte[]> entry : pdfFiles.entrySet()) {
			String fileName = entry.getKey().toLowerCase();

			if (fileName.contains("user_" + userIdLower) || fileName.contains("userid_" + userIdLower)
					|| fileName.contains("cert_" + userIdLower) || fileName.matches(".*\\b" + userIdLower + "\\b.*")) {

				log.info("Found pattern match for userId {}: {}", userId, entry.getKey());
				return entry.getValue();
			}
		}

		log.warn("No matching PDF found for userId: {}", userId);
		return null;
	}

	@Override
	@Transactional
	public ServiceResponse storeCertificates(CertificateRequest request, MultipartFile file) {
		List<Certificate> savedCertificates = new ArrayList<>();
		List<String> processingErrors = new ArrayList<>();

		try {
			log.info("Processing certificate request for {} users with ZIP file", request.getUsers().size());

			// Validate that the uploaded file is a ZIP
			if (!isZipFile(file)) {
				return ResponseBuilder.badRequest("Uploaded file must be a ZIP archive containing PDF certificates");
			}

			// Extract all files from ZIP (including nested ZIPs)
			Map<String, byte[]> extractedFiles = extractAllFilesFromZip(file);
			if (extractedFiles.isEmpty()) {
				return ResponseBuilder.badRequest("No PDF files found in the ZIP archive or nested ZIP files");
			}

			log.info("Found {} PDF files in total", extractedFiles.size());

			// Process each user
			for (UserCertificateDto user : request.getUsers()) {
				try {
					log.info("Processing certificate for user: {} (ID: {})", user.getFullName(), user.getUserId());

					// Validate user data
					if (user.getUserId() == null) {
						String error = String.format("User ID is null for user: %s", user.getFullName());
						log.error(error);
						processingErrors.add(error);
						continue;
					}

					// Find matching PDF for this user
					byte[] userPdfData = findPdfForUser(extractedFiles, user.getUserId().toString());
					if (userPdfData == null || userPdfData.length == 0) {
						String error = String.format("No PDF found for user: %s (ID: %s)", user.getFullName(),
								user.getUserId());
						log.error(error);
						processingErrors.add(error);
						continue;
					}

					// Generate certificate filename with proper naming convention
					String certificateFileName = generateCertificateFileName(request.getCertificateType().toString(),
							request.getProgramId() != null ? request.getProgramId().toString()
									: request.getEventName());

					log.info("Generated certificate filename: {}", certificateFileName);

					// Upload to S3
					String s3Url = uploadCertificateToS3(userPdfData, certificateFileName, user.getUserId().toString());

					if (s3Url == null) {
						String error = String.format("Failed to upload certificate for user: %s (ID: %s)",
								user.getFullName(), user.getUserId());
						log.error(error);
						processingErrors.add(error);
						continue;
					}

					// Check if a certificate with the same userId, programId, and certificateType
					// already exists
					List<Certificate> existingCertificates = certificateRepository
							.findByUser_Id(user.getUserId().toString());
					Certificate existingCertificate = null;

					if (existingCertificates != null && !existingCertificates.isEmpty()) {
						// Find certificate with matching programId and certificateType
						for (Certificate cert : existingCertificates) {
							if (cert.getCourse() != null && request.getProgramId() != null
									&& cert.getCourse().getId().equals(request.getProgramId())
									&& cert.getCertificateType().equals(request.getCertificateType())) {
								existingCertificate = cert;
								break;
							}
						}
					}

					if (existingCertificate != null) {
						// Update existing certificate
						log.info("Updating existing certificate for user ID: {}, program ID: {}, certificate type: {}",
								user.getUserId(), request.getProgramId(), request.getCertificateType());

						// Update fields
						existingCertificate.setTitle(request.getTitle());
						existingCertificate.setDescription(request.getDescription());
						existingCertificate.setRank(request.getRank());
						existingCertificate.setCertificateUrl(s3Url);
						existingCertificate.setEventName(request.getEventName());
						existingCertificate.setFullName(user.getFullName());

						// Save the updated certificate
						Certificate savedCertificate = certificateRepository.save(existingCertificate);
						savedCertificates.add(savedCertificate);
						log.info("Certificate updated successfully for user: {} (ID: {})", user.getFullName(),
								user.getUserId());

					} else {
						// Create new certificate
						Certificate certificate = buildCertificate(request, user, s3Url);
						certificateRepository.save(certificate);
						savedCertificates.add(certificate);
						log.info("Certificate created successfully for user: {} (ID: {})", user.getFullName(),
								user.getUserId());
					}

				} catch (Exception e) {
					String error = String.format("Error processing certificate for user %s (ID: %s): %s",
							user.getFullName(), user.getUserId(), e.getMessage());
					log.error(error, e);
					processingErrors.add(error);
				}
			}

			// Prepare response
			if (savedCertificates.isEmpty()) {
				String errorMessage = "No certificates could be processed. Errors: "
						+ String.join("; ", processingErrors);
				return ResponseBuilder.badRequest(errorMessage);
			}

			log.info("Certificate processing completed. Success: {}, Errors: {}", savedCertificates.size(),
					processingErrors.size());

			return ResponseBuilder.success(ApiResponse.CERTIFICATE_SAVED_SUCCESS, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Fatal error storing certificates: {}", e.getMessage(), e);
			return ResponseBuilder.internalServerError("Error storing certificates: " + e.getMessage());
		}
	}

	private String generateCertificateFileName(String certificateType, String programIdOrEvent) {
		String timestamp = String.valueOf(System.currentTimeMillis());
		String programOrEvent = (programIdOrEvent != null && !programIdOrEvent.isEmpty()) ? programIdOrEvent : "NA";

		String rawFileName = String.format("certificate_%s_%s_%s.pdf", certificateType, timestamp, programOrEvent);

		// Clean the generated file name using your utility method
		return cleanFileName(rawFileName);
	}

	/**
	 * Utility method to clean file names - removes special characters, spaces, etc.
	 */
	public String cleanFileName(String fileName) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return "unnamed_file";
		}

		// Get file extension
		String extension = "";
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
			extension = fileName.substring(lastDotIndex);
			fileName = fileName.substring(0, lastDotIndex);
		}

		// Clean the filename
		String cleanedName = fileName.toLowerCase().replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("_{2,}", "_")
				.replaceAll("^_+|_+$", "");

		if (cleanedName.isEmpty()) {
			cleanedName = "file";
		}

		if (cleanedName.length() > 200) {
			cleanedName = cleanedName.substring(0, 200);
		}

		return cleanedName + extension;
	}

	/**
	 * Uploads certificate PDF to S3 with proper user ID–based folder structure
	 */
	private String uploadCertificateToS3(byte[] pdfData, String fileName, String userId) {
		try {
			log.info("Uploading certificate to S3: {} for user ID: {}", fileName, userId);

			if (pdfData == null || pdfData.length == 0) {
				log.error("PDF data is null or empty for file: {}", fileName);
				return null;
			}

			String folder = "certificates/";
			String subFolder = String.format("%s/", userId); // Include user ID as subfolder

			String filePath = folder + subFolder + fileName;
			String cdnStoragePath = folder + subFolder + fileName;

			storageService.upload(certificateBucket, filePath, pdfData, "application/pdf");

			String fileUrl = certificateBaseUrl + cdnStoragePath;
			log.info("Certificate uploaded successfully. URL: {}", fileUrl);

			return fileUrl;

		} catch (Exception e) {
			log.error("Failed to upload certificate: {} for user ID: {}", fileName, userId, e);
			return null;
		}
	}

	/**
	 * Validates if the uploaded file is a ZIP file
	 */
	private boolean isZipFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return false;
		}

		String contentType = file.getContentType();
		String fileName = file.getOriginalFilename();

		return (contentType != null
				&& (contentType.equals("application/zip") || contentType.equals("application/x-zip-compressed")))
				|| (fileName != null && fileName.toLowerCase().endsWith(".zip"));
	}

	/**
	 * Builds Certificate entity from request data
	 */
	private Certificate buildCertificate(CertificateRequest request, UserCertificateDto user, String s3Url) {
		Certificate certificate = new Certificate();
		certificate.setTitle(request.getTitle());
		certificate.setDescription(request.getDescription());
		certificate.setCertificateType(request.getCertificateType());
		certificate.setRank(request.getRank());
		certificate.setCertificateUrl(s3Url);
		certificate.setInactive(false);
		certificate.setEventName(request.getEventName());
		certificate.setFullName(user.getFullName());

		// Set foreign keys
		if (request.getAcademyId() != null) {
			Academy academy = new Academy();
			academy.setId(request.getAcademyId());
			certificate.setAcademy(academy);
		}

		if (request.getProgramId() != null) {
			Course course = new Course();
			course.setId(request.getProgramId());
			certificate.setCourse(course);
		}

		if (request.getCreatedById() != null) {
			UserProfile createdBy = new UserProfile();
			createdBy.setId(request.getCreatedById());
			certificate.setCreatedBy(createdBy);
		}

		if (user.getUserId() != null) {
			UserProfile userProfile = new UserProfile();
			userProfile.setId(user.getUserId());
			certificate.setUser(userProfile);
		}

		return certificate;
	}

	@Override
	public ServiceResponse getPlayers(GenericFilter filter) {
		try {
			log.info("Fetching players with filter: {}", filter);

			Set<String> playerIds = new HashSet<>();

			// 1️⃣ Collect all academy IDs
			Set<String> academyIds = new HashSet<>();
			if (StringUtils.hasText(filter.getAcademyId())) {
				academyIds.add(filter.getAcademyId());
			}
			if (filter.getAcademyIds() != null && !filter.getAcademyIds().isEmpty()) {
				academyIds.addAll(filter.getAcademyIds());
			}

			// 2️⃣ Get player IDs linked to academies
			if (!academyIds.isEmpty()) {
				List<TraineeAcademyMapping> mappings = traineeAcademyMappingRepo.findByAcademyIdIn(academyIds);
				mappings.forEach(mapping -> playerIds.add(mapping.getTraineeUserProfile().getId()));
			}

			// 3️⃣ If program IDs are given, filter players enrolled in ALL programs
			if (filter.getProgramIds() != null && !filter.getProgramIds().isEmpty()) {
				List<String> programIds = filter.getProgramIds();

				List<TraineeCourseEnrollment> enrollments = traineeCourseEnrollmentRepo.findByCourse_IdIn(programIds);

				// Group by player ID -> set of program IDs
				Map<String, Set<String>> playerProgramMap = new HashMap<>();
				for (TraineeCourseEnrollment enrollment : enrollments) {
					String playerId = enrollment.getTraineeUserProfile().getId();
					playerProgramMap.computeIfAbsent(playerId, k -> new HashSet<>())
							.add(enrollment.getCourse().getId());
				}

				// Keep only players enrolled in ALL programs
				Set<String> enrolledInAllPrograms = playerProgramMap.entrySet().stream()
						.filter(e -> e.getValue().containsAll(programIds)).map(Map.Entry::getKey)
						.collect(Collectors.toSet());

				if (!playerIds.isEmpty()) {
					// If academy filter already applied, intersect
					playerIds.retainAll(enrolledInAllPrograms);
				} else {
					playerIds.addAll(enrolledInAllPrograms);
				}
			}

			// 4️⃣ Final player list
			List<UserProfile> players;

			if (academyIds.isEmpty() && (filter.getProgramIds() == null || filter.getProgramIds().isEmpty())) {
				// Free players: not linked to any academy
				players = userProfileRepo.findPlayersNotLinkedToAnyAcademy();
			} else {
				// Players matching academy/program filters
				players = playerIds.isEmpty() ? List.of() : userProfileRepo.findByIdIn(List.copyOf(playerIds));
			}

			// 5️⃣ Map to DAO
			List<UserProfileDao> profileDaos = players.stream().map(player -> {
				UserProfileDao dao = new UserProfileDao();
				BeanUtils.copyProperties(player, dao);
				return dao;
			}).toList();

			log.info("Players fetched successfully: {}", profileDaos.size());
			return ResponseBuilder.success(profileDaos, ApiResponse.LIST_FETCHED_SUCCESSFULLY);

		} catch (Exception e) {
			log.error("Error retrieving players: {}", e.getMessage(), e);
			return ResponseBuilder.internalServerError("Error retrieving players: " + e.getMessage());
		}
	}

	@Override
	public ServiceResponse getCertificates(GenericFilter filter) {
		try {
			List<Certificate> certificates = certificateRepository.findByUser_Id(filter.getUserId());

			if (certificates == null || certificates.isEmpty()) {
				return ResponseBuilder.success(Collections.emptyList(), ApiResponse.NO_RECORD_FOUND, HttpStatus.OK);
			}

			List<CertificateDao> certificateDaos = certificates.stream().map(this::mapToCertificateDto)
					.collect(Collectors.toList());

			return ResponseBuilder.success(certificateDaos, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Error retrieving certificates: {}", e.getMessage(), e);
			return ResponseBuilder.internalServerError("Error retrieving certificates: " + e.getMessage());
		}
	}

	/**
	 * Maps Certificate entity to CertificateDao
	 */
	private CertificateDao mapToCertificateDto(Certificate certificate) {
		CertificateDao dao = new CertificateDao();
		BeanUtils.copyProperties(certificate, dao);

		// Map related entities
		if (certificate.getAcademy() != null) {
			dao.setAcademyId(certificate.getAcademy().getId());
			dao.setAcademyName(certificate.getAcademy().getName());
		}

		if (certificate.getCourse() != null) {
			dao.setProgramId(certificate.getCourse().getId());
			dao.setProgramName(certificate.getCourse().getTitle());
		}

		if (certificate.getUser() != null) {
			dao.setUserId(certificate.getUser().getId());
		}

		if (certificate.getCreatedBy() != null) {
			dao.setCreatedById(certificate.getCreatedBy().getId());
			dao.setCreatedByName(certificate.getCreatedBy().getDisplayName());
		}

		return dao;
	}
}