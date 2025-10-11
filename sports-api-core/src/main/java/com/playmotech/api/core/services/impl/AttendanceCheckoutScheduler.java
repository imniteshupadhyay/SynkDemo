package com.playmotech.api.core.services.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.playmotech.api.core.dao_postgres.GeoFenceCoachAttendance;
import com.playmotech.api.core.dto.AutoCheckOutRequestDto;
import com.playmotech.api.core.repo.GeoFenceCoachAttendanceRepository;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.GeoFenceAttendanceService;

import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler that runs at 12:30 AM every day to automatically check out any
 * active check-ins
 */
@Slf4j
@Component
public class AttendanceCheckoutScheduler {

	private final GeoFenceCoachAttendanceRepository attendanceRepository;
	private final GeoFenceAttendanceService geoFenceAttendanceService;

	private final TaskExecutor taskExecutor;

	public AttendanceCheckoutScheduler(@Qualifier("midnightCheckoutScheduler") TaskExecutor taskExecutor,
			GeoFenceAttendanceService geoFenceAttendanceService,
			GeoFenceCoachAttendanceRepository attendanceRepository) {
		this.attendanceRepository = attendanceRepository;
		this.geoFenceAttendanceService = geoFenceAttendanceService;
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Runs every day at 02:00 AM IST to check out any active check-ins
	 */
	@Scheduled(cron = "0 0 2 * * *", zone = "Asia/Kolkata")
	public void processMidnightCheckouts() {
		LocalDateTime startTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
		log.info("⏰ Starting midnight check-out process at: {}", startTime);

		try {
			// Find all active check-ins
			List<GeoFenceCoachAttendance> activeAttendances = attendanceRepository.findByIsActiveTrueAndDeletedFalse();

			if (activeAttendances.isEmpty()) {
				log.info("No active check-ins found for midnight check-out");
				return;
			}

			log.info("Found {} active check-ins to process for midnight check-out", activeAttendances.size());

			// Process each active check-in asynchronously
			CompletableFuture<?>[] futures = activeAttendances.stream()
					.map(attendance -> CompletableFuture.runAsync(() -> {
						try {
							processSingleCheckout(attendance);
						} catch (Exception e) {
							log.error("Error processing check-out for attendance ID: {}", attendance.getId(), e);
						}
					}, taskExecutor)).toArray(CompletableFuture[]::new);

			// Wait for all check-outs to complete
			CompletableFuture.allOf(futures).join();

			log.info("✅ Completed midnight check-out process. Total check-outs: {}", activeAttendances.size());
		} catch (Exception e) {
			log.error("❌ Error in midnight check-out process: {}", e.getMessage(), e);
		}
	}

	private void processSingleCheckout(GeoFenceCoachAttendance attendance) {
		try {
			log.debug("Processing check-out for attendance ID: {}, Coach: {}", attendance.getId(),
					attendance.getCoach().getId());

			// Create auto-checkout request
			AutoCheckOutRequestDto requestDto = new AutoCheckOutRequestDto();
			requestDto.setLatitude(attendance.getCheckInLatitude());
			requestDto.setLongitude(attendance.getCheckInLongitude());

			// Call the auto-checkout service
			ServiceResponse response = geoFenceAttendanceService.autoCheckOut(attendance.getCoach().getId(),
					requestDto);

			if (!response.getHttpStatus().is2xxSuccessful()) {
				log.warn("Failed to auto-checkout attendance ID: {}. Response: {}", attendance.getId(), response);
			}
		} catch (Exception e) {
			log.error("Error processing check-out for attendance ID: {}", attendance.getId(), e);
		}
	}
}
