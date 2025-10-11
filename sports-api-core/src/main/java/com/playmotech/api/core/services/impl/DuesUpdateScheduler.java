package com.playmotech.api.core.services.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.repo.TraineeCourseEnrollmentRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.NewPaymentService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DuesUpdateScheduler {

	private final TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo;
	private final NewPaymentService newPaymentService;
	private final TaskExecutor taskExecutor;

	public DuesUpdateScheduler(TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo,
			NewPaymentService newPaymentService, @Qualifier("duesTaskExecutor") TaskExecutor taskExecutor) {
		this.traineeCourseEnrollmentRepo = traineeCourseEnrollmentRepo;
		this.newPaymentService = newPaymentService;
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Runs every hour — adjust as per requirements.
	 */
	@Scheduled(cron = "0 0 0/12 * * *", zone = "Asia/Kolkata")
	// @Scheduled(fixedRate = 3600000, zone = "Asia/Kolkata") // every 1 hour
	public void processDuesUpdates() {
		LocalDateTime startTime = LocalDateTime.now();
		log.info("⏰ Starting dues update process at: {}", startTime);

		try {
			LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata")); // Ensure correct timezone
			log.info("📅 Processing date: {}", today);

			// Fetch enrollments in batches to avoid loading all into memory
			int page = 0;
			int pageSize = 500; // tune as needed
			List<TraineeCourseEnrollment> enrollmentsWithDues;

			do {
				enrollmentsWithDues = traineeCourseEnrollmentRepo.findEnrollmentsWithDuesPaged(page, pageSize);

				if (enrollmentsWithDues.isEmpty()) {
					if (page == 0) {
						log.info("✅ No enrollments with pending dues found.");
					}
					break;
				}

				log.info("📌 Batch {} → Found {} enrollments", page + 1, enrollmentsWithDues.size());

				// Group by course for parallel processing
				Map<String, List<TraineeCourseEnrollment>> enrollmentsByCourse = enrollmentsWithDues.stream()
						.collect(Collectors.groupingBy(e -> e.getCourse().getId()));

				List<CompletableFuture<Void>> futures = new ArrayList<>();

				for (Map.Entry<String, List<TraineeCourseEnrollment>> entry : enrollmentsByCourse.entrySet()) {
					String courseId = entry.getKey();
					List<TraineeCourseEnrollment> courseEnrollments = entry.getValue();

					futures.add(CompletableFuture.runAsync(
							() -> processCourseEnrollments(courseId, courseEnrollments, today), taskExecutor));

				}

				// Wait until all course tasks in this batch finish
				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

				page++;
			} while (enrollmentsWithDues.size() == pageSize);

			log.info("✅ All dues processing completed at: {}", LocalDateTime.now());

		} catch (Exception e) {
			log.error("💥 Error in dues update scheduler: {}", e.getMessage(), e);
		}
	}

	private void processCourseEnrollments(String courseId, List<TraineeCourseEnrollment> enrollments, LocalDate today) {
		log.debug("🔍 Processing {} enrollments for course ID: {}", enrollments.size(), courseId);

		int successCount = 0;
		int errorCount = 0;
		int tooEarlyCount = 0;
		int noMoreInstallmentsCount = 0;

		for (TraineeCourseEnrollment enrollment : enrollments) {
			String enrollmentId = enrollment.getId();
			try {
				ServiceResponse response = newPaymentService.updateDues(enrollmentId, today);

				if (response.getHttpStatus().is2xxSuccessful()) {
					String apiResponse = response.getMessage(); // compare message text

					if (ApiResponse.TOO_EARLY_FOR_NEXT_DUE.message.equals(apiResponse)) {
						tooEarlyCount++;
						log.debug("🕒 Too early for next due - Enrollment ID: {}", enrollmentId);
					} else if (ApiResponse.NO_MORE_INSTALLMENTS.message.equals(apiResponse)) {
						noMoreInstallmentsCount++;
						log.debug("📘 No more installments - Enrollment ID: {}", enrollmentId);
					} else if (ApiResponse.DUES_UPDATED_SUCCESSFULLY.message.equals(apiResponse)) {
						successCount++;
						log.debug("✅ Dues updated - Enrollment ID: {}", enrollmentId);
					} else {
						successCount++;
						log.debug("✅ Dues processed - Enrollment ID: {} | Response: {}", enrollmentId, apiResponse);
					}
				} else {
					errorCount++;
					log.warn("⚠️ Dues NOT updated for Enrollment ID: {} | Message: {}", enrollmentId,
							response.getMessage());
				}
			} catch (Exception e) {
				errorCount++;
				log.error("❌ Exception updating dues for Enrollment ID: {} - {}", enrollmentId, e.getMessage(), e);
			}
		}

		log.info("📦 Course {} ➤ Success: {}, Too Early: {}, No More Installments: {}, Failures: {}", courseId,
				successCount, tooEarlyCount, noMoreInstallmentsCount, errorCount);
	}

	/**
	 * Process dues updates for enrollments based on academy and program
	 * 
	 * @param academyId The academy ID to filter enrollments
	 * @param programId The program ID to filter enrollments (if null, processes all
	 *                  enrollments for the academy)
	 */
	public void processAcademyProgramDues(String academyId, String programId) {
		LocalDateTime startTime = LocalDateTime.now();
		log.info("⏰ Starting academy-program dues update process at: {}", startTime);
		log.info("🏫 Academy ID: {}, 📚 Program ID: {}", academyId, programId != null ? programId : "ALL");

		try {
			LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata")); // Ensure correct timezone
			log.info("📅 Processing date: {}", today);

			// Fetch enrollments in batches to avoid loading all into memory
			int page = 0;
			int pageSize = 500; // tune as needed
			List<TraineeCourseEnrollment> enrollmentsWithDues;
			int totalProcessed = 0;

			do {
				// Fetch enrollments based on academy and program
				if (programId != null) {
					enrollmentsWithDues = traineeCourseEnrollmentRepo
							.findEnrollmentsByAcademyAndProgramWithDuesPaged(academyId, programId, page, pageSize);
					log.debug("🔍 Fetching enrollments for academy {} and program {} - Page {}", academyId, programId,
							page + 1);
				} else {
					enrollmentsWithDues = traineeCourseEnrollmentRepo.findEnrollmentsByAcademyWithDuesPaged(academyId,
							page, pageSize);
					log.debug("🔍 Fetching all enrollments for academy {} - Page {}", academyId, page + 1);
				}

				if (enrollmentsWithDues.isEmpty()) {
					if (page == 0) {
						log.info("✅ No enrollments with pending dues found for academy: {}, program: {}", academyId,
								programId != null ? programId : "ALL");
					}
					break;
				}

				log.info("📌 Batch {} → Found {} enrollments", page + 1, enrollmentsWithDues.size());
				totalProcessed += enrollmentsWithDues.size();

				// Group by course for parallel processing
				Map<String, List<TraineeCourseEnrollment>> enrollmentsByCourse = enrollmentsWithDues.stream()
						.collect(Collectors.groupingBy(e -> e.getCourse().getId()));

				List<CompletableFuture<Void>> futures = new ArrayList<>();

				for (Map.Entry<String, List<TraineeCourseEnrollment>> entry : enrollmentsByCourse.entrySet()) {
					String courseId = entry.getKey();
					List<TraineeCourseEnrollment> courseEnrollments = entry.getValue();

					// Process all courses for this academy/program combination
					futures.add(CompletableFuture.runAsync(() -> processCourseEnrollmentsForAcademyProgram(courseId,
							courseEnrollments, today, academyId, programId), taskExecutor));
				}

				// Wait until all course tasks in this batch finish
				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

				page++;
			} while (enrollmentsWithDues.size() == pageSize);

			log.info("✅ Academy-Program dues processing completed at: {} | Total enrollments processed: {}",
					LocalDateTime.now(), totalProcessed);

		} catch (Exception e) {
			log.error("💥 Error in academy-program dues update: Academy: {}, Program: {}, Error: {}", academyId,
					programId, e.getMessage(), e);
		}
	}

	private void processCourseEnrollmentsForAcademyProgram(String courseId, List<TraineeCourseEnrollment> enrollments,
			LocalDate today, String academyId, String programId) {

		log.debug("🔍 Processing {} enrollments for course ID: {} (Academy: {}, Program: {})", enrollments.size(),
				courseId, academyId, programId != null ? programId : "ALL");

		int successCount = 0;
		int errorCount = 0;
		int tooEarlyCount = 0;
		int noMoreInstallmentsCount = 0;

		for (TraineeCourseEnrollment enrollment : enrollments) {
			String enrollmentId = enrollment.getId();
			try {
				ServiceResponse response = newPaymentService.updateDues(enrollmentId, today);

				if (response.getHttpStatus().is2xxSuccessful()) {
					String apiResponse = response.getMessage(); // compare message text

					if (ApiResponse.TOO_EARLY_FOR_NEXT_DUE.message.equals(apiResponse)) {
						tooEarlyCount++;
						log.debug("🕒 Too early for next due - Enrollment ID: {}", enrollmentId);
					} else if (ApiResponse.NO_MORE_INSTALLMENTS.message.equals(apiResponse)) {
						noMoreInstallmentsCount++;
						log.debug("📘 No more installments - Enrollment ID: {}", enrollmentId);
					} else if (ApiResponse.DUES_UPDATED_SUCCESSFULLY.message.equals(apiResponse)) {
						successCount++;
						log.debug("✅ Dues updated - Enrollment ID: {}", enrollmentId);
					} else {
						successCount++;
						log.debug("✅ Dues processed - Enrollment ID: {} | Response: {}", enrollmentId, apiResponse);
					}
				} else {
					errorCount++;
					log.warn("⚠️ Dues NOT updated for Enrollment ID: {} | Message: {}", enrollmentId,
							response.getMessage());
				}
			} catch (Exception e) {
				errorCount++;
				log.error("❌ Exception updating dues for Enrollment ID: {} - {}", enrollmentId, e.getMessage(), e);
			}
		}

		log.info(
				"📦 Academy: {} | Program: {} | Course: {} ➤ Success: {}, Too Early: {}, No More Installments: {}, Failures: {}",
				academyId, programId != null ? programId : "ALL", courseId, successCount, tooEarlyCount,
				noMoreInstallmentsCount, errorCount);
	}

	public void processSpecificAcademyProgramDues(String academyId, String programId) {
		// Example: Process specific academy-program combinations
		// You can configure these IDs as needed or read from configuration

		processAcademyProgramDues(academyId, programId);

		// You can add multiple academy-program combinations here:
		// processAcademyProgramDues("academy-1", null); // All programs in academy-1
		// processAcademyProgramDues("academy-2", "program-x"); // Specific program in
		// academy-2
	}

}
