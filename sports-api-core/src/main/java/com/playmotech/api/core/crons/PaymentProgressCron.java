package com.playmotech.api.core.crons;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.context.annotation.Profile;

import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.PaymentReminderType;
import com.playmotech.api.core.dto.PaymentReminderConfigDto;
import com.playmotech.api.core.dto.PaymentReminderDto;
import com.playmotech.api.core.repo.DuePaymentsViewRepository;
import com.playmotech.api.core.services.IPaymentReminderConfigService;
import com.playmotech.api.core.services.IPaymentService;
import com.playmotech.api.core.utils.CurrencyUtils;
import com.playmotech.api.core.views.DuePaymentsView;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile(value = { "dev", "prod" })
public class PaymentProgressCron {

	private final IPaymentService paymentService;
	private final IPaymentReminderConfigService paymentReminderConfigService;
	private final DuePaymentsViewRepository duePaymentsViewRepository;

	// Default threshold in case no configuration exists
	private static final int DEFAULT_UPCOMING_PAYMENT_DAYS_THRESHOLD = 7;

	public PaymentProgressCron(IPaymentService paymentService,
			IPaymentReminderConfigService paymentReminderConfigService,
			DuePaymentsViewRepository duePaymentsViewRepository) {
		this.paymentService = paymentService;
		this.paymentReminderConfigService = paymentReminderConfigService;
		this.duePaymentsViewRepository = duePaymentsViewRepository;
		log.info("PaymentProgressCron initialized with default upcoming payment threshold: {} days",
				DEFAULT_UPCOMING_PAYMENT_DAYS_THRESHOLD);
	}

	/**
	 * Scheduled job to send payment reminder notifications Runs every minute to
	 * check if any configured reminders should be sent based on academy
	 * configurations
	 */
	@Scheduled(cron = "0 * * * * *") // Check every minute for configurations that need to be processed
	@Transactional(readOnly = true)
	public void sendPaymentReminders() {
		log.info("Starting payment reminder notification job...");
		try {
			// Get all enabled reminder configurations
			List<PaymentReminderConfigDto> enabledConfigs = paymentReminderConfigService.getAllEnabledConfigs();

			if (CollectionUtils.isEmpty(enabledConfigs)) {
				log.info("No active payment reminder configurations found");
				return;
			}

			LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

			for (PaymentReminderConfigDto config : enabledConfigs) {
				// Check if it's time to send this notification based on configured time
				if (shouldSendReminder(config, now)) {
					switch (config.getReminderType()) {
					case UPCOMING:
						processUpcomingPaymentReminders(config);
						break;
					case OVERDUE:
						processOverduePaymentReminders(config);
						break;
					default:
						log.warn("Unknown reminder type: {}", config.getReminderType());
					}
				}
			}
		} catch (Exception e) {
			log.error("Error sending payment reminders", e);
		}
	}

	/**
	 * Determines if a reminder should be sent based on its configured time
	 *
	 * @param config The reminder configuration
	 * @param now    The current time
	 * @return true if the reminder should be sent now
	 */
	private boolean shouldSendReminder(PaymentReminderConfigDto config, LocalDateTime now) {
		LocalTime configTime = config.getNotificationTime();
		LocalTime currentTime = now.toLocalTime();

		// Send if within a 1-minute window of the configured time
		return Math.abs(currentTime.getMinute() - configTime.getMinute()) < 1
				&& currentTime.getHour() == configTime.getHour();
	}

	/**
	 * Process and send reminders for upcoming payments based on configuration
	 */
	private void processUpcomingPaymentReminders(PaymentReminderConfigDto config) {
		try {
			int daysThreshold = config.getFrequencyDays() != null ? config.getFrequencyDays()
					: DEFAULT_UPCOMING_PAYMENT_DAYS_THRESHOLD;

			Set<String> academyIds = config.getAcademyIds();
			String academiesLog = String.join(", ", academyIds);

			log.info("Processing upcoming payment reminders for academies: {} (due within {} days)...", academiesLog,
					daysThreshold);

			// Get upcoming payments due within the threshold days
			List<DuePaymentsView> upcomingPayments = duePaymentsViewRepository
					.findUpcomingPaymentsDueWithinDays(daysThreshold);

			if (CollectionUtils.isEmpty(upcomingPayments)) {
				log.info("No upcoming payments found for reminders");
				return;
			}

			// Filter for academies in this configuration
			List<DuePaymentsView> academyPayments = upcomingPayments.stream()
					.filter(payment -> academyIds.contains(payment.getAcademyId())).collect(Collectors.toList());

			if (CollectionUtils.isEmpty(academyPayments)) {
				log.info("No upcoming payments found for configured academies");
				return;
			}

			log.info("Found {} upcoming payments for reminders in {} academies", academyPayments.size(),
					academyIds.size());

			// Convert to DTOs for sending notifications
			List<PaymentReminderDto> reminderDtos = academyPayments.stream()
					// .filter(each -> each.getUpcomingAmount().intValue() > 0)
					.filter(each -> each.getTotalCourseDue().intValue() > 0)
					.map(payment -> convertToReminderDto(payment, PaymentReminderType.UPCOMING, config))
					.collect(Collectors.toList());

			log.info("Found {} reminderDtos", reminderDtos.size());

			// Send notifications
			paymentService.sendPaymentReminders(reminderDtos);

			log.info("Successfully sent {} upcoming payment reminder notifications for configured academies",
					reminderDtos.size());
		} catch (Exception e) {
			log.error("Error processing upcoming payment reminders for academies: {}",
					String.join(", ", config.getAcademyIds()), e);
		}
	}

