package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.internal.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.PaymentStatus;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.OrganisationConfig;
import com.playmotech.api.core.dao_postgres.Payment;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.CoursePaymentDetailsDto;
import com.playmotech.api.core.dto.InitPaymentDto;
import com.playmotech.api.core.dto.PaymentDetailsDto;
import com.playmotech.api.core.dto.PaymentDto;
import com.playmotech.api.core.dto.PaymentDueDto;
import com.playmotech.api.core.dto.PaymentReminderDto;
import com.playmotech.api.core.dto.ReceiptDto;
import com.playmotech.api.core.dto.ScheduleDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.UpdatePaymentDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.EmailSendException;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.OrgConfigRepo;
import com.playmotech.api.core.repo.PaymentRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.IMailService;
import com.playmotech.api.core.services.IPaymentService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.utils.ChartUtils;
import com.playmotech.api.core.utils.CurrencyUtils;
import com.playmotech.api.core.utils.DateTimeUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaymentService implements IPaymentService {

	private static final String PAYMENT_SUCCESS_TITLE = "Payment Successful";
	private static final String PAYMENT_FAILED_TITLE = "Payment Failed";
	private static final String PAYMENT_SUCCESS_MESSAGE = "Payment of %s is successful.";
	private static final String PAYMENT_FAILED_MESSAGE = "Payment of %s failed.";
	private static final String TRAINEE_PAYMENT_SCREEN = "TRAINEE_PAYMENTS";
	private static final String COACH_PAYMENT_SCREEN = "TRAINEE_PAYMENT_DETAILS";
	private static final String PAYMENT_REMINDER_TEMPLATE_ID_KEY = "paymentReminderEmailTemplateId";

	private final ModelMapper modelMapper = new ModelMapper();
	private final ICourseService courseService;
	private final IAcademyService academyService;
	private final IUserProfileService userProfileService;
	private final PaymentRepo paymentRepo;
	private final IPushNotificationService pushNotificationService;
	private final PdfService pdfService;
	private final IStorageService storageService;
	private final IMailService mailService;
	private final OrgConfigRepo orgConfigRepo;
	private final AcademyRepo academyRepo;

	@Value("${payments-base-url}")
	private String paymentsBaseUrl;

	@Value("${storage.payments-bucket}")
	private String paymentsBucket;

	@Value("${sendgrid.template.payment-reminder}")
	private String paymentReminderTemplateId;

	public PaymentService(ICourseService courseService, IAcademyService academyService,
			IUserProfileService userProfileService, PaymentRepo paymentRepo,
			IPushNotificationService pushNotificationService, PdfService pdfService, IStorageService storageService,
			IMailService mailService, OrgConfigRepo orgConfigRepo, AcademyRepo academyRepo) {
		this.academyService = academyService;
		this.userProfileService = userProfileService;
		this.paymentRepo = paymentRepo;
		this.pushNotificationService = pushNotificationService;
		this.pdfService = pdfService;
		this.storageService = storageService;
		this.mailService = mailService;
		this.orgConfigRepo = orgConfigRepo;
		this.courseService = courseService;
		this.academyRepo = academyRepo;
	}

	@Override
	public PaymentDto initPayment(String userId, String enrollmentId, InitPaymentDto initPaymentDto)
			throws ResourceException {
		courseService.enrollmentExists(enrollmentId);
		PaymentDetailsDto paymentDetailsDto = getPaymentDetails(enrollmentId,
				initPaymentDto.getPaymentCategory() == null ? PaymentCategory.COURSE_FEE
						: initPaymentDto.getPaymentCategory());
		if (!paymentDetailsDto.getNextDueAt().equalsIgnoreCase(initPaymentDto.getPaymentInstallmentDate())) {
			throw new ResourceException(ErrorCodes.INVALID_REQUEST,
					"Invalid Payment Installment date " + initPaymentDto.getPaymentInstallmentDate());
		}
		Payment payment = modelMapper.map(initPaymentDto, Payment.class);
		payment.setId(UUID.randomUUID().toString());
		payment.setTraineeCourseEnrollment(TraineeCourseEnrollment.builder().id(enrollmentId).build());
		payment.setPaymentStatus(PaymentStatus.PENDING);
		payment.setCreatedAt(Timestamp.from(Instant.now()));
		payment.setPaymentMode(initPaymentDto.getPaymentMode());
		payment.setPaymentInitiatedByUserProfile(UserProfile.builder().id(userId).build());
		if (initPaymentDto.getPaymentCategory() == null) {
			payment.setPaymentCategory(PaymentCategory.COURSE_FEE);
		} else {
			payment.setPaymentCategory(initPaymentDto.getPaymentCategory());
		}
		Payment savedPayment = paymentRepo.save(payment);
		return modelMapper.map(savedPayment, PaymentDto.class);
	}

	@Override
	public PaymentDto updatePayment(String paymentId, String enrollmentId, UpdatePaymentDto updatePaymentDto)
			throws ResourceException {
		Payment payment = paymentRepo.findByIdAndTraineeCourseEnrollment_Id(paymentId, enrollmentId)
				.orElseThrow(() -> new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Payment not found."));

		payment.setUpdatedAt(Timestamp.from(Instant.now()));
		payment.setPaymentMode(updatePaymentDto.getPaymentMode());
		payment.setPaymentStatus(updatePaymentDto.getPaymentStatus());
		payment.setExternalTransactionId(updatePaymentDto.getExternalTransactionId());
		payment.setTransactionTime(updatePaymentDto.getTransactionTime());
		payment.setExtraArgs(updatePaymentDto.getExtraArgs());
		// payment.setPaymentCategory(updatePaymentDto.getPaymentCategory());

		if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
			payment.setReceiptId(ChartUtils.generateReceiptNumber());
			ReceiptDto receipt = new ReceiptDto(
					payment.getTraineeCourseEnrollment().getTraineeUserProfile().getDisplayName(), // Customer Name
																									// (payer)
					"Playmo Technologies Pvt. Ltd.", // My Company Name (payee)
					LocalDateTime.now(), // Transaction DateTime
					"receipts/logo.png", // Customer Logo Path
					Arrays.asList( // Amount Details
							new ReceiptDto.TransactionItem("Fee for period: " + payment.getPaymentInstallmentDate(),
									payment.getAmount())),
					payment.getAmount(), // Total Amount
					payment.getReceiptId());

			try {
				byte[] pdfBytes = pdfService.generateReceiptPdf(receipt);
				String prefix = "receipts/" + enrollmentId + "/" + UUID.randomUUID() + ".pdf";
				storageService.upload(paymentsBucket, prefix, pdfBytes, "application/pdf");
				payment.setReceiptUrl(paymentsBaseUrl + prefix);
			} catch (Exception e) {
				log.error("Error while generating receipt", e);
			}
		}
		Payment savedPayment = paymentRepo.save(payment);

		CompletableFuture.runAsync(() -> {
			try {
				Pair<TraineeCourseEnrollmentDto, CourseDto> traineeCourseEnrollment = courseService
						.getByEnrollmentId(enrollmentId);
				Map<String, String> extraArgs = new HashMap<>();
				extraArgs.put("academyId", traineeCourseEnrollment.getRight().getAcademyId());
				extraArgs.put("traineeUserId", traineeCourseEnrollment.getLeft().getUserProfile().getId());
				extraArgs.put("courseId", traineeCourseEnrollment.getRight().getId());
				String title = savedPayment.getPaymentStatus() == PaymentStatus.SUCCESS ? PAYMENT_SUCCESS_TITLE
						: PAYMENT_FAILED_TITLE;
				String message = String
						.format(savedPayment.getPaymentStatus() == PaymentStatus.SUCCESS ? PAYMENT_SUCCESS_MESSAGE
								: PAYMENT_FAILED_MESSAGE, savedPayment.getAmount());
				log.info("Sending push notification for payment update to User: {}",
						traineeCourseEnrollment.getLeft().getUserProfile().getDisplayName());
				pushNotificationService.sendMessageToPushToken(
						traineeCourseEnrollment.getLeft().getUserProfile().getAndroidFcmPushToken(),
						NotificationType.LIVE_NOTIFICATION, title, message, TRAINEE_PAYMENT_SCREEN, CtaType.SCREEN,
						extraArgs);
				pushNotificationService.addNotification(
						List.of(traineeCourseEnrollment.getLeft().getUserProfile().getId()), message, CtaType.SCREEN,
						TRAINEE_PAYMENT_SCREEN, extraArgs);

				Map<String, UserProfileDto> userprofiles = userProfileService
						.getUserProfileByIds(List.of(traineeCourseEnrollment.getLeft().getUserProfile().getId(),
								traineeCourseEnrollment.getRight().getCoachUserId()))
						.stream().collect(Collectors.toMap(UserProfileDto::getId, userProfileDto -> userProfileDto));

				String coachFcmToken = userprofiles.get(traineeCourseEnrollment.getRight().getCoachUserId())
						.getAndroidFcmPushToken();
				log.info("Sending push notification for payment update to User: {}",
						userprofiles.get(traineeCourseEnrollment.getRight().getCoachUserId()).getDisplayName());
				pushNotificationService.sendMessageToPushToken(coachFcmToken, NotificationType.LIVE_NOTIFICATION,
						"Payment Received",
						"Payment received from " + userprofiles
								.get(traineeCourseEnrollment.getLeft().getUserProfile().getId()).getDisplayName(),
						COACH_PAYMENT_SCREEN, CtaType.SCREEN, extraArgs);
				pushNotificationService.addNotification(
						List.of(userprofiles.get(traineeCourseEnrollment.getRight().getCoachUserId()).getId()),
						"Payment received from " + userprofiles
								.get(traineeCourseEnrollment.getLeft().getUserProfile().getId()).getDisplayName(),
						CtaType.SCREEN, COACH_PAYMENT_SCREEN, extraArgs);

			} catch (ResourceException e) {
				log.error("Failed to send push notification for payment update.", e);
			}
		});

		return modelMapper.map(savedPayment, PaymentDto.class);
	}

	@Override
	public List<CoursePaymentDetailsDto> getCoursePaymentDetails(String academyId, String courseId,
			PaymentCategory category) throws ResourceException {
		academyService.getAcademyById(academyId);
		List<TraineeCourseEnrollment> traineeCourseEnrollment = courseService.getEnrollmentsByCourseId(academyId,
				courseId);
		if (CollectionUtils.isEmpty(traineeCourseEnrollment)) {
			return List.of();
		}
		List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(traineeCourseEnrollment.stream()
				.map(traineeCourseEnrollment1 -> traineeCourseEnrollment1.getTraineeUserProfile().getId()).toList());
		Map<String, UserProfileDto> userProfileDtoMap = userProfileDtos.stream()
				.collect(Collectors.toMap(UserProfileDto::getId, userProfileDto -> userProfileDto));
		List<CoursePaymentDetailsDto> coursePaymentDetailsDtos = new ArrayList<>();

		for (TraineeCourseEnrollment enrollment : traineeCourseEnrollment) {
			CoursePaymentDetailsDto coursePaymentDetailsDto = new CoursePaymentDetailsDto();
			PaymentDetailsDto paymentDetailsDto = getPaymentDetails(enrollment.getId(), category);
			coursePaymentDetailsDto.setEnrollmentId(enrollment.getId());
			coursePaymentDetailsDto.setTraineeUserId(enrollment.getTraineeUserProfile().getId());
			coursePaymentDetailsDto.setPaymentHistory(paymentDetailsDto.getPaymentHistory());
			coursePaymentDetailsDto.setIsInProgress(paymentDetailsDto.getIsInProgress());
			if (userProfileDtoMap.containsKey(enrollment.getTraineeUserProfile().getId())) {
				coursePaymentDetailsDto.setUserProfile(modelMapper.map(
						userProfileDtoMap.get(enrollment.getTraineeUserProfile().getId()), UserProfileMinDto.class));
			}
			if (paymentDetailsDto.getIsDue()) {
				coursePaymentDetailsDto.setDueAmount(paymentDetailsDto.getNextDueAmount());
				coursePaymentDetailsDto.setCurrency(paymentDetailsDto.getCurrency());
				coursePaymentDetailsDto.setIsDuePending(paymentDetailsDto.getIsDue());
			}
			coursePaymentDetailsDtos.add(coursePaymentDetailsDto);
		}

		return sort(coursePaymentDetailsDtos);
	}

	@Override
	public List<Payment> getPaymentsBetween(String academyId, String startDate, String endDate, List<String> courseIds)
			throws ResourceException {
		List<Payment> payments = paymentRepo
				.findByTransactionTimeGreaterThanEqualAndTransactionTimeLessThanEqual(startDate, endDate);
		payments = payments.stream()
				.filter(payment -> LocalDate.parse(startDate).isBefore(LocalDateTime
						.parse(payment.getTransactionTime(), DateTimeUtils.TRANSACTION_TIME_FORMATTER).toLocalDate())
						&& LocalDate.parse(endDate)
								.isAfter(LocalDateTime
										.parse(payment.getTransactionTime(), DateTimeUtils.TRANSACTION_TIME_FORMATTER)
										.toLocalDate()))
				.toList();
		if (CollectionUtils.isEmpty(payments)) {
			return List.of();
		}
		List<String> enrollmentIds = payments.stream().map(payment -> payment.getTraineeCourseEnrollment().getId())
				.toList();
		List<String> enrollmentsFilteredByAcademyId = courseService.getEnrollmentsByAcademyId(academyId, enrollmentIds)
				.stream()
				.filter(traineeCourseEnrollment -> traineeCourseEnrollment.getAcademy().getId()
						.equalsIgnoreCase(academyId)
						&& (CollectionUtils.isEmpty(courseIds)
								|| courseIds.contains(traineeCourseEnrollment.getCourse().getId())))
				.map(TraineeCourseEnrollment::getId).toList();
		return payments.stream().filter(
				payment -> enrollmentsFilteredByAcademyId.contains(payment.getTraineeCourseEnrollment().getId()))
				.toList();
	}

	@Override
	public List<PaymentDetailsDto> getPaymentDetailsByAcademy(String academyId, List<String> courseIds)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		List<TraineeCourseEnrollment> traineeCourseEnrollments = courseService.getEnrollmentsByAcademyId(academyId);
		if (!CollectionUtils.isEmpty(courseIds)) {
			traineeCourseEnrollments = traineeCourseEnrollments.stream()
					.filter(traineeCourseEnrollment -> courseIds.contains(traineeCourseEnrollment.getCourse().getId()))
					.toList();
		}
		if (CollectionUtils.isEmpty(traineeCourseEnrollments)) {
			return List.of();
		}
		List<String> enrollmentIds = traineeCourseEnrollments.stream().map(TraineeCourseEnrollment::getId).toList();
		List<Payment> payments = paymentRepo.findByTraineeCourseEnrollment_IdIn(enrollmentIds);
		Map<String, List<Payment>> paymentMap = payments.stream()
				.collect(Collectors.groupingBy(payment -> payment.getTraineeCourseEnrollment().getId()));
		Map<String, Pair<TraineeCourseEnrollmentDto, CourseDto>> enrollmentCourseDetailsMap = courseService
				.getByEnrollmentIds(enrollmentIds).stream()
				.collect(Collectors.toMap(pair -> pair.getLeft().getId(), pair -> pair));
		List<PaymentDetailsDto> paymentDetailsDtos = new ArrayList<>();
		for (TraineeCourseEnrollment traineeCourseEnrollment : traineeCourseEnrollments) {
			PaymentDetailsDto paymentDetailsDto = new PaymentDetailsDto();
			paymentDetailsDto.setEnrollmentId(traineeCourseEnrollment.getId());
			List<Payment> paymentsByEnrollment = paymentMap.get(traineeCourseEnrollment.getId());
			Pair<TraineeCourseEnrollmentDto, CourseDto> enrollmentCourseDetails = enrollmentCourseDetailsMap
					.get(traineeCourseEnrollment.getId());
			if (enrollmentCourseDetails == null || enrollmentCourseDetails.getLeft() == null
					|| enrollmentCourseDetails.getRight() == null) {
				throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE,
						"Not able to fetch enrollment and course details.");
			}

			TraineeCourseEnrollmentDto traineeCourseEnrollmentDto = enrollmentCourseDetails.getLeft();
			CourseDto courseDto = enrollmentCourseDetails.getRight();

			paymentDetailsDto.setPaymentSchedule(traineeCourseEnrollmentDto.getPaymentSchedule());
			paymentDetailsDto.setCurrency(courseDto.getSchedule().getCurrency());
			setFuturePayments(paymentDetailsDto, paymentsByEnrollment, courseDto, traineeCourseEnrollmentDto,
					paymentDetailsDto.getPaymentCategory());
			paymentDetailsDtos.add(paymentDetailsDto);
		}

		return paymentDetailsDtos;
	}

	@Override
	public List<PaymentDueDto> getPaymentDueDto(String academyId) throws ResourceException {
		academyService.getAcademyById(academyId);
		List<PaymentDueDto> paymentDueDtos;
		List<PaymentDetailsDto> paymentDetailsDtos = getPaymentDetailsByAcademy(academyId, new ArrayList<>());
		if (CollectionUtils.isEmpty(paymentDetailsDtos)) {
			return List.of();
		}

		List<TraineeCourseEnrollmentDto> enrollmentDtos = courseService
				.getByEnrollmentIdsByEID(paymentDetailsDtos.stream().map(PaymentDetailsDto::getEnrollmentId).toList());
		Map<String, TraineeCourseEnrollmentDto> enrollmentDtoMap = enrollmentDtos.stream().collect(Collectors
				.toMap(TraineeCourseEnrollmentDto::getId, traineeCourseEnrollmentDto -> traineeCourseEnrollmentDto));
		Map<String, PaymentDueDto> paymentDueDtoMap = new HashMap<>();
		for (PaymentDetailsDto paymentDetailsDto : paymentDetailsDtos) {
			if (!paymentDetailsDto.getIsDue() || !enrollmentDtoMap.containsKey(paymentDetailsDto.getEnrollmentId())) {
				continue;
			}
			TraineeCourseEnrollmentDto traineeCourseEnrollmentDto = enrollmentDtoMap
					.get(paymentDetailsDto.getEnrollmentId());
			if (!paymentDueDtoMap.containsKey(traineeCourseEnrollmentDto.getUserProfile().getId())) {
				PaymentDueDto paymentDueDto = new PaymentDueDto();
				paymentDueDto.setTraineeUserId(traineeCourseEnrollmentDto.getUserProfile().getId());
				paymentDueDto.setUserProfile(
						modelMapper.map(traineeCourseEnrollmentDto.getUserProfile(), UserProfileMinDto.class));
				paymentDueDto.setPendingAmount("0");
				paymentDueDtoMap.put(traineeCourseEnrollmentDto.getUserProfile().getId(), paymentDueDto);
			}
			paymentDueDtoMap.get(traineeCourseEnrollmentDto.getUserProfile().getId())
					.setPendingAmount(String.valueOf(
							Long.parseLong(paymentDueDtoMap.get(traineeCourseEnrollmentDto.getUserProfile().getId())
									.getPendingAmount()) + paymentDetailsDto.getNextDueAmount()));
		}
		paymentDueDtos = new ArrayList<>(paymentDueDtoMap.values());
		paymentDueDtos = paymentDueDtos.stream()
				.peek(paymentDueDto -> paymentDueDto.setPendingAmount(
						CurrencyUtils.formatCurrency(Currency.INR, Long.parseLong(paymentDueDto.getPendingAmount()))))
				.toList();

		return paymentDueDtos;
	}

	@Async
	@Override
	public void sendPaymentReminders(List<PaymentReminderDto> paymentReminderDtos) throws ResourceException {
		if (paymentReminderDtos == null || paymentReminderDtos.isEmpty()) {
			log.info("No payment reminders to process");
			return;
		}

		// Build user profile map
		Map<String, UserProfileDto> userProfileDtoMap = userProfileService
				.getUserProfileByIds(paymentReminderDtos.stream().map(PaymentReminderDto::getTraineeUserId).toList())
				.stream().collect(Collectors.toMap(UserProfileDto::getId, userProfileDto -> userProfileDto));

		for (PaymentReminderDto paymentReminderDto : paymentReminderDtos) {
			try {
				if (!userProfileDtoMap.containsKey(paymentReminderDto.getTraineeUserId())) {
					log.error("User not found for traineeUserId: {}", paymentReminderDto.getTraineeUserId());
					continue;
				}

				UserProfileDto userProfileDto = userProfileDtoMap.get(paymentReminderDto.getTraineeUserId());
				;

				// Build extra parameters
				Map<String, String> extraArgs = new HashMap<>();
				extraArgs.put("academyId", paymentReminderDto.getAcademyId());
				extraArgs.put("traineeUserId", paymentReminderDto.getTraineeUserId());

				if (paymentReminderDto.getExtraParams() != null) {
					extraArgs.putAll(paymentReminderDto.getExtraParams());
				}

				// Get notification configuration
				String notificationType = extraArgs.getOrDefault("reminderType", "UPCOMING");
				int dueDaysCount = Integer.parseInt(extraArgs.getOrDefault("dueDaysCount", "0"));
				String titleTemplate = extraArgs.get("titleTemplate");
				String messageTemplate = extraArgs.get("messageTemplate");

				// Format days text for messages
				String daysText = formatDaysText(notificationType, dueDaysCount);

				// Build notification title and message
				String title = buildNotificationTitle(titleTemplate, paymentReminderDto.getAmount(), daysText,
						notificationType);
				String message = buildNotificationMessage(messageTemplate, paymentReminderDto.getAmount(), daysText,
						notificationType, dueDaysCount);

				if (org.springframework.util.StringUtils.hasText(userProfileDto.getAndroidFcmPushToken())) {
					log.info("Sending {} notification for payment to User: {}", notificationType,
							userProfileDto.getDisplayName());

					// Send push notification
					pushNotificationService.sendMessageToPushToken(userProfileDto.getAndroidFcmPushToken(),
							NotificationType.LIVE_NOTIFICATION, title, message, TRAINEE_PAYMENT_SCREEN, CtaType.SCREEN,
							extraArgs);

					// Add in-app notification
					pushNotificationService.addNotification(Collections.singletonList(userProfileDto.getId()), message,
							CtaType.SCREEN, TRAINEE_PAYMENT_SCREEN, extraArgs);
				} else {
					log.warn("FCM Token not found for traineeUserId: {}, skipping push notifications",
							paymentReminderDto.getTraineeUserId());
				}

				// Send email notifications
				sendEmailReminder(paymentReminderDto, extraArgs, title, message, daysText);

			} catch (Exception e) {
				log.error("Failed to process payment reminder for trainee: {}", paymentReminderDto.getTraineeUserId(),
						e);
			}
		}
	}

	private String formatDaysText(String notificationType, int dueDaysCount) {
		int days = Math.abs(dueDaysCount);

		if (days == 0) {
			return "today";
		} else if (days == 1) {
			return "OVERDUE".equals(notificationType) ? "1 day" : "tomorrow";
		} else {
			return days + " days";
		}
	}

	private String buildNotificationTitle(String titleTemplate, String amount, String daysText,
			String notificationType) {
		if (titleTemplate != null) {
			return formatTemplate(titleTemplate, amount, daysText);
		}

		return "OVERDUE".equals(notificationType) ? "Payment Overdue" : "Payment Reminder";
	}

	private String buildNotificationMessage(String messageTemplate, String amount, String daysText,
			String notificationType, int dueDaysCount) {
		if (messageTemplate != null) {
			return formatTemplate(messageTemplate, amount, daysText);
		}

		return buildDefaultMessage(amount, notificationType, Math.abs(dueDaysCount));
	}

	private String buildDefaultMessage(String amount, String notificationType, int days) {
		if ("OVERDUE".equals(notificationType)) {
			if (days == 1) {
				return "Your payment for " + amount + " is overdue by 1 day. Click to pay now. Ignore if already paid.";
			} else {
				return "Your payment for " + amount + " is overdue by " + days
						+ " days. Click to pay now. Ignore if already paid.";
			}
		} else {
			if (days == 0) {
				return "Your payment for " + amount + " is due today. Click to pay. Ignore if already paid.";
			} else if (days == 1) {
				return "Your payment for " + amount + " is due tomorrow. Click to pay. Ignore if already paid.";
			} else {
				return "Your payment for " + amount + " is due in " + days
						+ " days. Click to pay. Ignore if already paid.";
			}
		}
	}

	private void sendEmailReminder(PaymentReminderDto paymentReminderDto, Map<String, String> extraArgs, String title,
			String message, String daysText) {
		String recipientEmail = extraArgs.get("playerEmailId");

		if (recipientEmail == null) {
			log.info("No email address provided for trainee: {}", paymentReminderDto.getTraineeUserId());
			return;
		}

		try {
			// Get email template ID
			String templateId = getEmailTemplateId(paymentReminderDto.getAcademyId());

			if (templateId == null) {
				log.warn("Email template not configured for academy: {}", paymentReminderDto.getAcademyId());
				return;
			}
			
			// Build template data
			Map<String, String> templateData = new HashMap<>(extraArgs);
			templateData.put("amount", paymentReminderDto.getAmount());
			templateData.put("days", daysText);
			templateData.put("title", title);
			templateData.put("message", message);

			log.info("Sending email reminder to {} using template {}", recipientEmail, templateId);
			mailService.sendTemplateEmail(recipientEmail, title, templateId, templateData);
			log.info("Payment reminder email sent successfully to {}", recipientEmail);

		} catch (EmailSendException e) {
			log.error("Failed to send email reminder to {}: {}", recipientEmail, e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error sending email reminder to {}: {}", recipientEmail, e.getMessage());
		}
	}

	private String getEmailTemplateId(String academyId) {
		try {
			Academy academy = academyRepo.findById(academyId).orElse(null);

			if (academy != null && academy.getOrg() != null) {
				Optional<OrganisationConfig> orgConfigOpt = orgConfigRepo.findByOrgIdAndKey(academy.getOrg().getId(),
						PAYMENT_REMINDER_TEMPLATE_ID_KEY);

				if (orgConfigOpt.isPresent() && StringUtils.isNotEmpty(orgConfigOpt.get().getValue())) {
					return orgConfigOpt.get().getValue();
				}
			}

			if (StringUtils.isEmpty(paymentReminderTemplateId)) {
				log.warn("Default payment reminder template ID not configured in properties");
			}

			return paymentReminderTemplateId;

		} catch (Exception e) {
			log.error("Error retrieving email template ID for academy {}: {}", academyId, e.getMessage());
			return paymentReminderTemplateId;
		}
	}

	/**
	 * Format a template string with placeholders
	 * 
	 * @param template The template string with placeholders {amount} and {days}
	 * @param amount   The formatted amount to include
	 * @param days     The days text to include
	 * @return The formatted string
	 */
	private String formatTemplate(String template, String amount, String days) {
		return template.replace("{amount}", amount).replace("{days}", days);
	}

	@Override
	public PaymentDetailsDto getPaymentDetails(String enrollmentId, PaymentCategory paymentCategory)
			throws ResourceException {
		PaymentDetailsDto paymentDetailsDto = new PaymentDetailsDto();
		paymentDetailsDto.setEnrollmentId(enrollmentId);
		courseService.enrollmentExists(enrollmentId);
		List<Payment> payments = paymentRepo.findByTraineeCourseEnrollment_Id(enrollmentId);
		Pair<TraineeCourseEnrollmentDto, CourseDto> enrollmentCourseDetails = courseService
				.getByEnrollmentId(enrollmentId);

		if (enrollmentCourseDetails == null || enrollmentCourseDetails.getLeft() == null
				|| enrollmentCourseDetails.getRight() == null) {
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE,
					"Not able to fetch enrollment and course details.");
		}

		TraineeCourseEnrollmentDto traineeCourseEnrollmentDto = enrollmentCourseDetails.getLeft();
		CourseDto courseDto = enrollmentCourseDetails.getRight();

		paymentDetailsDto.setPaymentSchedule(traineeCourseEnrollmentDto.getPaymentSchedule());
		paymentDetailsDto.setCurrency(courseDto.getSchedule().getCurrency());
		setFuturePayments(paymentDetailsDto, payments, courseDto, traineeCourseEnrollmentDto, paymentCategory);
		return paymentDetailsDto;
	}

	/**
	 * This method sets the future payments for a given course enrollment. It will
	 * calculate the next due date, amount and whether the payment is due or not. It
	 * will also fill up the payment history map with the paid payments.
	 *
	 * @param paymentDetailsDto          The payment details dto to be populated.
	 * @param payments                   The list of payments made for the course
	 *                                   enrollment.
	 * @param courseDto                  The course dto for the course enrollment.
	 * @param traineeCourseEnrollmentDto The trainee course enrollment dto for the
	 *                                   course enrollment.
	 */
	// private void setFuturePayments(PaymentDetailsDto paymentDetailsDto,
	// List<Payment> payments, CourseDto courseDto,
	// TraineeCourseEnrollmentDto traineeCourseEnrollmentDto) {
	// if (paymentDetailsDto.getPaymentSchedule() == PaymentSchedule.FULL) {
	//
	// if (!CollectionUtils.isEmpty(payments)) {
	// paymentDetailsDto.setFuturePayments(new HashMap<>());
	// paymentDetailsDto.setNextDueAmount(null);
	// paymentDetailsDto.setNextDueAt(null);
	// paymentDetailsDto.setIsDue(false);
	// paymentDetailsDto.setIsInProgress(isLatestPaymentPending(payments));
	// Map<String, PaymentDto> paymentHistory = new HashMap<>();
	// for (Payment payment : payments) {
	// paymentHistory.put(payment.getPaymentInstallmentDate(),
	// modelMapper.map(payment, PaymentDto.class));
	// }
	// paymentDetailsDto.setPaymentHistory(paymentHistory);
	// } else {
	// String courseStartDate =
	// StringUtils.isEmpty(courseDto.getSchedule().getStartDate())
	// ? courseDto.getSchedule().getCustomDates().get(0)
	// : courseDto.getSchedule().getStartDate();
	// Map<String, Long> futurePayments = new HashMap<>();
	// futurePayments.put(courseStartDate, traineeCourseEnrollmentDto.getAmount() -
	// getTotalPaid(payments));
	// paymentDetailsDto.setFuturePayments(futurePayments);
	// paymentDetailsDto.setNextDueAt(courseStartDate);
	// paymentDetailsDto.setNextDueAmount(traineeCourseEnrollmentDto.getAmount());
	// LocalDate nowLocal =
	// ZonedDateTime.now(ZoneId.of(AppConstants.DEFAULT_TIMEZONE)).toLocalDate();
	// paymentDetailsDto.setIsDue(LocalDate.parse(courseStartDate).isBefore(nowLocal)
	// || LocalDate.parse(courseStartDate).isEqual(nowLocal));
	// paymentDetailsDto.setPaymentHistory(new HashMap<>());
	// paymentDetailsDto.setIsInProgress(isLatestPaymentPending(payments));
	// if (!CollectionUtils.isEmpty(payments)) {
	// Map<String, PaymentDto> paymentHistory = new HashMap<>();
	// for (Payment payment : payments) {
	// paymentHistory.put(payment.getPaymentInstallmentDate(),
	// modelMapper.map(payment, PaymentDto.class));
	// }
	// paymentDetailsDto.setPaymentHistory(paymentHistory);
	// }
	// }
	// } else if (paymentDetailsDto.getPaymentSchedule() == PaymentSchedule.MONTHLY)
	// {
	//
	// Map<String, Long> paidDues = CollectionUtils.isEmpty(payments) ? new
	// HashMap<>()
	// : payments.stream()
	// .filter(payment -> payment.getPaymentStatus() != PaymentStatus.FAILED
	// && payment.getPaymentStatus() != PaymentStatus.ABANDONED)
	// .collect(Collectors.toMap(Payment::getPaymentInstallmentDate,
	// Payment::getAmount));
	//
	// String startDate =
	// StringUtils.isEmpty(courseDto.getSchedule().getStartDate())
	// ? courseDto.getSchedule().getCustomDates().get(0)
	// : courseDto.getSchedule().getStartDate();
	// String endDate = StringUtils.isEmpty(courseDto.getSchedule().getEndDate())
	// ?
	// courseDto.getSchedule().getCustomDates().get(courseDto.getSchedule().getCustomDates().size()
	// - 1)
	// : courseDto.getSchedule().getEndDate();
	// List<String> dueDates = DateTimeUtils.getStartDateMonthly(startDate,
	// endDate);
	// String nextDueDate = null;
	// Map<String, Long> futurePayments = new HashMap<>();
	// Map<String, PaymentDto> paymentHistory = new HashMap<>();
	// for (String dueDate : dueDates) {
	// if (!paidDues.containsKey(dueDate)) {
	// if (nextDueDate == null) {
	// nextDueDate = dueDate;
	// }
	// futurePayments.put(dueDate, traineeCourseEnrollmentDto.getAmount());
	// } else {
	// paymentHistory
	// .put(dueDate,
	// modelMapper.map(
	// payments.stream()
	// .filter(payment -> payment.getPaymentInstallmentDate()
	// .equalsIgnoreCase(dueDate))
	// .findFirst().get(),
	// PaymentDto.class));
	// }
	// }
	// paymentDetailsDto.setFuturePayments(futurePayments);
	// paymentDetailsDto.setPaymentHistory(paymentHistory);
	// paymentDetailsDto.setNextDueAt(nextDueDate);
	// paymentDetailsDto.setNextDueAmount(traineeCourseEnrollmentDto.getAmount());
	// paymentDetailsDto.setIsInProgress(isLatestPaymentPending(payments));
	// LocalDate nowLocal =
	// ZonedDateTime.now(ZoneId.of(AppConstants.DEFAULT_TIMEZONE)).toLocalDate();
	// paymentDetailsDto
	// .setIsDue(!StringUtils.isEmpty(nextDueDate) &&
	// (LocalDate.parse(nextDueDate).isBefore(nowLocal)
	// || LocalDate.parse(nextDueDate).isEqual(nowLocal)));
	// } else if (paymentDetailsDto.getPaymentSchedule() == PaymentSchedule.YEARLY)
	// {
	//
	// Map<String, Long> paidDues = CollectionUtils.isEmpty(payments) ? new
	// HashMap<>()
	// : payments.stream()
	// .filter(payment -> payment.getPaymentStatus() != PaymentStatus.FAILED
	// && payment.getPaymentStatus() != PaymentStatus.ABANDONED)
	// .collect(Collectors.toMap(Payment::getPaymentInstallmentDate,
	// Payment::getAmount));
	//
	// String startDate =
	// StringUtils.isEmpty(courseDto.getSchedule().getStartDate())
	// ? courseDto.getSchedule().getCustomDates().get(0)
	// : courseDto.getSchedule().getStartDate();
	// String endDate = StringUtils.isEmpty(courseDto.getSchedule().getEndDate())
	// ?
	// courseDto.getSchedule().getCustomDates().get(courseDto.getSchedule().getCustomDates().size()
	// - 1)
	// : courseDto.getSchedule().getEndDate();
	// List<String> dueDates = DateTimeUtils.getStartDateYearly(startDate, endDate);
	// String nextDueDate = null;
	// Map<String, Long> futurePayments = new HashMap<>();
	// Map<String, PaymentDto> paymentHistory = new HashMap<>();
	// for (String dueDate : dueDates) {
	// if (!paidDues.containsKey(dueDate)) {
	// if (nextDueDate == null) {
	// nextDueDate = dueDate;
	// }
	// futurePayments.put(dueDate, traineeCourseEnrollmentDto.getAmount());
	// } else {
	// paymentHistory
	// .put(dueDate,
	// modelMapper.map(
	// payments.stream()
	// .filter(payment -> payment.getPaymentInstallmentDate()
	// .equalsIgnoreCase(dueDate))
	// .findFirst().get(),
	// PaymentDto.class));
	// }
	// }
	// paymentDetailsDto.setFuturePayments(futurePayments);
	// paymentDetailsDto.setPaymentHistory(paymentHistory);
	// paymentDetailsDto.setNextDueAt(nextDueDate);
	// paymentDetailsDto.setNextDueAmount(traineeCourseEnrollmentDto.getAmount());
	// paymentDetailsDto.setIsInProgress(isLatestPaymentPending(payments));
	// LocalDate nowLocal =
	// ZonedDateTime.now(ZoneId.of(AppConstants.DEFAULT_TIMEZONE)).toLocalDate();
	// paymentDetailsDto
	// .setIsDue(!StringUtils.isEmpty(nextDueDate) &&
	// (LocalDate.parse(nextDueDate).isBefore(nowLocal)
	// || LocalDate.parse(nextDueDate).isEqual(nowLocal)));
	// }
	// }

	private void setFuturePayments(PaymentDetailsDto dto, List<Payment> payments, CourseDto course,
			TraineeCourseEnrollmentDto enrollment, PaymentCategory paymentCategory) {
		Map<String, Long> futurePayments = new LinkedHashMap<>();
		Map<String, PaymentDto> paymentHistory = new HashMap<>();
		Map<String, Long> pendingPayments = new LinkedHashMap<>();
		long pendingAmount = 0; // Track pending payment amount

		if (PaymentCategory.REGISTRATION_FEE.toString().equals(paymentCategory.toString())) {
			pendingAmount = handleRegistrationFee(dto, payments, course, enrollment, futurePayments, pendingPayments,
					paymentHistory);
		} else {
			pendingAmount = handleCoursePayments(dto, payments, course, enrollment, futurePayments, pendingPayments,
					paymentHistory);
		}

		// Set DTO fields
		dto.setFuturePayments(futurePayments);
		dto.setPaymentHistory(paymentHistory);
		dto.setPendingPayments(pendingPayments);
		dto.setPaymentCategory(paymentCategory);
		dto.setPendingAmount(pendingPayments.values().stream().mapToLong(Long::longValue).sum());

		updateNextDueInformation(dto, futurePayments, pendingPayments);
	}

	private long handleRegistrationFee(PaymentDetailsDto dto, List<Payment> payments, CourseDto course,
			TraineeCourseEnrollmentDto enrollment, Map<String, Long> futurePayments, Map<String, Long> pendingPayments,
			Map<String, PaymentDto> paymentHistory) {
		Long registrationFee = course.getRegistrationFee();
		boolean hasRegistrationFee = registrationFee != null && registrationFee > 0;
		long totalPendingAmount = 0;

		if (hasRegistrationFee) {
			List<Payment> regPayments = payments.stream()
					.filter(payment -> payment.getPaymentCategory().equals(PaymentCategory.REGISTRATION_FEE))
					.filter(p -> p.getPaymentStatus() != PaymentStatus.FAILED
							&& p.getPaymentStatus() != PaymentStatus.ABANDONED)
					.collect(Collectors.toList());

			if (!regPayments.isEmpty()) {
				// Calculate total paid for registration fee
				long totalPaid = regPayments.stream().map(Payment::getAmount).reduce(0L, Long::sum);

				// Check if there's a pending amount
				if (totalPaid < registrationFee) {
					// Get the latest payment date
					Payment latestPayment = regPayments.stream()
							.max(Comparator.comparing(Payment::getPaymentInstallmentDate)).get();

					String dateStr = latestPayment.getPaymentInstallmentDate().toString();
					long pendingAmount = registrationFee - totalPaid;
					pendingPayments.put(dateStr, pendingAmount);
					pendingAmount += pendingAmount;
				}

				// Add the latest payment to history
				Payment latestPayment = regPayments.stream()
						.max(Comparator.comparing(Payment::getPaymentInstallmentDate)).get();

				String dateStr = latestPayment.getPaymentInstallmentDate().toString();
				paymentHistory.put(dateStr, modelMapper.map(latestPayment, PaymentDto.class));
			} else {
				// No payment made yet, full amount pending
				LocalDate dueDate = enrollment.getJoiningDate();

				String dateStr = dueDate.toString();
				futurePayments.put(dateStr, registrationFee);
				// For registration fee, we don't add to pending until the due date has passed
				LocalDate today = LocalDate.now(ZoneId.of("UTC"));
				if (dueDate.isBefore(today) || dueDate.isEqual(today)) {
					pendingPayments.put(dateStr, registrationFee);
					totalPendingAmount += registrationFee;
				}
			}
		}

		return totalPendingAmount;
	}

	private long handleCoursePayments(PaymentDetailsDto dto, List<Payment> payments, CourseDto course,
			TraineeCourseEnrollmentDto enrollment, Map<String, Long> futurePayments, Map<String, Long> pendingPayments,
			Map<String, PaymentDto> paymentHistory) {
		LocalDate joiningDate = enrollment.getJoiningDate();
		LocalDate dueDate = enrollment.getDueDate();
		LocalDate courseDate = null;

		if (dueDate == null) {
			courseDate = calculateDueDateFromJoiningDate(joiningDate, course);
		}

		if (joiningDate == null && courseDate == null) {
			throw new IllegalArgumentException("Either joining date or due date must be provided.");
		}

		// Calculate Course Fee Installments
		Long courseFee = enrollment.getAmount();
		PaymentSchedule schedule = enrollment.getPaymentSchedule();

		if (schedule != PaymentSchedule.FULL) {
			return processInstallmentPayments(payments, joiningDate, dueDate, courseDate, course, courseFee, schedule,
					futurePayments, pendingPayments, paymentHistory);
		} else {
			return processFullPayment(payments, course, courseFee, futurePayments, pendingPayments);
		}

	}

	private LocalDate calculateDueDateFromJoiningDate(LocalDate joiningDate, CourseDto course) {
		if (joiningDate == null || course.getSchedule() == null || course.getSchedule().getStartDate() == null) {
			return null;
		}

		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			int dueYear = joiningDate.getYear();
			int dueDay = LocalDate.parse(course.getSchedule().getStartDate(), formatter).getDayOfMonth();
			int dueMonth = dueDay < joiningDate.getDayOfMonth() ? (joiningDate.getMonthValue() + 1)
					: joiningDate.getMonthValue();

			// Handle month overflow
			if (dueMonth > 12) {
				dueMonth = 1;
				dueYear += 1;
			}

			// Ensure the day is valid for the calculated month/year
			return LocalDate.of(dueYear, dueMonth, Math.min(dueDay, YearMonth.of(dueYear, dueMonth).lengthOfMonth()));
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Invalid course start date format", e);
		}
	}

	private long processInstallmentPayments(List<Payment> payments, LocalDate joiningDate, LocalDate dueDate,
			LocalDate courseDate, CourseDto course, Long courseFee, PaymentSchedule schedule,
			Map<String, Long> futurePayments, Map<String, Long> pendingPayments,
			Map<String, PaymentDto> paymentHistory) {
		long totalPendingAmount = 0;

		List<Payment> validPayments = payments.stream()
				.filter(p -> !p.getPaymentCategory().equals(PaymentCategory.REGISTRATION_FEE))
				.filter(p -> p.getPaymentStatus() != PaymentStatus.FAILED
						&& p.getPaymentStatus() != PaymentStatus.ABANDONED)
				.collect(Collectors.toList());

		Map<String, Long> paidAmountsByDate = new HashMap<>();
		for (Payment payment : validPayments) {
			String dateStr = payment.getPaymentInstallmentDate().toString();
			paidAmountsByDate.merge(dateStr, payment.getAmount(), Long::sum);
		}

		List<LocalDate> dueDates = generateDueDates(schedule, joiningDate, dueDate == null ? courseDate : dueDate,
				course.getSchedule());
		Collections.sort(dueDates);

		boolean needsProration = !dueDate.equals(joiningDate);
		if (needsProration) {
			dueDates.add(0, joiningDate);
		}
		long installmentAmount = courseFee;

		long excess = 0L;
		if (needsProration && !dueDates.isEmpty()) {
			LocalDate firstDueDate = dueDates.get(0);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			LocalDate endDate = LocalDate.parse(course.getSchedule().getEndDate(), formatter);
			// LocalDate startDate = LocalDate.parse(course.getSchedule().getStartDate(),
			// formatter);
			long daysDiff = ChronoUnit.DAYS.between(joiningDate, dueDate);
			long proratedAmount = calculateProratedAmount(joiningDate, installmentAmount, daysDiff, schedule);

			ProcessInstallmentResult firstResult = processInstallment(firstDueDate, proratedAmount, paidAmountsByDate,
					validPayments, futurePayments, pendingPayments, paymentHistory, excess);
			excess = firstResult.excess;
			totalPendingAmount += firstResult.pending;

			for (int i = 1; i < dueDates.size(); i++) {

				LocalDate date = dueDates.get(i);
				boolean isLastDueDate = date.equals(dueDates.get(dueDates.size() - 1));
				Long newInstallmentAmount = null;

				if (isLastDueDate) {
					// DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					// LocalDate endDate = LocalDate.parse(course.getSchedule().getEndDate(),
					// formatter);

					// Calculate actual days between due date and course end date
					long daysBetween = ChronoUnit.DAYS.between(date, endDate) + 1;
					// Prorate only if the due date's day is different from the end date's day
					if (daysBetween > 0) {
						newInstallmentAmount = calculateProratedAmount(date, installmentAmount, daysBetween, schedule);
					}
				}
				ProcessInstallmentResult result = processInstallment(date,
						newInstallmentAmount == null ? installmentAmount : newInstallmentAmount, paidAmountsByDate,
						validPayments, futurePayments, pendingPayments, paymentHistory, excess);
				excess = result.excess;
				totalPendingAmount += result.pending;
			}
		} else {
			for (LocalDate date : dueDates) {
				boolean isLastDueDate = date.equals(dueDates.get(dueDates.size() - 1));
				Long newInstallmentAmount = null;
				if (isLastDueDate) {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					LocalDate endDate = LocalDate.parse(course.getSchedule().getEndDate(), formatter);

					// Calculate actual days between due date and course end date
					long daysBetween = ChronoUnit.DAYS.between(date, endDate) + 1;
					// Prorate only if the due date's day is different from the end date's day
					if (daysBetween > 0) {
						newInstallmentAmount = calculateProratedAmount(date, installmentAmount, daysBetween, schedule);
					}
				}
				ProcessInstallmentResult result = processInstallment(date,
						newInstallmentAmount == null ? installmentAmount : newInstallmentAmount, paidAmountsByDate,
						validPayments, futurePayments, pendingPayments, paymentHistory, excess);
				excess = result.excess;
				totalPendingAmount += result.pending;
			}
		}

		return totalPendingAmount;
	}

	private static class ProcessInstallmentResult {
		long excess;
		long pending;

		public ProcessInstallmentResult(long excess, long pending) {
			this.excess = excess;
			this.pending = pending;
		}
	}

	private ProcessInstallmentResult processInstallment(LocalDate date, long expectedAmount,
			Map<String, Long> paidAmountsByDate, List<Payment> validPayments, Map<String, Long> futurePayments,
			Map<String, Long> pendingPayments, Map<String, PaymentDto> paymentHistory, long currentExcess) {

		String dateStr = date.toString();
		Long paidAmount = paidAmountsByDate.getOrDefault(dateStr, 0L);

		long appliedExcess = Math.min(currentExcess, expectedAmount);
		long adjustedExpected = expectedAmount - appliedExcess;
		long remainingExcess = currentExcess - appliedExcess;

		long overpayment = paidAmount - adjustedExpected;

		long newExcess;
		long pendingForDate;

		if (overpayment >= 0) {
			newExcess = remainingExcess + overpayment;
			pendingForDate = 0;
		} else {
			pendingForDate = adjustedExpected - paidAmount;
			newExcess = 0;
		}

		if (pendingForDate > 0) {
			futurePayments.put(dateStr, pendingForDate);
			LocalDate today = LocalDate.now(ZoneId.of("UTC"));
			if (date.isBefore(today) || date.isEqual(today)) {
				pendingPayments.put(dateStr, pendingForDate);
			}
		}

		if (paidAmount > 0) {
			List<Payment> paymentsForDate = validPayments.stream()
					.filter(p -> p.getPaymentInstallmentDate().toString().equals(dateStr)).collect(Collectors.toList());

			for (int i = 0; i < paymentsForDate.size(); i++) {
				String key = paymentsForDate.size() == 1 ? dateStr : dateStr + "_" + (i + 1);
				paymentHistory.put(key, modelMapper.map(paymentsForDate.get(i), PaymentDto.class));
			}
		}
		return new ProcessInstallmentResult(newExcess, pendingForDate);
	}

	private long processFullPayment(List<Payment> payments, CourseDto course, Long courseFee,
			Map<String, Long> futurePayments,

			Map<String, Long> pendingPayments) {
		// Filter course payments (exclude registration fees, failed, and abandoned)
		List<Payment> validPayments = payments.stream()
				.filter(p -> !p.getPaymentCategory().equals(PaymentCategory.REGISTRATION_FEE))
				.filter(p -> p.getPaymentStatus() != PaymentStatus.FAILED
						&& p.getPaymentStatus() != PaymentStatus.ABANDONED)
				.collect(Collectors.toList());

		// Calculate total paid amount
		long totalPaid = validPayments.stream().map(Payment::getAmount).reduce(0L, Long::sum);

		// Check if any payment is pending
		long pendingAmount = 0;
		if (totalPaid < courseFee) {
			pendingAmount = courseFee - totalPaid;
			LocalDate today = LocalDate.now(ZoneId.of("UTC"));

			// Add to future payments if no valid payment exists
			if (validPayments.isEmpty() && course.getSchedule() != null) {
				String dueDateStr = course.getSchedule().getStartDate();
				futurePayments.put(dueDateStr, courseFee);

				// If due date is today or past, add to pending payments
				try {
					LocalDate dueDate = LocalDate.parse(dueDateStr);
					if (dueDate.isBefore(today) || dueDate.isEqual(today)) {
						pendingPayments.put(dueDateStr, courseFee);
					}
				} catch (DateTimeParseException e) {
					// If can't parse date, still track the pending amount
					pendingPayments.put(dueDateStr, courseFee);
				}
			} else if (!validPayments.isEmpty()) {
				// Get the latest payment date
				String latestPaymentDate = validPayments.stream()
						.max(Comparator.comparing(Payment::getPaymentInstallmentDate))
						.map(p -> p.getPaymentInstallmentDate().toString()).orElse(course.getSchedule().getStartDate());

				// Add pending amount to this date
				pendingPayments.put(latestPaymentDate, pendingAmount);
			}
		}

		return pendingAmount;
	}

	private void updateNextDueInformation(PaymentDetailsDto dto, Map<String, Long> futurePayments,
			Map<String, Long> pendingPayments) {
		// First, check if there are any pending payments
		if (!pendingPayments.isEmpty()) {
			// Get the earliest pending payment date
			Optional<String> earliestPendingDate = pendingPayments.keySet().stream().min(Comparator.naturalOrder());

			earliestPendingDate.ifPresent(date -> {
				dto.setNextDueAt(date);
				dto.setNextDueAmount(pendingPayments.get(date));
				dto.setIsDue(true); // If it's pending, it's always due
			});
		} else {
			// No pending payments, check future payments
			Optional<String> nextDue = futurePayments.keySet().stream().min(Comparator.naturalOrder());

			nextDue.ifPresent(date -> {
				dto.setNextDueAt(date);
				dto.setNextDueAmount(futurePayments.get(date));

				// Check if payment is due
				LocalDate dueDate = LocalDate.parse(date);
				LocalDate today = LocalDate.now(ZoneId.of("UTC"));
				dto.setIsDue(dueDate.isBefore(today) || dueDate.isEqual(today));
			});
		}
	}

	private List<LocalDate> generateDueDates(PaymentSchedule schedule, LocalDate joiningDate, LocalDate dueDate,
			ScheduleDto scheduleDto) {
		if (scheduleDto == null || scheduleDto.getEndDate() == null) {
			return new ArrayList<>(); // Return empty modifiable list instead
		}

		LocalDate start = dueDate != null ? dueDate : joiningDate;
		LocalDate end = LocalDate.parse(scheduleDto.getEndDate());

		if (start == null || end == null || start.isAfter(end)
				|| (joiningDate.getDayOfMonth() != dueDate.getDayOfMonth() && start.getMonth() == end.getMonth()
						&& start.getYear() == end.getYear())) {
			return new ArrayList<>(); // Return empty modifiable list instead
		}

		List<LocalDate> result;
		switch (schedule) {
		case MONTHLY:
			result = new ArrayList<>(DateTimeUtils.getMonthlyDueDates(start, end));
			break;
		case YEARLY:
			result = new ArrayList<>(DateTimeUtils.getYearlyDueDates(start, end));
			break;
		case HALFYEARLY:
			result = new ArrayList<>(DateTimeUtils.getHalfYearlyDueDates(start, end));
			break;
		case QUARTERLY:
			result = new ArrayList<>(DateTimeUtils.getQuarterlyDueDates(start, end));
			break;
		default:
			result = new ArrayList<>();
		}

		return result;
	}

	private long calculateProratedAmount(LocalDate joiningDate, LocalDate startDate, LocalDate endDate,
			long installmentAmount) {
		LocalDate monthEnd;

		// Check if joining date and end date are in the same month and year
		if (joiningDate.getMonth() == endDate.getMonth() && joiningDate.getYear() == endDate.getYear()) {
			monthEnd = endDate; // Use end date when in same month and year
		} else if (joiningDate.getDayOfMonth() < startDate.getDayOfMonth()) {
			monthEnd = LocalDate.of(joiningDate.getYear(), joiningDate.getMonth(), startDate.getDayOfMonth());
		} else {
			monthEnd = joiningDate.withDayOfMonth(joiningDate.lengthOfMonth()); // Last day of joining date's month
		}

		long daysInMonth = joiningDate.lengthOfMonth();
		long daysRemaining = ChronoUnit.DAYS.between(joiningDate, monthEnd) + 1;
		return Math.round((double) installmentAmount * daysRemaining / daysInMonth);
	}

	private long calculateProratedAmount(LocalDate joiningDate, long installmentAmount, long totalDays,
			PaymentSchedule schedule) {
		long daysInMonth;
		switch (schedule) {
		case YEARLY:
			daysInMonth = 365;
			break;
		case QUARTERLY:
			daysInMonth = 90;
			break;
		case HALFYEARLY:
			daysInMonth = 182;
			break;
		default:
			daysInMonth = joiningDate.lengthOfMonth();
		}
		return Math.round((double) installmentAmount * totalDays / daysInMonth);
	}

	private Long getTotalPaid(List<Payment> payments) {
		return CollectionUtils.isEmpty(payments) ? 0L
				: payments.stream()
						.filter(payment -> payment.getPaymentStatus() != PaymentStatus.FAILED
								&& payment.getPaymentStatus() != PaymentStatus.ABANDONED)
						.map(Payment::getAmount).reduce(0L, Long::sum);
	}

	private List<CoursePaymentDetailsDto> sort(List<CoursePaymentDetailsDto> coursePaymentDetailsDtos) {
		return coursePaymentDetailsDtos.stream().sorted(Comparator
				.comparing(coursePaymentDetailsDto -> coursePaymentDetailsDto.getUserProfile().getDisplayName()))
				.toList();
	}

	private boolean isLatestPaymentPending(List<Payment> payments) {
		if (CollectionUtils.isEmpty(payments)) {
			return false;
		}
		List<Payment> mutablePaymentList = new ArrayList<>(payments);

		mutablePaymentList.sort(Comparator.comparing(
				person -> LocalDate.parse(person.getPaymentInstallmentDate(), DateTimeUtils.YYYY_MM_DD_FORMATTER)));
		return payments.get(payments.size() - 1).getPaymentStatus() == PaymentStatus.PENDING;
	}
}