	/**
	 * Process and send reminders for overdue payments based on configuration
	 */
	private void processOverduePaymentReminders(PaymentReminderConfigDto config) {
		try {
			int daysThreshold = config.getFrequencyDays() != null ? config.getFrequencyDays() : 1;

			Set<String> academyIds = config.getAcademyIds();
			String academiesLog = String.join(", ", academyIds);

			log.info("Processing overdue payment reminders for academies: {} (overdue by {} days)...", academiesLog,
					daysThreshold);

			// Get overdue payments
			List<DuePaymentsView> overduePayments = duePaymentsViewRepository.findOverduePayments();

			if (CollectionUtils.isEmpty(overduePayments)) {
				log.info("No overdue payments found for reminders");
				return;
			}

			// Filter for academies in this configuration
			List<DuePaymentsView> academyPayments = overduePayments.stream()
					.filter(payment -> academyIds.contains(payment.getAcademyId())).collect(Collectors.toList());

			if (CollectionUtils.isEmpty(academyPayments)) {
				log.info("No overdue payments found for configured academies");
				return;
			}

			log.info("Found {} overdue payments for reminders in {} academies", academyPayments.size(),
					academyIds.size());

			// Convert to DTOs for sending notifications
			List<PaymentReminderDto> reminderDtos = academyPayments.stream()
					.filter(each -> each.getTotalCourseDue().intValue() > 0)
					.map(payment -> convertToReminderDto(payment, PaymentReminderType.OVERDUE, config))
					.collect(Collectors.toList());

			// Send notifications
			paymentService.sendPaymentReminders(reminderDtos);

			log.info("Successfully sent {} overdue payment reminder notifications for configured academies",
					reminderDtos.size());
		} catch (Exception e) {
			log.error("Error processing overdue payment reminders for academies: {}",
					String.join(", ", config.getAcademyIds()), e);
		}
	}

	/**
	 * Convert a payment view to a reminder DTO
	 */
	private PaymentReminderDto convertToReminderDto(DuePaymentsView payment, PaymentReminderType reminderType,
			PaymentReminderConfigDto config) {
		PaymentReminderDto dto = new PaymentReminderDto();

		// Set required fields
		dto.setTraineeUserId(payment.getTraineeUserId());
		dto.setAcademyId(payment.getAcademyId());

		// Format amount with currency - default to INR since currencyCode is not
		// available
		String formattedAmount = CurrencyUtils.formatCurrency(Currency.INR, payment.getTotalCourseDue().longValue());
		dto.setAmount(formattedAmount);

		// Add extra parameters for customization
		Map<String, String> extraParams = new HashMap<>();
		extraParams.put("reminderType", reminderType.name());
		extraParams.put("dueDaysCount", String.valueOf(payment.getCourseDueDaysCount()));

		// Add additional fields from payment
		if (payment.getPlayerName() != null) {
			extraParams.put("playerName", payment.getPlayerName());
		}
		if (payment.getPlayerContactNumber() != null) {
			extraParams.put("playerContactNumber", payment.getPlayerContactNumber());
		}
		if (payment.getPlayerEmailId() != null) {
			extraParams.put("playerEmailId", payment.getPlayerEmailId());
		}
		if (payment.getAcademyName() != null) {
			extraParams.put("academyName", payment.getAcademyName());
		}
		if (payment.getCourseId() != null) {
			extraParams.put("courseId", payment.getCourseId());
		}
		if (payment.getCourseName() != null) {
			extraParams.put("courseName", payment.getCourseName());
		}

		// Add configuration parameters
		extraParams.put("configId", String.valueOf(config.getId()));

		// mobile push templates
		if (Boolean.TRUE.equals(config.getSendPushNotification()) && config.getMobileConfig() != null) {
			extraParams.put("titleTemplate", config.getMobileConfig().getTitleTemplate());
			extraParams.put("messageTemplate", config.getMobileConfig().getMessageTemplate());
		}

		// email templates
		if (Boolean.TRUE.equals(config.getSendEmailNotification()) && config.getEmailConfig() != null) {
			extraParams.put("title", config.getEmailConfig().getSubjectTemplate());
			extraParams.put("message", config.getEmailConfig().getMessageBody());
		}

		dto.setExtraParams(extraParams);

		return dto;
	}
}
