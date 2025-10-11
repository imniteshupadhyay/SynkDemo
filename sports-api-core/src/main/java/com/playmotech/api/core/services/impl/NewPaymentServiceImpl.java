package com.playmotech.api.core.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.modelmapper.internal.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.PaymentStatus;
import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.controllers.PaymentController.SettlePaymentRequestDto;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.OrganisationConfig;
import com.playmotech.api.core.dao_postgres.Payment;
import com.playmotech.api.core.dao_postgres.PaymentLedger;
import com.playmotech.api.core.dao_postgres.PaymentLedger.LedgerType;
import com.playmotech.api.core.dao_postgres.PaymentLedger.PaymentEntryStatus;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.CoursePaymentDetailsDto;
import com.playmotech.api.core.dto.DiscountRequest;
import com.playmotech.api.core.dto.DiscountRequest.DiscountEntry;
import com.playmotech.api.core.dto.InitPaymentDto;
import com.playmotech.api.core.dto.InstallmentInfo;
import com.playmotech.api.core.dto.PaymentDetailsDto;
import com.playmotech.api.core.dto.PaymentDto;
import com.playmotech.api.core.dto.PaymentDueDto;
import com.playmotech.api.core.dto.PaymentLedgerDto;
import com.playmotech.api.core.dto.PaymentReminderDto;
import com.playmotech.api.core.dto.ReceiptDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.UpdatePaymentDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.EmailSendException;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.helper.PaymentLedgerHelper;
import com.playmotech.api.core.repo.AcademyRepo;
import com.playmotech.api.core.repo.OrgConfigRepo;
import com.playmotech.api.core.repo.PaymentLedgerRepository;
import com.playmotech.api.core.repo.PaymentRepo;
import com.playmotech.api.core.repo.TraineeCourseEnrollmentRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.PaymentLedgerDao;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.IMailService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.services.NewPaymentService;
import com.playmotech.api.core.utils.ChartUtils;
import com.playmotech.api.core.utils.CurrencyUtils;
import com.playmotech.api.core.utils.DateTimeUtils;
import com.playmotech.api.core.utils.InstallmentUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewPaymentServiceImpl implements NewPaymentService {

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
	private final TraineeCourseEnrollmentRepo traineeCourseEnrollmentRepo;
	private final PaymentLedgerRepository ledgerRepository;

	private final InstallmentUtil installmentUtil;
	private final PaymentLedgerHelper ledgerHelper;

	@Value("${payments-base-url}")
	private String paymentsBaseUrl;

	@Value("${storage.payments-bucket}")
	private String paymentsBucket;

	@Value("${sendgrid.template.payment-reminder}")
	private String paymentReminderTemplateId;

	@Value("${excel.file.path}")
	private String excelFilePath;

	@Override
	public ServiceResponse addDiscount(DiscountRequest request) {
		try {
			Optional<TraineeCourseEnrollment> enrollmentOpt = traineeCourseEnrollmentRepo
					.findById(request.getEnrollmentId());
			if (enrollmentOpt.isEmpty()) {
				log.warn("❌ Enrollment not found for ID: {}", request.getEnrollmentId());
				return ResponseBuilder.error("Enrollment not found", ApiResponse.ENROLLMENT_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			TraineeCourseEnrollment enrollment = enrollmentOpt.get();

			// Step 1: Group discounts by category
			Map<PaymentCategory, List<DiscountRequest.DiscountEntry>> discountMap = request.getDiscounts().stream()
					.collect(Collectors.groupingBy(DiscountRequest.DiscountEntry::getCategory));

			// Step 2: Prepare payment details map
			Map<PaymentCategory, PaymentDetailsDto> paymentDetailsByCategory = new HashMap<>();

			for (PaymentCategory category : discountMap.keySet()) {
				log.info("📦 Fetching payment details for category: {}", category);

				ServiceResponse paymentResponse = getPaymentDetails(request.getEnrollmentId(), category);
				if (!paymentResponse.getHttpStatus().is2xxSuccessful()) {
					log.warn("❌ Payment details not found for category: {}", category);
					return paymentResponse;
				}

				PaymentDetailsDto paymentDetails = (PaymentDetailsDto) paymentResponse.getBody();
				paymentDetailsByCategory.put(category, paymentDetails);
			}

			// Step 3: Apply each discount
			for (Map.Entry<PaymentCategory, List<DiscountRequest.DiscountEntry>> entry : discountMap.entrySet()) {
				PaymentCategory category = entry.getKey();
				List<DiscountRequest.DiscountEntry> discounts = entry.getValue();
				PaymentDetailsDto paymentDetails = paymentDetailsByCategory.get(category);

				for (DiscountRequest.DiscountEntry discount : discounts) {
					switch (category) {
					case COURSE_FEE:
						applyCourseLevelDiscount(enrollment, discount.getDiscountAmount(),
								discount.isScheduleSpecific(), discount, paymentDetails, request.getCreatedById());
						break;

					case REGISTRATION_FEE:
						applyRegistrationLevelDiscount(enrollment, discount.getDiscountAmount(), discount,
								paymentDetails, request.getCreatedById());
						break;

					default:
						log.warn("⚠️ Unsupported payment category: {}", category);
						break;
					}
				}
			}

			return ResponseBuilder.success(ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.CREATED);

		} catch (Exception ex) {
			log.error("❌ Error in addDiscount for enrollment {}: {}", request.getEnrollmentId(), ex.getMessage(), ex);
			return ResponseBuilder.error("Unexpected error while applying discount: " + ex.getMessage(),
					ApiResponse.DUES_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void applyRegistrationLevelDiscount(TraineeCourseEnrollment enrollment, Double discountAmount,
			DiscountEntry discount, PaymentDetailsDto paymentDetails, String createdBy) {
		log.info("Applying registration discount ₹{} to enrollment {}", discountAmount, enrollment.getId());

		if (discountAmount == null || discountAmount <= 0) {
			log.warn("Invalid discount amount: {}", discountAmount);
			return;
		}

		List<PaymentLedger> paymentLedgers = ledgerRepository.findByEnrollment_Id(enrollment.getId());

		// Get all DEBIT registration fee ledger entries
		List<PaymentLedger> registrationDebits = paymentLedgers.stream()
				.filter(pl -> pl.getCategory() == PaymentCategory.REGISTRATION_FEE)
				.filter(pl -> pl.getEntryType() == PaymentLedger.PaymentEntryType.REGISTRATION)
				.filter(pl -> pl.getLedgerType() == PaymentLedger.LedgerType.DEBIT)
				.filter(pl -> pl.getRemainingAmount() != null && pl.getRemainingAmount() > 0)
				.collect(Collectors.toList());

		if (registrationDebits.isEmpty()) {
			log.warn("No registration debit entries with remaining amount found for enrollment {}", enrollment.getId());
			return;
		}

		double remainingDiscount = discountAmount;

		// Apply discount across multiple debit ledger entries if needed
		for (PaymentLedger debit : registrationDebits) {
			Double remaining = debit.getRemainingAmount();
			if (remaining == null)
				remaining = 0.0;

			if (remainingDiscount <= 0)
				break;

			double originalAmount = debit.getAmount();

			if (remaining >= remainingDiscount) {
				debit.setRemainingAmount(remaining - remainingDiscount);
				remainingDiscount = 0;
			} else {
				remainingDiscount -= remaining;
				debit.setRemainingAmount(0.0);
			}

			// Update entry status
			if (debit.getRemainingAmount() == 0.0) {
				debit.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
			} else if (debit.getRemainingAmount() < originalAmount) {
				debit.setEntryStatus(PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED);
			}
		}

		if (remainingDiscount > 0) {
			log.warn("Discount amount ₹{} exceeds total remaining registration fee amount; excess discount ignored",
					remainingDiscount);
		}

		// Save all updated debits
		ledgerRepository.saveAll(registrationDebits);

		// Create credit discount entry for full discount amount
		PaymentLedger discountCredit = ledgerHelper.createDiscountLedger(enrollment.getAcademy().getId(),
				enrollment.getCourse().getId(), enrollment.getTraineeUserProfile(), enrollment,
				discountAmount.longValue(), 0L, LocalDate.now(), PaymentCategory.REGISTRATION_FEE, discount.getReason(),
				createdBy);

		// Save the discount credit entry
		ledgerRepository.save(discountCredit);

		log.info("Applied discount ₹{}: updated {} debit ledger entries, created settled credit entry", discountAmount,
				registrationDebits.size());
	}

	private void applyCourseLevelDiscount(TraineeCourseEnrollment enrollment, Double discountAmount,
			boolean scheduleSpecific, DiscountEntry discount, PaymentDetailsDto paymentDetails, String createdBy) {
		log.info("Applying course discount ₹{} to enrollment {}", discountAmount, enrollment.getId());

		if (discountAmount == null || discountAmount <= 0) {
			log.warn("Invalid discount amount: {}", discountAmount);
			return;
		}

		if (!scheduleSpecific) {

			List<PaymentLedger> paymentLedgers = ledgerRepository.findByEnrollment_Id(enrollment.getId());

			List<PaymentLedger> pastDueLedgers = paymentLedgers.stream()
					.filter(ledger -> ledger.getCategory() == PaymentCategory.COURSE_FEE
							&& ledger.getLedgerType() == PaymentLedger.LedgerType.DEBIT
							&& ledger.getEntryType() == PaymentLedger.PaymentEntryType.PAST_DUE
							&& (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
									|| ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
					.collect(Collectors.toList());

			// Current due
			Long currentDue = enrollment.getFinalDueAmount();

			// Future payments (date, amount)
			Map<String, Long> futurePayments = paymentDetails.getFuturePayments();

			// Apply discount to past dues, current due, and future payments
			Long remainingDiscount = discountAmount.longValue();

			// Apply discount to past due ledgers first
			for (PaymentLedger ledger : pastDueLedgers) {
				if (remainingDiscount <= 0)
					break;

				Double pendingAmount = ledger.getRemainingAmount();
				if (pendingAmount > 0) {
					Long discountForThisLedger = Math.min(remainingDiscount, pendingAmount.longValue());

					// Update the ledger's remaining amount
					ledger.setRemainingAmount(pendingAmount - discountForThisLedger);
					remainingDiscount -= discountForThisLedger;

					double originalAmount = ledger.getAmount();

					// Update status if fully settled
					if (ledger.getRemainingAmount() == 0.0) {
						ledger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
					} else if (ledger.getRemainingAmount() < originalAmount) {
						ledger.setEntryStatus(PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED);
					}

					log.info("Applied ₹{} discount to past due ledger {}. Remaining amount: ₹{}", discountForThisLedger,
							ledger.getId(), ledger.getRemainingAmount());
				}
			}

			// Apply remaining discount to current due only if past dues are handled
			if (remainingDiscount > 0 && currentDue != null && currentDue > 0) {
				Long discountForCurrentDue = Math.min(remainingDiscount, currentDue);

				// Update current due amount
				enrollment.setFinalDueAmount(currentDue - discountForCurrentDue);
				remainingDiscount -= discountForCurrentDue;

				log.info("Applied ₹{} discount to current due. Remaining current due: ₹{}", discountForCurrentDue,
						enrollment.getFinalDueAmount());

				// If current due is fully utilized, check remaining discount
				if (enrollment.getFinalDueAmount() == 0) {
					if (remainingDiscount == 0) {
						// No remaining discount - set next future payment as current due
						if (!futurePayments.isEmpty()) {
							Map.Entry<String, Long> nextPayment = futurePayments.entrySet().stream()
									.min(Map.Entry.comparingByKey()).orElse(null);

							if (nextPayment != null) {
								enrollment.setDuesOn(LocalDate.parse(nextPayment.getKey()));
								enrollment.setFinalDueAmount(nextPayment.getValue());
								futurePayments.remove(nextPayment.getKey());

								log.info(
										"Current due fully utilized with no remaining discount. Set next due date: {} with amount: ₹{}",
										nextPayment.getKey(), nextPayment.getValue());
							}
						}
					} else {
						// Still have remaining discount - apply to future payments first, then set
						// earliest
						log.info(
								"Current due fully utilized but remaining discount: ₹{}. Processing future payments first.",
								remainingDiscount);
					}
				}
			}

			// Apply remaining discount to future payments
			if (remainingDiscount > 0 && !futurePayments.isEmpty()) {
				// Sort future payments by date
				List<Map.Entry<String, Long>> sortedFuturePayments = futurePayments.entrySet().stream()
						.sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());

				for (Map.Entry<String, Long> payment : sortedFuturePayments) {
					if (remainingDiscount <= 0)
						break;

					String paymentDate = payment.getKey();
					Long paymentAmount = payment.getValue();

					Long discountForThisPayment = Math.min(remainingDiscount, paymentAmount);
					Long updatedAmount = paymentAmount - discountForThisPayment;

					futurePayments.put(paymentDate, updatedAmount);
					remainingDiscount -= discountForThisPayment;

					log.info("Applied ₹{} discount to future payment on {}. Updated amount: ₹{}",
							discountForThisPayment, paymentDate, updatedAmount);

					// If this future payment is fully utilized, remove it
					if (updatedAmount == 0) {
						futurePayments.remove(paymentDate);
						log.info("Future payment on {} fully utilized and removed", paymentDate);
					}
				}

				// After applying discount to future payments, set earliest remaining payment as
				// current due
				// (only if current due was 0 and we had remaining discount)
				if (enrollment.getFinalDueAmount() == 0 && !futurePayments.isEmpty()) {
					Map.Entry<String, Long> earliestPayment = futurePayments.entrySet().stream()
							.min(Map.Entry.comparingByKey()).orElse(null);

					if (earliestPayment != null) {
						enrollment.setDuesOn(LocalDate.parse(earliestPayment.getKey()));
						enrollment.setFinalDueAmount(earliestPayment.getValue());
						futurePayments.remove(earliestPayment.getKey());

						log.info("Set earliest remaining future payment as current due. Date: {} Amount: ₹{}",
								earliestPayment.getKey(), earliestPayment.getValue());
					}
				}

				// If all future payments are fully utilized, set last future date with 0 amount
				if (futurePayments.isEmpty() && !sortedFuturePayments.isEmpty()) {
					String lastDate = sortedFuturePayments.get(sortedFuturePayments.size() - 1).getKey();
					enrollment.setDuesOn(LocalDate.parse(lastDate));
					enrollment.setFinalDueAmount(0L);

					log.info("All future payments fully utilized. Set final due date: {} with amount: 0", lastDate);
				}
			}

			// If discount still remains, create a credit entry
			if (remainingDiscount > 0) {
				PaymentLedger additionalCredit = ledgerHelper.createCreditLedger(enrollment.getAcademy().getId(),
						enrollment.getCourse().getId(), enrollment.getTraineeUserProfile(), enrollment,
						remainingDiscount, LocalDate.now(), PaymentCategory.COURSE_FEE,
						discount.getReason() + " - Excess Credit", createdBy);

				ledgerRepository.save(additionalCredit);

				log.info("Created additional credit entry for remaining discount: ₹{}", remainingDiscount);
			}

			// Save all updated debits (only if scheduleSpecific is false)

			ledgerRepository.saveAll(pastDueLedgers);

			// Create credit discount entry for full discount amount
			PaymentLedger discountCredit = ledgerHelper.createDiscountLedger(enrollment.getAcademy().getId(),
					enrollment.getCourse().getId(), enrollment.getTraineeUserProfile(), enrollment,
					discountAmount.longValue(), 0L, LocalDate.now(), PaymentCategory.COURSE_FEE, discount.getReason(),
					createdBy);

			// Save the discount credit entry
			ledgerRepository.save(discountCredit);

			log.info("Applied discount ₹{}: scheduleSpecific={}, updated {} debit ledger entries, created credit entry",
					discountAmount, scheduleSpecific, scheduleSpecific ? 0 : pastDueLedgers.size());

		} else {
			// scheduleSpecific is true - no immediate application, just store discount for
			// future use
			log.info("Schedule specific discount - storing discount amount ₹{} for future application", discountAmount);
			// The discount will be used when processing future payments/schedules
			// No immediate changes to current due or past dues

			enrollment.setUseForFuture(discount.isFutureUsable());
			enrollment.setDiscountAmount(discountAmount.longValue());
		}

		// Save updated enrollment (only if scheduleSpecific is false and changes were
		// made)
		traineeCourseEnrollmentRepo.save(enrollment);

	}

	@Override
	@Transactional
	public ServiceResponse updateDues(String enrollmentId, LocalDate todayDate) {
		try {
			log.info("🔄 Starting updateDues for enrollmentId: {} and date: {}", enrollmentId, todayDate);

			// 1️⃣ Validate enrollment exists
			Optional<TraineeCourseEnrollment> enrollmentOpt = traineeCourseEnrollmentRepo.findById(enrollmentId);
			if (enrollmentOpt.isEmpty()) {
				log.warn("❌ Enrollment not found for ID: {}", enrollmentId);
				return ResponseBuilder.error("Enrollment not found", ApiResponse.ENROLLMENT_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}
			TraineeCourseEnrollment savedEnrollment = enrollmentOpt.get();

			// 2️⃣ Get payment details
			ServiceResponse serviceResponse = getPaymentDetails(enrollmentId, PaymentCategory.COURSE_FEE);
			PaymentDetailsDto paymentDetailsDto = (PaymentDetailsDto) serviceResponse.getBody();

			// 3️⃣ Fetch enrollment and course details
			Pair<TraineeCourseEnrollmentDto, CourseDto> courseEnrollmentDetails = courseService
					.getByEnrollmentId(enrollmentId);
			if (courseEnrollmentDetails == null || courseEnrollmentDetails.getLeft() == null
					|| courseEnrollmentDetails.getRight() == null) {
				log.warn("❌ Enrollment or course details missing for enrollment ID: {}", enrollmentId);
				return ResponseBuilder.error("Unable to fetch enrollment or course details.",
						ApiResponse.COURSE_DETAILS_NOT_FOUND, HttpStatus.NOT_FOUND);
			}

			TraineeCourseEnrollmentDto enrollmentDto = courseEnrollmentDetails.getLeft();
			CourseDto courseDto = courseEnrollmentDetails.getRight();

			// 4️⃣ Extract payment details data
			Map<String, Long> futurePayments = paymentDetailsDto.getFuturePayments();
			Long pendingAmount = paymentDetailsDto.getPendingAmount();
			String nextDueDateStr = paymentDetailsDto.getNextDueAt();

			// 5️⃣ Parse next due date
			LocalDate nextDueDate = null;
			if (nextDueDateStr != null && !nextDueDateStr.isEmpty()) {
				try {
					nextDueDate = LocalDate.parse(nextDueDateStr);
				} catch (Exception e) {
					log.error("❌ Error parsing next due date: {}", nextDueDateStr, e);
					return ResponseBuilder.error("Invalid next due date format", ApiResponse.INVALID_DATE_FORMAT,
							HttpStatus.BAD_REQUEST);
				}
			}

			// 6️⃣ Get current dues info from enrollment
			LocalDate currentDuesOn = enrollmentDto.getDuesOn();
			Long currentDuesAmount = enrollmentDto.getFinalDueAmount();

			// 7️⃣ Check if there are future payments
			if (futurePayments == null || futurePayments.isEmpty()) {
				log.info("📘 No future payments for enrollment ID: {}", enrollmentId);
				Map<String, Object> preview = new LinkedHashMap<>();
				preview.put("message", "No future payments scheduled.");
				preview.put("today", todayDate);
				preview.put("pendingAmount", pendingAmount);
				return ResponseBuilder.success(preview, ApiResponse.NO_MORE_INSTALLMENTS, HttpStatus.OK);
			}

			// 8️⃣ Find the actual next due date from future payments (after current duesOn)
			LocalDate actualNextDueDate = null;
			Long actualNextDueAmount = null;

			List<LocalDate> futureDates = new ArrayList<>();
			for (String dateStr : futurePayments.keySet()) {
				try {
					LocalDate date = LocalDate.parse(dateStr);
					LocalDate comparisonDate = currentDuesOn != null ? currentDuesOn : nextDueDate;
					if (comparisonDate == null || date.isAfter(comparisonDate)) {
						futureDates.add(date);
					}
				} catch (Exception e) {
					log.warn("❌ Error parsing future payment date: {}", dateStr, e);
				}
			}

			if (futureDates.isEmpty()) {
				log.info("📘 No future payments after current due date for enrollment ID: {}", enrollmentId);
				Map<String, Object> preview = new LinkedHashMap<>();
				preview.put("message", "No more future installments after current due date.");
				preview.put("today", todayDate);
				preview.put("currentDuesOn", currentDuesOn);
				preview.put("pendingAmount", pendingAmount);
				return ResponseBuilder.success(preview, ApiResponse.NO_MORE_INSTALLMENTS, HttpStatus.OK);
			}

			// Sort and get the earliest future date
			Collections.sort(futureDates);
			actualNextDueDate = futureDates.get(0);
			actualNextDueAmount = futurePayments.get(actualNextDueDate.toString());

			// 9️⃣ Check if it's too early to process the actual next due date
			if (actualNextDueDate.isAfter(todayDate)) {
				log.info("🕒 Today ({}) is too early for actual next due date ({}) for enrollment ID: {}", todayDate,
						actualNextDueDate, enrollmentId);
				Map<String, Object> preview = new LinkedHashMap<>();
				preview.put("message", "Today is too early. Next due date not reached.");
				preview.put("today", todayDate);
				preview.put("currentDuesOn", currentDuesOn);
				preview.put("nextDueDate", actualNextDueDate);
				preview.put("pendingAmount", pendingAmount);
				return ResponseBuilder.success(preview, ApiResponse.TOO_EARLY_FOR_NEXT_DUE, HttpStatus.OK);
			}

			// 🔟 Validate the next due amount
			if (actualNextDueAmount == null) {
				log.warn("❌ No amount found for actual next due date: {}", actualNextDueDate);
				return ResponseBuilder.error("Amount not found for due date: " + actualNextDueDate,
						ApiResponse.INSTALLMENT_INFO_NOT_FOUND, HttpStatus.EXPECTATION_FAILED);
			}

			// List<PaymentLedger> paymentsLedgers =
			// ledgerRepository.findByEnrollment_Id(enrollmentId);
			//
			// // Filter and update only eligible ledgers
			// List<PaymentLedger> updatedLedgers = paymentsLedgers.stream()
			// .filter(ledger -> (ledger.getEntryType() ==
			// PaymentLedger.PaymentEntryType.ADVANCE
			// || ledger.getEntryType() == PaymentLedger.PaymentEntryType.DISCOUNT)
			// && (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
			// || ledger.getEntryStatus() ==
			// PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
			// .peek(ledger -> {
			// ledger.setRemainingAmount(0.0);
			// ledger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
			// }).collect(Collectors.toList());
			//
			// // Save only modified entries
			// ledgerRepository.saveAll(updatedLedgers);

			// 🔁 Handle past due ledger with discount
			Long discountAmount = enrollmentDto.getDiscountAmount();
			Boolean useForFuture = paymentDetailsDto.getUseForFuture();
			Long currentDiscountAmount = Optional.ofNullable(savedEnrollment.getDiscountAmount()).orElse(0L);

			if (currentDuesOn != null && currentDuesAmount != null) {
				log.info("📗 Creating ledger entry for previous due date: {}", currentDuesOn);

				Long discountToApply = 0L;
				if (discountAmount != null && discountAmount > 0) {
					log.info("📘 Applying discount ₹{} to past due ledger", discountAmount);
					discountToApply = discountAmount;
				}

				Long remainingAmount = currentDuesAmount - discountToApply;
				if (remainingAmount < 0) {
					log.warn("⚠️ Discount {} exceeds due amount {}. Setting remaining to 0.", discountToApply,
							currentDuesAmount);
					remainingAmount = 0L;
				}

				InstallmentInfo currentInstallment = new InstallmentInfo(1, currentDuesAmount, currentDuesOn);
				PaymentLedger pastDueLedger = ledgerHelper.createPastDueLedger(courseDto.getAcademyId(),
						courseDto.getId(), savedEnrollment, currentInstallment);
				pastDueLedger.setRemainingAmount(remainingAmount.doubleValue());
				pastDueLedger.setEntryStatus(PaymentLedger.PaymentEntryStatus.PENDING);
				ledgerRepository.save(pastDueLedger);

				if (discountToApply > 0) {
					PaymentLedger discountLedger = ledgerHelper.createDiscountLedger(courseDto.getAcademyId(),
							courseDto.getId(), savedEnrollment, discountToApply, currentDuesOn);
					discountLedger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
					ledgerRepository.save(discountLedger);

					if (useForFuture != null && !useForFuture) {
						log.info("📘 Resetting discount amount as useForFuture is false");
						savedEnrollment.setDiscountAmount(0L);
					}
				}
			}

			// 🔁 Update enrollment with new due
			savedEnrollment.setDuesOn(actualNextDueDate);
			if (useForFuture) {
				savedEnrollment.setFinalDueAmount(actualNextDueAmount + currentDiscountAmount);
			} else {
				savedEnrollment.setFinalDueAmount(actualNextDueAmount);
			}
			traineeCourseEnrollmentRepo.save(savedEnrollment);

			// 1️⃣3️⃣ Build response
			Map<String, Object> responseData = new LinkedHashMap<>();
			responseData.put("message", "Ledger for " + currentDuesOn + " added. Dues updated.");
			responseData.put("ledgerDate", currentDuesOn);
			responseData.put("ledgerAmount", currentDuesAmount);
			responseData.put("newDueDate", actualNextDueDate);
			responseData.put("newDueAmount", actualNextDueAmount);
			responseData.put("pendingAmount", pendingAmount);
			responseData.put("totalFuturePayments", futurePayments.size());
			responseData.put("discountProcessed", discountAmount != null && discountAmount > 0);
			responseData.put("discountReset",
					useForFuture != null && !useForFuture && discountAmount != null && discountAmount > 0);

			log.info("✅ Dues successfully updated for enrollment ID: {} - Next due: {} for amount: {}", enrollmentId,
					actualNextDueDate, actualNextDueAmount);
			return ResponseBuilder.success(responseData, ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.CREATED);

		} catch (Exception ex) {
			log.error("❌ Error in updateDues for enrollment {}: {}", enrollmentId, ex.getMessage(), ex);
			return ResponseBuilder.error("Unexpected error while updating dues: " + ex.getMessage(),
					ApiResponse.DUES_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse generateDuesAndValidate(String academyId, String courseId, PaymentSchedule paymentSchedule,
			Long amount, LocalDate joiningDate, LocalDate dueDate, Long absoluteDiscount, String enrollmentId) {

		try {
			long discount = absoluteDiscount != null ? absoluteDiscount : 0L;

			if (enrollmentId != null && !enrollmentId.isBlank()) {
				// ✅ EDIT FLOW: Only enrollmentId + discount needed
				ServiceResponse serviceResponse = getPaymentDetails(enrollmentId, PaymentCategory.COURSE_FEE);

				if (serviceResponse.getBody() instanceof PaymentDetailsDto) {
					PaymentDetailsDto paymentDetailsDto = (PaymentDetailsDto) serviceResponse.getBody();

					long pendingAmount = paymentDetailsDto.getPendingAmount() != null
							? paymentDetailsDto.getPendingAmount()
							: 0L;
					long totalPayable = paymentDetailsDto.getTotalPayableAmount() != null
							? paymentDetailsDto.getTotalPayableAmount()
							: 0L;

					// ✅ Disallow negative discount ever
					if (discount < 0) {
						return ResponseBuilder.error("Discount cannot be negative",
								ErrorCodes.RESOURCE_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
					}

					// ✅ Zero payable: allow only zero discount
					if (totalPayable <= 0) {
						if (discount > 0) {
							return ResponseBuilder.error(
									"Cannot apply discount (" + discount + ") when total payable amount is zero",
									ErrorCodes.RESOURCE_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
						}
						// If discount is zero too → valid, just return info
						Map<String, Object> response = new HashMap<>();
						response.put("enrollmentId", enrollmentId);
						response.put("originalTotal", totalPayable);
						response.put("originalPending", pendingAmount);
						response.put("discount", discount);
						response.put("newPending", 0L);
						response.put("valid", true);
						response.put("message", "No payable amount or discount to apply. Enrollment already settled.");
						return ResponseBuilder.success(response, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);
					}

					// ✅ If payable > 0, discount must be strictly less than payable
					if (discount >= totalPayable) {
						return ResponseBuilder.error(
								"Discount (" + discount + ") cannot be greater than or equal to total payable amount ("
										+ totalPayable + ")",
								ErrorCodes.RESOURCE_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
					}

					long newPending = totalPayable - discount;

					Map<String, Object> response = new HashMap<>();
					response.put("enrollmentId", enrollmentId);
					response.put("originalTotal", totalPayable);
					response.put("originalPending", pendingAmount);
					response.put("discount", discount);
					response.put("newPending", newPending);
					response.put("valid", true);

					return ResponseBuilder.success(response, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);
				} else {
					return ResponseBuilder.error("Unable to fetch payment details for enrollment",
							ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
				}

			} else {
				// ✅ NEW FLOW: Full validation
				if (academyId == null || academyId.isBlank()) {
					return ResponseBuilder.error("Academy ID is required", ErrorCodes.RESOURCE_VALIDATION_FAILED,
							HttpStatus.BAD_REQUEST);
				}

				if (courseId == null || courseId.isBlank()) {
					return ResponseBuilder.error("Course ID is required", ErrorCodes.RESOURCE_VALIDATION_FAILED,
							HttpStatus.BAD_REQUEST);
				}

				if (paymentSchedule == null) {
					return ResponseBuilder.error("Payment schedule is required", ErrorCodes.RESOURCE_VALIDATION_FAILED,
							HttpStatus.BAD_REQUEST);
				}

				if (amount == null || amount <= 0) {
					return ResponseBuilder.error("Amount must be greater than zero",
							ErrorCodes.RESOURCE_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
				}

				if (discount < 0) {
					return ResponseBuilder.error("Discount cannot be negative", ErrorCodes.RESOURCE_VALIDATION_FAILED,
							HttpStatus.BAD_REQUEST);
				}

				if (joiningDate == null) {
					return ResponseBuilder.error("Joining date is required", ErrorCodes.RESOURCE_VALIDATION_FAILED,
							HttpStatus.BAD_REQUEST);
				}

				if (dueDate != null && dueDate.isBefore(joiningDate)) {
					return ResponseBuilder.error("Due date cannot be before joining date",
							ErrorCodes.RESOURCE_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
				}

				CourseDto courseDto = courseService.getCourse(academyId, courseId);
				if (courseDto == null) {
					return ResponseBuilder.error("Course not found", ErrorCodes.RESOURCE_NOT_FOUND,
							HttpStatus.NOT_FOUND);
				}

				TraineeCourseEnrollmentDto enrollmentDto = new TraineeCourseEnrollmentDto();
				enrollmentDto.setAmount(amount);
				enrollmentDto.setJoiningDate(joiningDate);
				enrollmentDto.setDueDate(dueDate);
				enrollmentDto.setPaymentSchedule(paymentSchedule);

				List<InstallmentInfo> installments = installmentUtil.calculateInstallments(enrollmentDto, courseDto,
						paymentSchedule);
				if (installments == null || installments.isEmpty()) {
					return ResponseBuilder.error("No installments generated", ErrorCodes.UNEXPECTED_FAILURE,
							HttpStatus.INTERNAL_SERVER_ERROR);
				}

				long installmentsTotal = installments.stream().mapToLong(InstallmentInfo::getAmount).sum();

				if (installmentsTotal <= 0) {
					return ResponseBuilder.error("Installments sum must be greater than zero",
							ErrorCodes.RESOURCE_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
				}

				if (discount >= installmentsTotal) {
					return ResponseBuilder.error(
							"Discount (" + discount + ") cannot be greater than or equal to total installments sum ("
									+ installmentsTotal + ")",
							ErrorCodes.RESOURCE_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
				}

				long netPayable = installmentsTotal - discount;

				Map<String, Object> response = new HashMap<>();
				response.put("academyId", academyId);
				response.put("courseId", courseId);
				response.put("paymentSchedule", paymentSchedule);
				response.put("joiningDate", joiningDate);
				response.put("dueDate", dueDate);
				response.put("amount", amount);
				response.put("discount", discount);
				response.put("netPayable", netPayable);
				response.put("totalInstallmentAmount", installmentsTotal);
				response.put("valid", true);

				return ResponseBuilder.success(response, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);
			}

		} catch (Exception ex) {
			return ResponseBuilder.error("Unexpected error while generating dues: " + ex.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse markPaymentsPendingOnly(String enrollmentId) {
		try {
			List<Payment> payments = paymentRepo.findByTraineeCourseEnrollment_Id(enrollmentId);

			List<Payment> successfulCoursePayments = payments.stream()
					.filter(p -> p.getPaymentCategory() == PaymentCategory.COURSE_FEE)
					.filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS).collect(Collectors.toList());

			for (Payment payment : successfulCoursePayments) {
				payment.setPaymentStatus(PaymentStatus.PENDING);
			}

			paymentRepo.saveAll(successfulCoursePayments);

			log.info("✅ Marked {} course payments as PENDING for enrollmentId: {}", successfulCoursePayments.size(),
					enrollmentId);

			List<String> updatedIds = successfulCoursePayments.stream().map(Payment::getId)
					.collect(Collectors.toList());

			Map<String, Object> response = new HashMap<>();
			response.put("markedPaymentIds", updatedIds);
			response.put("count", updatedIds.size());

			return ResponseBuilder.success(response, ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("❌ Error in markPaymentsPendingOnly", e);
			return ResponseBuilder.error("Failed to mark payments pending", ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional
	public ServiceResponse updatePaymentsToSuccessOnly(List<String> paymentIds, String enrollmentId) {
		try {
			if (paymentIds == null || paymentIds.isEmpty()) {
				return ResponseBuilder.error("No payment IDs provided to update", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.BAD_REQUEST);
			}

			// 1️⃣ Fetch all payments by enrollment
			List<Payment> payments = paymentRepo.findByTraineeCourseEnrollment_Id(enrollmentId);

			// 2️⃣ Filter only COURSE_FEE + PENDING + matching paymentIds
			List<Payment> selectedPayments = payments.stream()
					.filter(p -> p.getPaymentCategory() == PaymentCategory.COURSE_FEE)
					.filter(p -> p.getPaymentStatus() == PaymentStatus.PENDING)
					.filter(p -> paymentIds.contains(p.getId()))
					.sorted(Comparator
							.comparing(Payment::getPaymentInstallmentDate, Comparator.nullsLast(String::compareTo))
							.thenComparing(Payment::getCreatedAt, Comparator.nullsLast(Timestamp::compareTo)))
					.collect(Collectors.toList());

			List<String> updatedPaymentIds = new ArrayList<>();

			// 3️⃣ Update each payment in sorted order
			for (Payment payment : selectedPayments) {
				UpdatePaymentDto dto = new UpdatePaymentDto();
				dto.setPaymentStatus(PaymentStatus.SUCCESS);
				dto.setPaymentMode(payment.getPaymentMode());
				dto.setPaymentCategory(payment.getPaymentCategory());
				dto.setExtraArgs(payment.getExtraArgs());
				dto.setExternalTransactionId(payment.getExternalTransactionId());
				dto.setTransactionTime(payment.getTransactionTime());

				ServiceResponse serviceResponse = updatePayment(payment.getId(), enrollmentId, dto, true);

				if (serviceResponse.getHttpStatus().is2xxSuccessful()) { // ✅ Check for successful update
					updatedPaymentIds.add(payment.getId());
				} else {
					log.warn("⚠️ Failed to update payment ID: {}, Reason: {}", payment.getId(),
							serviceResponse.getMessage());
					return serviceResponse;
				}
			}

			// 4️⃣ Prepare response
			Map<String, Object> response = new HashMap<>();
			response.put("updatedPaymentIds", updatedPaymentIds);
			response.put("count", updatedPaymentIds.size());

			return ResponseBuilder.success(response, ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("❌ Error in updatePaymentsToSuccessOnly", e);
			return ResponseBuilder.error("Failed to update payments", ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional
	public ServiceResponse generateDuesForEnrollment(String enrollmentId) {
		try {
			Optional<TraineeCourseEnrollment> optionalEnrollment = traineeCourseEnrollmentRepo.findById(enrollmentId);

			if (optionalEnrollment.isEmpty()) {
				return ResponseBuilder.error("Enrollment not found", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			TraineeCourseEnrollment enrollment = optionalEnrollment.get();
			String traineeName = enrollment.getTraineeUserProfile().getDisplayName();
			String courseName = enrollment.getCourse().getTitle();

			// Check if ledgers already exist for this enrollment
			List<PaymentLedger> existingLedgers = ledgerRepository.findByEnrollment_Id(enrollmentId);
			if (!existingLedgers.isEmpty()) {
				return ResponseBuilder.error("Dues already exist for this enrollment", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.CONFLICT);
			}

			// Course & Enrollment DTOs
			CourseDto courseDto = courseService.getCourse(enrollment.getAcademy().getId(),
					enrollment.getCourse().getId());
			TraineeCourseEnrollmentDto enrollmentDto = convertToDto(enrollment);

			// Calculate installments and due dates
			Pair<List<InstallmentInfo>, List<LocalDate>> result = installmentUtil
					.calculateInstallmentsAndDueDates(enrollmentDto, courseDto, enrollment.getPaymentSchedule());

			List<InstallmentInfo> allInstallments = result.getLeft();
			List<LocalDate> dueDates = result.getRight();
			LocalDate lastDue = installmentUtil.getLastPassedDueDate(dueDates, LocalDate.now());

			List<InstallmentInfo> installmentsBeforeLastDue = new ArrayList<>();
			InstallmentInfo lastDueInstallment = null;

			if (lastDue != null) {
				for (InstallmentInfo inst : allInstallments) {
					if (inst.getDueDate().isBefore(lastDue)) {
						installmentsBeforeLastDue.add(inst);
					} else if (inst.getDueDate().equals(lastDue)) {
						lastDueInstallment = inst;
					}
				}
			}

			// Update enrollment
			if (lastDueInstallment != null) {
				enrollment.setDuesOn(lastDueInstallment.getDueDate());
				enrollment.setFinalDueAmount(lastDueInstallment.getAmount());
			} else {
				enrollment.setDuesOn(enrollment.getDueDate());
				enrollment.setFinalDueAmount(enrollment.getAmount());
			}
			TraineeCourseEnrollment updatedEnrollment = traineeCourseEnrollmentRepo.save(enrollment);

			List<PaymentLedger> ledgersToSave = new ArrayList<>();

			// Create past due ledger entries
			for (InstallmentInfo pastInst : installmentsBeforeLastDue) {
				PaymentLedger pastDueLedger = ledgerHelper.createPastDueLedger(enrollment.getAcademy().getId(),
						enrollment.getCourse().getId(), updatedEnrollment, pastInst);
				ledgersToSave.add(pastDueLedger);
			}

			// REGISTRATION_FEE logic
			List<Payment> payments = paymentRepo.findByTraineeCourseEnrollment_Id(enrollmentId);
			Optional<Payment> successfulRegPaymentOpt = payments.stream()
					.filter(p -> p.getPaymentCategory() == PaymentCategory.REGISTRATION_FEE)
					.filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS).findFirst();

			LocalDate effectiveDate = enrollment.getJoiningDate();
			PaymentCategory category = PaymentCategory.REGISTRATION_FEE;

			if (successfulRegPaymentOpt.isPresent()) {
				Payment payment = successfulRegPaymentOpt.get();
				long paidAmount = payment.getAmount();
				String createdBy = payment.getPaymentInitiatedByUserProfile().getId();

				// Debit
				PaymentLedger debitEntry = ledgerHelper.createRegistrationFeeLedger(enrollment.getAcademy().getId(),
						enrollment.getCourse().getId(), updatedEnrollment, courseDto, effectiveDate, category,
						paidAmount, 0L, "Registration fee paid", createdBy, PaymentLedger.PaymentEntryStatus.SETTLED);
				ledgersToSave.add(debitEntry);

				// Credit
				PaymentLedger creditEntry = ledgerHelper.createRegistrationCreditFeeLedger(
						enrollment.getAcademy().getId(), enrollment.getCourse().getId(), updatedEnrollment, courseDto,
						effectiveDate, category, paidAmount, 0L, "Registration credit applied", createdBy,
						PaymentLedger.PaymentEntryStatus.SETTLED);
				creditEntry.setPayment(payment);
				ledgersToSave.add(creditEntry);

			} else {
				// No successful registration payment
				long regFeeAmount = courseDto.getRegistrationFee() != null ? courseDto.getRegistrationFee() : 0L;
				PaymentLedger.PaymentEntryStatus status = regFeeAmount > 0 ? PaymentLedger.PaymentEntryStatus.PENDING
						: PaymentLedger.PaymentEntryStatus.SETTLED;

				PaymentLedger debitEntry = ledgerHelper.createRegistrationFeeLedger(enrollment.getAcademy().getId(),
						enrollment.getCourse().getId(), updatedEnrollment, courseDto, effectiveDate, category,
						regFeeAmount, regFeeAmount, "Registration fee due", null, status);
				ledgersToSave.add(debitEntry);
			}

			// Save all ledger entries
			ledgerRepository.saveAll(ledgersToSave);

			log.info("✅ Dues generated successfully for enrollment {}", enrollmentId);

			Map<String, Object> response = new HashMap<>();
			response.put("enrollmentId", enrollmentId);
			response.put("trainee", traineeName);
			response.put("course", courseName);
			response.put("status", "Dues Generated");

			return ResponseBuilder.success(response, ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("❌ Failed to generate dues for enrollment {}", enrollmentId, e);
			return ResponseBuilder.error("Failed to generate dues: " + e.getMessage(), ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse generateDuesForAcademy(String academyId) {
		log.info("🚀 Starting dues generation for academy ID: {}", academyId);
		try {
			// Step 1: Fetch enrollments
			log.debug("📋 Fetching enrollments for academy ID: {}", academyId);
			List<TraineeCourseEnrollment> enrollments = traineeCourseEnrollmentRepo.findByAcademy_Id(academyId);

			if (enrollments.isEmpty()) {
				log.error("❌ No enrollments found for academy ID: {}", academyId);
				return ResponseBuilder.error("No enrollments found for this academy", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}
			log.info("📊 Found {} enrollments for academy ID: {}", enrollments.size(), academyId);

			// Step 2: Process enrollments
			log.debug("⚙️ Processing enrollments for dues generation");
			List<TraineeCourseEnrollment> updatedEnrollments = new ArrayList<>();

			for (TraineeCourseEnrollment enrollment : enrollments) {
				log.debug("🔄 Processing enrollment ID: {} for trainee: {}", enrollment.getId(),
						enrollment.getTraineeUserProfile().getDisplayName());

				ServiceResponse response = generateDuesForEnrollment(enrollment.getId());

				if (response.getHttpStatus() == HttpStatus.OK) {
					updatedEnrollments.add(enrollment);
					log.debug("✅ Successfully processed enrollment ID: {}", enrollment.getId());
				} else {
					log.warn("⚠️ Failed to process enrollment ID: {}, reason: {}", enrollment.getId(),
							response.getMessage());
				}
			}

			if (updatedEnrollments.isEmpty()) {
				log.error("❌ No dues were updated for academy ID: {}", academyId);
				return ResponseBuilder.error("No dues were updated", ErrorCodes.UNEXPECTED_FAILURE,
						HttpStatus.CONFLICT);
			}
			log.info("✅ Successfully processed {} out of {} enrollments", updatedEnrollments.size(),
					enrollments.size());

			// Step 3: Generate and save Excel
			log.debug("📊 Starting Excel generation and file storage");
			String academyName = getAcademyName(academyId);
			byte[] excelBytes = generateDuesExcel(updatedEnrollments);
			String fileName = createFileName("dues_updated", academyName, academyId);
			// String filePath = saveExcelToFile(excelBytes, fileName);

			// log.info("💾 Excel file generated and saved: {}", filePath);

			String bucketFolder = "data-mirgration/";

			storageService.upload(paymentsBucket, bucketFolder + fileName, excelBytes,
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

			String fileUrl = paymentsBaseUrl.concat(bucketFolder + fileName);
			// Step 4: Prepare response
			Map<String, Object> responseData = createFileResponse(updatedEnrollments.size(), academyId, academyName,
					excelBytes, fileName, "done", fileUrl);

			log.info("🎉 Dues generation completed successfully for academy ID: {} with {} updates", academyId,
					updatedEnrollments.size());
			return ResponseBuilder.success(responseData, ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("💥 Failed to generate dues for academy ID: {}", academyId, e);
			return ResponseBuilder.error("Failed to generate dues: " + e.getMessage(), ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse markPaymentsPendingByAcademy(String academyId) {
		log.info("🚀 Starting to mark payments as PENDING for academy ID: {}", academyId);
		try {
			// Step 1: Fetch enrollments
			log.debug("📋 Fetching enrollments for academy ID: {}", academyId);
			List<TraineeCourseEnrollment> enrollments = traineeCourseEnrollmentRepo.findByAcademy_Id(academyId);

			if (enrollments.isEmpty()) {
				log.error("❌ No enrollments found for academy ID: {}", academyId);
				return ResponseBuilder.error("No enrollments found for this academy", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}
			log.info("📊 Found {} enrollments for academy ID: {}", enrollments.size(), academyId);

			// Step 2: Collect payments to update
			log.debug("🔍 Searching for successful course fee payments to mark as pending");
			List<Payment> paymentsToUpdate = new ArrayList<>();

			for (TraineeCourseEnrollment enrollment : enrollments) {
				log.debug("🔄 Processing enrollment ID: {} for trainee: {}", enrollment.getId(),
						enrollment.getTraineeUserProfile().getDisplayName());

				List<Payment> payments = paymentRepo.findByTraineeCourseEnrollment_Id(enrollment.getId());
				log.debug("💰 Found {} total payments for enrollment ID: {}", payments.size(), enrollment.getId());

				List<Payment> successfulCoursePayments = payments.stream()
						.filter(p -> p.getPaymentCategory() == PaymentCategory.COURSE_FEE)
						.filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS).collect(Collectors.toList());

				log.debug("✅ Found {} successful course fee payments for enrollment ID: {}",
						successfulCoursePayments.size(), enrollment.getId());

				successfulCoursePayments.forEach(p -> p.setPaymentStatus(PaymentStatus.PENDING));
				paymentsToUpdate.addAll(successfulCoursePayments);
			}

			if (paymentsToUpdate.isEmpty()) {
				log.warn("❌ No course payments found to mark as PENDING for academy ID: {}", academyId);
				return ResponseBuilder.error("No course payments marked as PENDING", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.CONFLICT);
			}

			// Step 3: Save updated payments
			log.debug("💾 Saving {} payments with PENDING status", paymentsToUpdate.size());
			paymentRepo.saveAll(paymentsToUpdate);
			log.info("✅ Marked {} course payments as PENDING for academy ID: {}", paymentsToUpdate.size(), academyId);

			// Step 4: Generate and save Excel
			log.debug("📊 Starting Excel generation for pending payments");
			String academyName = getAcademyName(academyId);
			byte[] csvBytes = generatePendingPaymentsCsv(paymentsToUpdate);
			String fileName = createCsvFileName("pending_course_payments", academyName, academyId);
			// String filePath = saveCsvToFile(csvBytes, fileName);

			String bucketFolder = "data-mirgration/";

			storageService.upload(paymentsBucket, bucketFolder + fileName, csvBytes,
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

			String fileUrl = paymentsBaseUrl.concat(bucketFolder + fileName);

			// log.info("💾 Excel file generated and saved: {}", filePath);

			// Step 5: Prepare response
			Map<String, Object> response = createPaymentResponse(paymentsToUpdate, academyId, academyName, csvBytes,
					fileName, "done", fileUrl);

			log.info("🎉 Successfully marked {} payments as PENDING for academy ID: {}", paymentsToUpdate.size(),
					academyId);
			return ResponseBuilder.success(response, ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("💥 Error while marking payments PENDING for academy ID: {}", academyId, e);
			return ResponseBuilder.error("Failed to mark payments pending: " + e.getMessage(),
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String createCsvFileName(String operation, String academyName, String academyId) {
		String sanitizedAcademyName = sanitizeFileName(academyName);
		String timestamp = getCurrentTimestamp();
		String fileName = String.format("%s_%s_%s_%s.csv", operation, sanitizedAcademyName,
				academyId != null ? academyId : "unknown", timestamp);
		log.debug("📝 Created CSV filename: {}", fileName);
		return fileName;
	}

//	private String saveCsvToFile(byte[] csvBytes, String fileName) throws IOException {
//		log.debug("💾 Starting CSV file save operation for: {}", fileName);
//
//		// Ensure the directory exists
//		Path directoryPath = Paths.get(excelFilePath); // Reuse the same base path
//		if (!Files.exists(directoryPath)) {
//			log.info("📁 Creating directory: {}", directoryPath);
//			Files.createDirectories(directoryPath);
//			log.info("✅ Directory created successfully: {}", directoryPath);
//		}
//
//		// Create full file path
//		Path filePath = directoryPath.resolve(fileName);
//
//		// Write file
//		log.debug("📝 Writing {} bytes to CSV file: {}", csvBytes.length, filePath);
//		Files.write(filePath, csvBytes);
//		log.info("✅ CSV file saved successfully: {} ({} bytes)", filePath, csvBytes.length);
//
//		return filePath.toString();
//	}

	private byte[] generatePendingPaymentsCsv(List<Payment> payments) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out))) {
			// Header
			String[] header = { "Payment ID", "Trainee Name", "Enrollment ID", "Amount", "Payment Date", "Status" };
			writer.writeNext(header);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

			for (Payment payment : payments) {
				String[] row = { payment.getId(),
						payment.getTraineeCourseEnrollment().getTraineeUserProfile().getDisplayName(),
						payment.getTraineeCourseEnrollment().getId(), String.valueOf(payment.getAmount()),
						payment.getCreatedAt().toLocalDateTime().format(formatter),
						payment.getPaymentStatus().toString() };
				writer.writeNext(row);
			}
		}

		return out.toByteArray();
	}

	private List<String> extractPaymentIdsFromCsv(MultipartFile csvFile) {
		log.debug("📄 Starting CSV extraction from file: {}", csvFile.getOriginalFilename());

		List<String> paymentIds = new ArrayList<>();

		try (InputStreamReader streamReader = new InputStreamReader(csvFile.getInputStream());
				CSVReader csvReader = new CSVReader(streamReader)) {
			// Read header
			String[] header = csvReader.readNext();
			if (header == null) {
				log.warn("⚠️ CSV file is empty: {}", csvFile.getOriginalFilename());
				return List.of();
			}

			// Locate the index of "Payment ID" column
			int paymentIdIndex = -1;
			for (int i = 0; i < header.length; i++) {
				if ("Payment ID".equalsIgnoreCase(header[i].trim())) {
					paymentIdIndex = i;
					break;
				}
			}

			if (paymentIdIndex == -1) {
				log.error("❌ 'Payment ID' column not found in CSV header: {}", Arrays.toString(header));
				return List.of();
			}

			// Read each row and extract the payment ID
			String[] row;
			while ((row = csvReader.readNext()) != null) {
				if (paymentIdIndex < row.length) {
					String paymentId = row[paymentIdIndex].trim();
					if (!paymentId.isEmpty()) {
						paymentIds.add(paymentId);
					}
				}
			}

			List<String> uniqueIds = paymentIds.stream().distinct().toList();
			log.debug("📊 Extracted {} unique Payment IDs from CSV", uniqueIds.size());
			return uniqueIds;

		} catch (IOException | CsvValidationException e) {
			log.error("💥 Failed to parse CSV file: {}", csvFile.getOriginalFilename(), e);
			return List.of();
		}
	}

	@Override
	@Transactional
	public ServiceResponse updatePaymentsToSuccessOnlyFromCsv(MultipartFile csvFile) {
		log.info("🚀 Starting CSV-based payment update to SUCCESS status");
		try {
			// Step 1: Extract payment IDs from CSV
			log.debug("📄 Extracting payment IDs from uploaded CSV file: {}", csvFile.getOriginalFilename());
			List<String> paymentIds = extractPaymentIdsFromCsv(csvFile);

			if (paymentIds.isEmpty()) {
				log.error("❌ CSV file contains no payment IDs");
				return ResponseBuilder.error("CSV contains no payment IDs", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.BAD_REQUEST);
			}
			log.info("📊 Extracted {} payment IDs from CSV", paymentIds.size());

			// Step 2: Fetch payments by IDs
			log.debug("🔍 Fetching payments from database using extracted IDs");
			List<Payment> payments = paymentRepo.findAllById(paymentIds);
			log.info("💰 Found {} payments in database out of {} requested IDs", payments.size(), paymentIds.size());

			if (payments.size() != paymentIds.size()) {
				log.warn("⚠️ Some payment IDs not found: requested {}, found {}", paymentIds.size(), payments.size());
			}

			// Step 3: Filter eligible payments
			log.debug("🔍 Filtering for eligible COURSE_FEE + PENDING payments");
			List<Payment> eligiblePayments = payments.stream().filter(p -> {
				boolean eligible = p.getPaymentCategory() == PaymentCategory.COURSE_FEE
						&& p.getPaymentStatus() == PaymentStatus.PENDING;
				if (!eligible) {
					log.debug("⏭️ Skipping payment ID: {} (Category: {}, Status: {})", p.getId(),
							p.getPaymentCategory(), p.getPaymentStatus());
				}
				return eligible;
			}).sorted(Comparator.comparing(Payment::getPaymentInstallmentDate, Comparator.nullsLast(String::compareTo))
					.thenComparing(Payment::getCreatedAt, Comparator.nullsLast(Timestamp::compareTo)))
					.collect(Collectors.toList());

			if (eligiblePayments.isEmpty()) {
				log.error("❌ No valid pending COURSE_FEE payments found in uploaded CSV");
				return ResponseBuilder.error("No valid pending COURSE_FEE payments found in uploaded CSV",
						ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
			}
			log.info("✅ Found {} eligible payments to update", eligiblePayments.size());

			// Step 4: Update payments to SUCCESS
			log.debug("⚙️ Starting payment updates to SUCCESS status");
			List<String> updatedPaymentIds = new ArrayList<>();
			String academyId = null;
			String academyName = null;

			for (Payment payment : eligiblePayments) {
				log.debug("🔄 Processing payment ID: {} for trainee: {}", payment.getId(),
						payment.getTraineeCourseEnrollment().getTraineeUserProfile().getDisplayName());

				// Get academy info from first payment if not already set
				if (academyId == null && payment.getTraineeCourseEnrollment() != null) {
					academyId = payment.getTraineeCourseEnrollment().getAcademy().getId();
					academyName = getAcademyName(academyId);
					log.debug("🏫 Identified academy: {} (ID: {})", academyName, academyId);
				}

				UpdatePaymentDto dto = createUpdatePaymentDto(payment);
				ServiceResponse serviceResponse = updatePayment(payment.getId(),
						payment.getTraineeCourseEnrollment().getId(), dto, true);

				if (serviceResponse.getHttpStatus().is2xxSuccessful()) {
					updatedPaymentIds.add(payment.getId());
					log.debug("✅ Successfully updated payment ID: {}", payment.getId());
				} else {
					log.error("❌ Failed to update payment ID: {}, Reason: {}", payment.getId(),
							serviceResponse.getMessage());
					return serviceResponse;
				}
			}
			log.info("🎉 Successfully updated {} payments to SUCCESS status", updatedPaymentIds.size());

			// Step 5: Generate and save Excel
			log.debug("📊 Generating Excel report for updated payments");
			String sanitizedAcademyName = academyName != null ? sanitizeFileName(academyName) : "Unknown_Academy";
			byte[] excelBytes = generatePendingPaymentsExcel(eligiblePayments);
			String fileName = createFileName("updated_success_payments", sanitizedAcademyName, academyId);
			// String filePath = saveExcelToFile(excelBytes, fileName);

			// log.info("💾 Excel report generated and saved: {}", filePath);

			// Step 6: Prepare response
			Map<String, Object> response = createCsvUpdateResponse(updatedPaymentIds, academyId, academyName,
					excelBytes, fileName, "done");

			log.info("🏁 CSV-based payment update completed successfully: {} payments updated",
					updatedPaymentIds.size());
			return ResponseBuilder.success(response, ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("💥 Error in updatePaymentsToSuccessOnlyFromCsv", e);
			return ResponseBuilder.error("Failed to update payments from CSV", ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Optimized helper methods with logging

//	private String saveExcelToFile(byte[] excelBytes, String fileName) throws IOException {
//		log.debug("💾 Starting file save operation for: {}", fileName);
//
//		// Ensure the directory exists
//		Path directoryPath = Paths.get(excelFilePath);
//		if (!Files.exists(directoryPath)) {
//			log.info("📁 Creating directory: {}", directoryPath);
//			Files.createDirectories(directoryPath);
//			log.info("✅ Directory created successfully: {}", directoryPath);
//		}
//
//		// Create full file path
//		Path filePath = directoryPath.resolve(fileName);
//
//		// Write file
//		log.debug("📝 Writing {} bytes to file: {}", excelBytes.length, filePath);
//		// Files.write(filePath, excelBytes);
//		log.info("✅ Excel file saved successfully: {} ({} bytes)", filePath, excelBytes.length);
//
//		return filePath.toString();
//	}

	private String getAcademyName(String academyId) {
		log.debug("🏫 Fetching academy name for ID: {}", academyId);
		try {
			Optional<Academy> academy = academyRepo.findById(academyId);
			String academyName = academy.map(Academy::getName).orElse("Unknown_Academy");
			log.debug("🏫 Academy name resolved: {} for ID: {}", academyName, academyId);
			return academyName;
		} catch (Exception e) {
			log.warn("⚠️ Failed to fetch academy name for ID: {}, using default", academyId, e);
			return "Unknown_Academy";
		}
	}

	private String sanitizeFileName(String fileName) {
		if (fileName == null || fileName.trim().isEmpty()) {
			log.debug("🧹 Empty filename provided, using 'Unknown'");
			return "Unknown";
		}

		String sanitized = fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("_{2,}", "_")
				.replaceAll("^_|_$", "");

		log.debug("🧹 Sanitized filename: '{}' -> '{}'", fileName, sanitized);
		return sanitized;
	}

	private String getCurrentTimestamp() {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		log.debug("🕐 Generated timestamp: {}", timestamp);
		return timestamp;
	}

	private String createFileName(String operation, String academyName, String academyId) {
		String sanitizedAcademyName = sanitizeFileName(academyName);
		String timestamp = getCurrentTimestamp();
		String fileName = String.format("%s_%s_%s_%s.xlsx", operation, sanitizedAcademyName,
				academyId != null ? academyId : "unknown", timestamp);
		log.debug("📝 Created filename: {}", fileName);
		return fileName;
	}

	private UpdatePaymentDto createUpdatePaymentDto(Payment payment) {
		log.debug("🏗️ Creating UpdatePaymentDto for payment ID: {}", payment.getId());
		UpdatePaymentDto dto = new UpdatePaymentDto();
		dto.setPaymentStatus(PaymentStatus.SUCCESS);
		dto.setPaymentMode(payment.getPaymentMode());
		dto.setPaymentCategory(payment.getPaymentCategory());
		dto.setExtraArgs(payment.getExtraArgs());
		dto.setExternalTransactionId(payment.getExternalTransactionId());
		dto.setTransactionTime(payment.getTransactionTime());
		return dto;
	}

	private Map<String, Object> createFileResponse(int count, String academyId, String academyName, byte[] excelBytes,
			String fileName, String filePath, String fileUrl) {
		log.debug("🏗️ Creating file response with count: {}", count);
		Map<String, Object> response = new HashMap<>();
		response.put("updatedCount", count);
		response.put("academyId", academyId);
		response.put("academyName", academyName);
		response.put("excelBase64", Base64.getEncoder().encodeToString(excelBytes));
		response.put("fileName", fileName);
		response.put("filePath", filePath);
		response.put("fileUrl", fileUrl);
		return response;
	}

	private Map<String, Object> createPaymentResponse(List<Payment> payments, String academyId, String academyName,
			byte[] excelBytes, String fileName, String filePath, String fileUrl) {
		log.debug("🏗️ Creating payment response for {} payments", payments.size());
		Map<String, Object> response = new HashMap<>();
		response.put("academyId", academyId);
		response.put("academyName", academyName);
		response.put("count", payments.size());
		response.put("markedPaymentIds", payments.stream().map(Payment::getId).collect(Collectors.toList()));
		response.put("excelBase64", Base64.getEncoder().encodeToString(excelBytes));
		response.put("fileName", fileName);
		response.put("filePath", filePath);
		response.put("fileUrl", fileUrl);
		return response;
	}

	private Map<String, Object> createCsvUpdateResponse(List<String> updatedIds, String academyId, String academyName,
			byte[] excelBytes, String fileName, String filePath) {
		log.debug("🏗️ Creating CSV update response for {} updated payments", updatedIds.size());
		Map<String, Object> response = new HashMap<>();
		response.put("updatedPaymentIds", updatedIds);
		response.put("count", updatedIds.size());
		response.put("academyId", academyId);
		response.put("academyName", academyName);
		response.put("excelBase64", Base64.getEncoder().encodeToString(excelBytes));
		response.put("fileName", fileName);
		response.put("filePath", filePath);
		return response;
	}

	// Reuse existing Excel generation methods (they already have good structure)
	private byte[] generatePendingPaymentsExcel(List<Payment> payments) throws IOException {
		log.debug("📊 Generating Excel for {} pending payments", payments.size());
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Pending Payments");

			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("Payment ID");
			header.createCell(1).setCellValue("Enrollment ID");
			header.createCell(2).setCellValue("Trainee Name");
			header.createCell(3).setCellValue("Amount");
			header.createCell(4).setCellValue("Status");
			header.createCell(5).setCellValue("Payment Date");

			int rowNum = 1;
			for (Payment p : payments) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(p.getId());
				row.createCell(1).setCellValue(p.getTraineeCourseEnrollment().getId());
				row.createCell(2).setCellValue(p.getTraineeCourseEnrollment().getTraineeUserProfile().getDisplayName());
				row.createCell(3).setCellValue(p.getAmount());
				row.createCell(4).setCellValue("SUCCESS");
				row.createCell(5).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			byte[] excelBytes = out.toByteArray();
			log.debug("✅ Excel generated successfully: {} bytes", excelBytes.length);
			return excelBytes;
		}
	}

	private byte[] generateDuesExcel(List<TraineeCourseEnrollment> updatedEnrollments) throws IOException {
		log.debug("📊 Generating Excel for {} updated enrollments", updatedEnrollments.size());
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Updated Dues");

			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("Enrollment ID");
			header.createCell(1).setCellValue("Trainee Name");
			header.createCell(2).setCellValue("Course Name");
			header.createCell(3).setCellValue("Dues On");
			header.createCell(4).setCellValue("Final Due Amount");

			int rowNum = 1;
			for (TraineeCourseEnrollment e : updatedEnrollments) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(e.getId());
				row.createCell(1).setCellValue(e.getTraineeUserProfile().getDisplayName());
				row.createCell(2).setCellValue(e.getCourse().getTitle());
				row.createCell(3).setCellValue(e.getDuesOn() != null ? e.getDuesOn().toString() : "");
				row.createCell(4).setCellValue(e.getFinalDueAmount());
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			byte[] excelBytes = out.toByteArray();
			log.debug("✅ Excel generated successfully: {} bytes", excelBytes.length);
			return excelBytes;
		}
	}

	@Override
	@Transactional
	public ServiceResponse generateBulkDues() {
		List<String[]> alreadyExistsRecords = new ArrayList<>();
		List<String[]> successRecords = new ArrayList<>();
		String[] headers = { "Enrollment ID", "Trainee Name", "Course Name", "Status" };

		try {
			List<TraineeCourseEnrollment> allEnrollments = traineeCourseEnrollmentRepo.findAll();

			for (TraineeCourseEnrollment enrollment : allEnrollments) {
				String enrollmentId = enrollment.getId();
				String traineeName = enrollment.getTraineeUserProfile().getDisplayName();
				String courseName = enrollment.getCourse().getTitle();

				// Check if ledgers already exist for this enrollment
				List<PaymentLedger> existingLedgers = ledgerRepository.findByEnrollment_Id(enrollmentId);
				if (!existingLedgers.isEmpty()) {
					alreadyExistsRecords.add(new String[] { enrollmentId, traineeName, courseName, "Already Exists" });
					continue;
				}

				try {
					// Get course details
					CourseDto courseDto = courseService.getCourse(enrollment.getAcademy().getId(),
							enrollment.getCourse().getId());

					// Convert enrollment to DTO for installment calculations
					TraineeCourseEnrollmentDto enrollmentDto = convertToDto(enrollment);

					// Calculate installments and due dates (same as enrollment logic)
					Pair<List<InstallmentInfo>, List<LocalDate>> result = installmentUtil
							.calculateInstallmentsAndDueDates(enrollmentDto, courseDto,
									enrollment.getPaymentSchedule());

					List<InstallmentInfo> allInstallments = result.getLeft();
					List<LocalDate> dueDates = result.getRight();

					LocalDate lastDue = installmentUtil.getLastPassedDueDate(dueDates, LocalDate.now());

					List<InstallmentInfo> installmentsBeforeLastDue = new ArrayList<>();
					InstallmentInfo lastDueInstallment = null;

					if (lastDue != null) {
						for (InstallmentInfo inst : allInstallments) {
							if (inst.getDueDate().isBefore(lastDue)) {
								installmentsBeforeLastDue.add(inst);
							} else if (inst.getDueDate().equals(lastDue)) {
								lastDueInstallment = inst;
							}
						}
					}

					// Update enrollment entity with calculated values (same as enrollment logic)
					if (lastDueInstallment != null) {
						enrollment.setDuesOn(lastDueInstallment.getDueDate());
						enrollment.setFinalDueAmount(lastDueInstallment.getAmount());
					} else {
						enrollment.setDuesOn(enrollment.getDueDate());
						enrollment.setFinalDueAmount(enrollment.getAmount());
					}

					// Update the existing enrollment (instead of creating new one)
					TraineeCourseEnrollment updatedEnrollment = traineeCourseEnrollmentRepo.save(enrollment);

					// Collect all ledger entries to save in batch
					List<PaymentLedger> ledgersToSave = new ArrayList<>();

					// Create past due ledger entries for installments before last due date
					for (InstallmentInfo pastInst : installmentsBeforeLastDue) {
						PaymentLedger pastDueLedger = ledgerHelper.createPastDueLedger(enrollment.getAcademy().getId(),
								enrollment.getCourse().getId(), updatedEnrollment, pastInst);
						ledgersToSave.add(pastDueLedger);
					}

					List<Payment> payments = paymentRepo.findByTraineeCourseEnrollment_Id(enrollmentId);

					// 1️⃣ Filter successful REGISTRATION_FEE payments
					Optional<Payment> successfulRegPaymentOpt = payments.stream()
							.filter(p -> p.getPaymentCategory() == PaymentCategory.REGISTRATION_FEE)
							.filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS).findFirst();

					LocalDate effectiveDate = enrollment.getJoiningDate();

					PaymentCategory category = PaymentCategory.REGISTRATION_FEE;

					if (successfulRegPaymentOpt.isPresent()) {
						Payment payment = successfulRegPaymentOpt.get();
						long paidAmount = payment.getAmount();
						String createdBy = payment.getPaymentInitiatedByUserProfile().getId(); // Or system user ID

						// ✅ Create debit ledger (settled)
						PaymentLedger debitEntry = ledgerHelper.createRegistrationFeeLedger(
								enrollment.getAcademy().getId(), enrollment.getCourse().getId(), updatedEnrollment,
								courseDto, effectiveDate, category, paidAmount, 0L, "Registration fee paid", createdBy,
								PaymentLedger.PaymentEntryStatus.SETTLED);
						ledgersToSave.add(debitEntry);

						// ✅ Create matching credit ledger (pending)
						PaymentLedger creditEntry = ledgerHelper.createRegistrationCreditFeeLedger(
								enrollment.getAcademy().getId(), enrollment.getCourse().getId(), updatedEnrollment,
								courseDto, effectiveDate, category, paidAmount, 0L, "Registration credit applied",
								createdBy, PaymentLedger.PaymentEntryStatus.SETTLED);
						creditEntry.setPayment(payment); // Link payment to credit ledger
						ledgersToSave.add(creditEntry);

					} else {
						// ❌ No successful payment → create a pending debit ledger
						long regFeeAmount = courseDto.getRegistrationFee() != null ? courseDto.getRegistrationFee()
								: 0L;

						PaymentLedger.PaymentEntryStatus status = regFeeAmount > 0
								? PaymentLedger.PaymentEntryStatus.PENDING
								: PaymentLedger.PaymentEntryStatus.SETTLED;

						PaymentLedger debitEntry = ledgerHelper.createRegistrationFeeLedger(
								enrollment.getAcademy().getId(), enrollment.getCourse().getId(), updatedEnrollment,
								courseDto, effectiveDate, category, regFeeAmount, regFeeAmount, "Registration fee due",
								null, status);
						ledgersToSave.add(debitEntry);
					}

					// Save all ledgers at once
					ledgerRepository.saveAll(ledgersToSave);

					log.info("Successfully generated dues for enrollment {} with payment schedule {}", enrollmentId,
							enrollment.getPaymentSchedule());

					successRecords.add(new String[] { enrollmentId, traineeName, courseName, "Dues Generated" });

				} catch (Exception ex) {
					log.warn("⚠️ Failed to generate dues for enrollment ID: {}", enrollmentId, ex);
					alreadyExistsRecords
							.add(new String[] { enrollmentId, traineeName, courseName, "Failed: " + ex.getMessage() });
				}
			}

			// Write to Excel files
			String successFilePath = writeToExcel("dues-generated-success.xlsx", headers, successRecords);
			String skippedFilePath = writeToExcel("dues-skipped-already-exist.xlsx", headers, alreadyExistsRecords);

			Map<String, Object> response = new HashMap<>();
			response.put("successFilePath", successFilePath);
			response.put("skippedFilePath", skippedFilePath);

			return ResponseBuilder.success(response, ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("❌ Error in generateBulkDues", e);
			return ResponseBuilder.error("Unexpected error while generating bulk dues", ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// Helper method to convert TraineeCourseEnrollment entity to DTO
	private TraineeCourseEnrollmentDto convertToDto(TraineeCourseEnrollment enrollment) {
		TraineeCourseEnrollmentDto dto = new TraineeCourseEnrollmentDto();
		dto.setId(enrollment.getId());
		dto.setTraineeUserId(enrollment.getTraineeUserProfile().getId());
		dto.setPaymentSchedule(enrollment.getPaymentSchedule());
		dto.setAmount(enrollment.getAmount());
		dto.setJoiningDate(enrollment.getJoiningDate());
		dto.setDueDate(enrollment.getDueDate());
		dto.setDiscountAmount(enrollment.getDiscountAmount());
		dto.setUseForFuture(enrollment.getUseForFuture());
		dto.setDuesOn(enrollment.getDuesOn());
		dto.setFinalDueAmount(enrollment.getFinalDueAmount());

		// Convert UserProfile to UserProfileDto if needed
		if (enrollment.getTraineeUserProfile() != null) {
			UserProfileDto userProfileDto = new UserProfileDto();
			userProfileDto.setId(enrollment.getTraineeUserProfile().getId());
			// Set other UserProfileDto fields as needed (displayName, email, etc.)
			dto.setUserProfile(userProfileDto);
		}

		return dto;
	}

	public static String writeToExcel(String fileName, String[] headers, List<String[]> data) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Report");

		int rowIdx = 0;
		Row headerRow = sheet.createRow(rowIdx++);
		for (int i = 0; i < headers.length; i++) {
			headerRow.createCell(i).setCellValue(headers[i]);
		}

		for (String[] record : data) {
			Row row = sheet.createRow(rowIdx++);
			for (int i = 0; i < record.length; i++) {
				row.createCell(i).setCellValue(record[i]);
			}
		}

		// Adjust column size
		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
		}

		// Write to disk
		String dirPath = "bulk-dues-reports";
		Files.createDirectories(Paths.get(dirPath));
		String filePath = dirPath + "/" + fileName;
		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			workbook.write(fos);
		}
		workbook.close();
		return filePath;
	}

	// List<Payment> payments =
	// paymentRepo.findByTraineeCourseEnrollment_Id(enrollmentId);
	//
	// DateTimeFormatter fullDateTimeFormatter =
	// DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	//
	// Map<PaymentCategory, Map<String, Long>> coursePaidPayments =
	// payments.stream()
	// .filter(p -> p.getPaymentCategory() != PaymentCategory.REGISTRATION_FEE)
	// .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS)
	// .collect(Collectors.groupingBy(Payment::getPaymentCategory,
	// LinkedHashMap::new,
	// Collectors.groupingBy(
	// p -> p.getCreatedAt().toLocalDateTime().format(fullDateTimeFormatter),
	// LinkedHashMap::new, Collectors.summingLong(Payment::getAmount))));
	//
	// Map<PaymentCategory, Map<String, Long>> registationPaidPayments =
	// payments.stream()
	// .filter(p -> p.getPaymentCategory() != PaymentCategory.COURSE_FEE)
	// .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS)
	// .collect(Collectors.groupingBy(Payment::getPaymentCategory,
	// LinkedHashMap::new,
	// Collectors.groupingBy(
	// p -> p.getCreatedAt().toLocalDateTime().format(fullDateTimeFormatter),
	// LinkedHashMap::new, Collectors.summingLong(Payment::getAmount))));

	@Override
	public ServiceResponse generateDues(String enrollmentId) throws ResourceException {
		// 1️⃣ Validate enrollment exists
		if (!traineeCourseEnrollmentRepo.existsById(enrollmentId)) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Enrollment not found");
		}

		// 2️⃣ Fetch enrollment and course details
		Pair<TraineeCourseEnrollmentDto, CourseDto> courseEnrollmentDetails = courseService
				.getByEnrollmentId(enrollmentId);

		if (courseEnrollmentDetails == null || courseEnrollmentDetails.getLeft() == null
				|| courseEnrollmentDetails.getRight() == null) {
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Unable to fetch enrollment or course details.");
		}

		TraineeCourseEnrollmentDto enrollmentDto = courseEnrollmentDetails.getLeft();
		CourseDto courseDto = courseEnrollmentDetails.getRight();
		LocalDate today = LocalDate.now();

		// 3️⃣ Calculate installments and due dates
		Pair<List<InstallmentInfo>, List<LocalDate>> result = installmentUtil
				.calculateInstallmentsAndDueDates(enrollmentDto, courseDto, enrollmentDto.getPaymentSchedule());

		List<InstallmentInfo> allInstallments = result.getLeft();
		List<LocalDate> dueDates = result.getRight();

		if (dueDates == null || dueDates.isEmpty()) {
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE,
					"No due dates generated. Please check schedule.");
		}

		// 4️⃣ Sort and filter due dates
		Collections.sort(dueDates);

		Optional<LocalDate> earliestFirstDate = dueDates.stream().min(Comparator.naturalOrder());

		// 5️⃣ Identify current due installment (if any)
		Optional<InstallmentInfo> optionalCurrentDue = allInstallments.stream()
				.filter(i -> !i.getDueDate().isAfter(today)).max(Comparator.comparing(InstallmentInfo::getDueDate));

		InstallmentInfo currentDueInstallment = optionalCurrentDue.orElse(null);

		// 6️⃣ Filter past installments (excluding current due)
		List<InstallmentInfo> pastInstallments = allInstallments.stream().filter(
				i -> currentDueInstallment == null || i.getDueDate().isBefore(currentDueInstallment.getDueDate()))
				.collect(Collectors.toList());

		// 7️⃣ Build ledger entries for past installments
		List<PaymentLedgerDto> ledgerDtos = new ArrayList<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

		for (InstallmentInfo installment : pastInstallments) {
			String formattedDate = installment.getDueDate().format(formatter);

			PaymentLedgerDto ledgerDto = PaymentLedgerDto.builder().effectiveDate(installment.getDueDate())
					.ledgerType(PaymentLedger.LedgerType.DEBIT).entryType(PaymentLedger.PaymentEntryType.PAST_DUE)
					.entryStatus(PaymentLedger.PaymentEntryStatus.PENDING).category(PaymentCategory.COURSE_FEE)
					.amount((double) installment.getAmount()).remainingAmount((double) installment.getAmount())
					.description(
							"Past due Installment #" + installment.getInstallmentNumber() + " due on " + formattedDate)
					// Additional fields
					.userId(enrollmentDto.getTraineeUserId()) // traineeUserId
					.enrollmentId(enrollmentDto.getId()) // TraineeCourseEnrollmentDto.id
					.academyId(courseDto.getAcademy() != null ? courseDto.getAcademy().getId() : null) // course dto
																										// academy id
					.programId(courseDto.getId()) // courseId
					.paymentId(null) // payment id - null for debit entries
					.createdById(null) // payment created by id - null for debit entries
					.build();

			ledgerDtos.add(ledgerDto);
		}

		// 8️⃣ Get paid payments
		List<Payment> payments = paymentRepo.findByTraineeCourseEnrollment_Id(enrollmentId);

		DateTimeFormatter fullDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		Map<PaymentCategory, Map<String, Long>> paidPayments = payments.stream()
				.filter(p -> p.getPaymentCategory() != PaymentCategory.REGISTRATION_FEE)
				.filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS)
				.collect(Collectors.groupingBy(Payment::getPaymentCategory, LinkedHashMap::new,
						Collectors.groupingBy(p -> p.getCreatedAt().toLocalDateTime().format(fullDateTimeFormatter),
								LinkedHashMap::new, Collectors.summingLong(Payment::getAmount))));

		// 9️⃣ Process FIFO payment allocation
		PaymentAllocationResult allocationResult = processFIFOPaymentAllocation(ledgerDtos, paidPayments,
				currentDueInstallment, formatter, payments, enrollmentDto, courseDto);

		// 🔟 Calculate updated amounts after payment allocation
		long updatedCurrentDueAmount = allocationResult.getCurrentDueRemainingAmount();
		LocalDate updatedDuesOn = allocationResult.getCurrentDueDate();

		// Calculate total outstanding amount (past due + current due)
		double totalOutstanding = ledgerDtos.stream()
				.filter(entry -> entry.getLedgerType() == PaymentLedger.LedgerType.DEBIT)
				.mapToDouble(PaymentLedgerDto::getRemainingAmount).sum() + updatedCurrentDueAmount;

		// Filter course pending to only include unpaid installments
		List<InstallmentInfo> coursePending = pastInstallments.stream()
				.filter(inst -> ledgerDtos.stream().anyMatch(
						entry -> entry.getEffectiveDate().equals(inst.getDueDate()) && entry.getRemainingAmount() > 0))
				.collect(Collectors.toList());

		// Update current due installment amount if partially paid
		InstallmentInfo updatedCurrentDue = null;
		if (currentDueInstallment != null) {
			updatedCurrentDue = new InstallmentInfo(currentDueInstallment.getInstallmentNumber(),
					updatedCurrentDueAmount, currentDueInstallment.getDueDate());
		}

		// ✅ 8️⃣ Add registration fee ledger if pending
		long paidRegFee = payments.stream().filter(p -> p.getPaymentCategory() == PaymentCategory.REGISTRATION_FEE)
				.filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS).mapToLong(Payment::getAmount).sum();

		long regFeeAmount;
		PaymentLedger.PaymentEntryStatus regFeeStatus;
		long pendingRegFee;

		if (paidRegFee > 0) {
			// Paid → take paid amount, mark settled
			regFeeAmount = paidRegFee;
			regFeeStatus = PaymentLedger.PaymentEntryStatus.SETTLED;
			pendingRegFee = 0L;
		} else {
			// Not paid → take course reg fee
			regFeeAmount = courseDto.getRegistrationFee() != null ? courseDto.getRegistrationFee() : 0L;
			regFeeStatus = regFeeAmount > 0 ? PaymentLedger.PaymentEntryStatus.PENDING
					: PaymentLedger.PaymentEntryStatus.SETTLED;
			pendingRegFee = regFeeAmount;
		}

		LocalDate regFeeEffectiveDate = earliestFirstDate.orElse(today);
		String regFeeDateFormatted = regFeeEffectiveDate.format(formatter);

		// ✅ Always add registration fee ledger, even if amount is 0 (mark as SETTLED)
		PaymentLedgerDto regFeeLedger = PaymentLedgerDto.builder().effectiveDate(regFeeEffectiveDate)
				.ledgerType(PaymentLedger.LedgerType.DEBIT).entryType(PaymentLedger.PaymentEntryType.REGISTRATION)
				.entryStatus(regFeeStatus).category(PaymentCategory.REGISTRATION_FEE).amount((double) regFeeAmount)
				.remainingAmount((double) pendingRegFee)
				.description("Registration fee ledger entry due on " + regFeeDateFormatted)
				// Additional fields
				.userId(enrollmentDto.getTraineeUserId()) // traineeUserId
				.enrollmentId(enrollmentDto.getId()) // TraineeCourseEnrollmentDto.id
				.academyId(courseDto.getAcademy() != null ? courseDto.getAcademy().getId() : null) // course dto academy
																									// id
				.programId(courseDto.getId()) // courseId
				.paymentId(null) // payment id - null for debit entries
				.createdById(null) // payment created by id - null for debit entries
				.build();

		// 1️⃣ Add REGISTRATION_FEE DEBIT entry (already present — keep as-is)
		ledgerDtos.add(regFeeLedger);

		// 2️⃣ If paid, add REGISTRATION_FEE CREDIT entry
		if (paidRegFee > 0) {
			PaymentLedgerDto regFeeCreditLedger = PaymentLedgerDto.builder().effectiveDate(regFeeEffectiveDate)
					.ledgerType(PaymentLedger.LedgerType.CREDIT).entryType(PaymentLedger.PaymentEntryType.REGISTRATION)
					.entryStatus(PaymentLedger.PaymentEntryStatus.SETTLED).category(PaymentCategory.REGISTRATION_FEE)
					.amount((double) paidRegFee).remainingAmount(0D)
					.description("Registration fee paid on " + regFeeDateFormatted)
					.userId(enrollmentDto.getTraineeUserId()).enrollmentId(enrollmentDto.getId())
					.academyId(courseDto.getAcademy() != null ? courseDto.getAcademy().getId() : null)
					.programId(courseDto.getId()).paymentId(null) // or you can attach actual payment ID if available
					.createdById(null) // or actual user if needed
					.build();

			ledgerDtos.add(regFeeCreditLedger);
		}

		// 1️⃣1️⃣ Prepare response
		Map<String, Object> response = new HashMap<>();
		response.put("duesOn", updatedDuesOn);
		response.put("duesAmount", updatedCurrentDueAmount);
		response.put("totalOutstanding", (long) totalOutstanding);
		response.put("coursePending", coursePending);
		response.put("currentDue", updatedCurrentDue);
		response.put("ledgerDtos", ledgerDtos);
		response.put("amount", enrollmentDto.getAmount());
		response.put("dueDates", dueDates);
		response.put("paidPayments", paidPayments);

		return ResponseBuilder.success(response, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);
	}

	/**
	 * Process FIFO payment allocation against past due and current due amounts
	 * Creates credit entries for each payment allocation and updates remaining
	 * amounts
	 */
	private PaymentAllocationResult processFIFOPaymentAllocation(List<PaymentLedgerDto> ledgerDtos,
			Map<PaymentCategory, Map<String, Long>> paidPayments, InstallmentInfo currentDueInstallment,
			DateTimeFormatter formatter, List<Payment> payments, TraineeCourseEnrollmentDto enrollmentDto,
			CourseDto courseDto) {

		// Get all COURSE_FEE payments and sort by payment date (FIFO)
		Map<String, Long> courseFeePayments = paidPayments.getOrDefault(PaymentCategory.COURSE_FEE, new HashMap<>());

		if (courseFeePayments.isEmpty()) {
			return new PaymentAllocationResult(currentDueInstallment != null ? currentDueInstallment.getAmount() : 0,
					currentDueInstallment != null ? currentDueInstallment.getDueDate() : null);
		}

		// Convert to list and sort by date for FIFO processing
		DateTimeFormatter fullDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		List<Map.Entry<LocalDateTime, Long>> sortedPayments = courseFeePayments.entrySet().stream().map(e -> {
			try {
				return new AbstractMap.SimpleEntry<>(LocalDateTime.parse(e.getKey(), fullDateTimeFormatter),
						e.getValue());
			} catch (Exception ex) {
				System.err.println("Invalid payment datetime: " + e.getKey());
				return null;
			}
		}).filter(Objects::nonNull).sorted(Map.Entry.comparingByKey()) // ✅ Sort by actual LocalDateTime
				.collect(Collectors.toList());

		// Create virtual current due for allocation calculation (don't add to ledger)
		long currentDueRemainingAmount = currentDueInstallment != null ? currentDueInstallment.getAmount() : 0;
		LocalDate currentDueDateForAllocation = currentDueInstallment != null ? currentDueInstallment.getDueDate()
				: null;

		// Create combined list for FIFO allocation (past due + virtual current due)
		List<DebitAllocation> allDebitAllocations = new ArrayList<>();

		// Add past due entries
		for (PaymentLedgerDto debitEntry : ledgerDtos) {
			allDebitAllocations.add(new DebitAllocation(debitEntry, debitEntry.getRemainingAmount().longValue()));
		}

		// Add current due for allocation calculation only
		if (currentDueInstallment != null) {
			allDebitAllocations.add(new DebitAllocation(null, currentDueRemainingAmount,
					currentDueInstallment.getDueDate(), "current_due"));
		}

		// Sort all allocations by due date (FIFO allocation - oldest dues first)
		allDebitAllocations.sort(Comparator.comparing(DebitAllocation::getDueDate));

		// Create a map for quick payment lookup by datetime
		Map<String, Payment> paymentByDateTime = payments.stream()
				.filter(p -> p.getPaymentCategory() == PaymentCategory.COURSE_FEE)
				.collect(Collectors.toMap(p -> p.getCreatedAt().toLocalDateTime().format(fullDateTimeFormatter), p -> p,
						(existing, replacement) -> existing // Keep first if duplicates
				));

		// Process each payment against debit entries using FIFO
		for (Map.Entry<LocalDateTime, Long> paymentEntry : sortedPayments) {
			LocalDateTime paymentDateTime = paymentEntry.getKey();
			Long paymentAmount = paymentEntry.getValue();
			Long remainingPaymentAmount = paymentAmount;

			try {
				LocalDate paymentLocalDate = paymentDateTime.toLocalDate();

				// Get payment details for credit entry
				String paymentDateTimeStr = paymentDateTime.format(fullDateTimeFormatter);
				Payment payment = paymentByDateTime.get(paymentDateTimeStr);
				String paymentId = payment != null ? payment.getId() : null;
				String createdById = payment != null ? payment.getPaymentInitiatedByUserProfile().getId() : null;

				// Create single credit entry for this payment
				PaymentLedgerDto creditEntry = PaymentLedgerDto.builder().effectiveDate(paymentLocalDate)
						.paymentDate(paymentLocalDate).ledgerType(PaymentLedger.LedgerType.CREDIT)
						.entryType(PaymentLedger.PaymentEntryType.PAYMENT)
						.entryStatus(PaymentLedger.PaymentEntryStatus.SETTLED).category(PaymentCategory.COURSE_FEE)
						.amount((double) paymentAmount).remainingAmount(0.0)
						.description(
								"Payment of ₹" + paymentAmount + " received on " + paymentLocalDate.format(formatter))
						// Additional fields for credit entries
						.userId(enrollmentDto.getTraineeUserId()) // traineeUserId
						.enrollmentId(enrollmentDto.getId()) // TraineeCourseEnrollmentDto.id
						.academyId(courseDto.getAcademy() != null ? courseDto.getAcademy().getId() : null) // course dto
																											// academy
																											// id
						.programId(courseDto.getId()) // courseId
						.paymentId(paymentId) // actual payment id for credit entries
						.createdById(createdById) // payment created by id for credit entries
						.build();
				ledgerDtos.add(creditEntry);

				// Apply this payment to debit entries in FIFO order (oldest dues first)
				for (DebitAllocation allocation : allDebitAllocations) {
					if (remainingPaymentAmount <= 0) {
						break; // Payment fully allocated
					}

					if (allocation.getRemainingAmount() <= 0) {
						continue; // This debit entry is already fully paid
					}

					// Calculate allocation amount
					long allocationAmount = Math.min(remainingPaymentAmount.longValue(),
							allocation.getRemainingAmount());

					if (allocationAmount > 0) {
						// Update remaining amount in allocation
						allocation.setRemainingAmount(allocation.getRemainingAmount() - allocationAmount);

						// If this is a past due entry, update the actual ledger entry
						if (allocation.getLedgerEntry() != null) {
							allocation.getLedgerEntry().setRemainingAmount(
									allocation.getLedgerEntry().getRemainingAmount() - allocationAmount);

							// Update entry status if fully paid
							if (allocation.getLedgerEntry().getRemainingAmount() == 0) {
								allocation.getLedgerEntry().setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
							}
						} else if ("current_due".equals(allocation.getType())) {
							// Update virtual current due remaining amount
							currentDueRemainingAmount = allocation.getRemainingAmount();
						}

						// Reduce remaining payment amount
						remainingPaymentAmount -= allocationAmount;
					}
				}

			} catch (Exception e) {
				// Log error and skip this payment if date parsing fails
				System.err.println("Error processing payment date: " + paymentDateTime + ", Error: " + e.getMessage());
			}
		}

		return new PaymentAllocationResult(currentDueRemainingAmount, currentDueDateForAllocation);
	}

	// Helper classes for allocation tracking
	private static class DebitAllocation {
		private PaymentLedgerDto ledgerEntry;
		private long remainingAmount;
		private LocalDate dueDate;
		private String type;

		public DebitAllocation(PaymentLedgerDto ledgerEntry, long remainingAmount) {
			this.ledgerEntry = ledgerEntry;
			this.remainingAmount = remainingAmount;
			this.dueDate = ledgerEntry.getEffectiveDate();
			this.type = "past_due";
		}

		public DebitAllocation(PaymentLedgerDto ledgerEntry, long remainingAmount, LocalDate dueDate, String type) {
			this.ledgerEntry = ledgerEntry;
			this.remainingAmount = remainingAmount;
			this.dueDate = dueDate;
			this.type = type;
		}

		// Getters and setters
		public PaymentLedgerDto getLedgerEntry() {
			return ledgerEntry;
		}

		public long getRemainingAmount() {
			return remainingAmount;
		}

		public void setRemainingAmount(long remainingAmount) {
			this.remainingAmount = remainingAmount;
		}

		public LocalDate getDueDate() {
			return dueDate;
		}

		public String getType() {
			return type;
		}
	}

	private static class PaymentAllocationResult {
		private long currentDueRemainingAmount;
		private LocalDate currentDueDate;

		public PaymentAllocationResult(long currentDueRemainingAmount, LocalDate currentDueDate) {
			this.currentDueRemainingAmount = currentDueRemainingAmount;
			this.currentDueDate = currentDueDate;
		}

		public long getCurrentDueRemainingAmount() {
			return currentDueRemainingAmount;
		}

		public LocalDate getCurrentDueDate() {
			return currentDueDate;
		}
	}

	@Override
	public ServiceResponse getLedgers(String enrollmentId) {
		try {
			Optional<TraineeCourseEnrollment> enrollmentOpt = traineeCourseEnrollmentRepo.findById(enrollmentId);
			if (enrollmentOpt.isEmpty()) {
				log.warn("❌ Enrollment not found for ID: {}", enrollmentId);
				return ResponseBuilder.error("Enrollment not found", ApiResponse.ENROLLMENT_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			TraineeCourseEnrollment enrollment = enrollmentOpt.get();

			// Create ledgerDao from enrollment
			PaymentLedgerDao ledgerDao = createDao(enrollment, enrollment.getAmount(), LedgerType.DEBIT,
					PaymentEntryStatus.PENDING);

			// Fetch ledgers from DB
			List<PaymentLedger> paymentLedgers = ledgerRepository.findByEnrollment_Id(enrollmentId);

			// Convert ledger entities to DAOs
			List<PaymentLedgerDao> ledgerDAOs = paymentLedgers.stream().map(this::convertToDAO)
					.collect(Collectors.toList());

			// Add the enrollment-level ledgerDao as well
			ledgerDAOs.add(ledgerDao);

			return ResponseBuilder.success(ledgerDAOs, ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("❌ Error while fetching dues", e);
			return ResponseBuilder.error("Unexpected error while fetching payment details",
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private PaymentLedgerDao createDao(TraineeCourseEnrollment enrollment, Long amount, LedgerType type,
			PaymentEntryStatus entryStatus) {
		PaymentLedgerDao dao = new PaymentLedgerDao();

		// Set enrollment-level fields
		dao.setEnrollmentId(enrollment.getId());
		dao.setEffectiveDate(enrollment.getDuesOn());
		dao.setAmount(amount.doubleValue());
		dao.setRemainingAmount(amount.doubleValue());
		dao.setLedgerType(type);
		dao.setEntryStatus(entryStatus);

		if (enrollment.getTraineeUserProfile() != null) {
			dao.setUserId(enrollment.getTraineeUserProfile().getId());
			dao.setUserName(enrollment.getTraineeUserProfile().getDisplayName());
		}

		if (enrollment.getAcademy() != null) {
			dao.setAcademyId(enrollment.getAcademy().getId());
			dao.setAcademyName(enrollment.getAcademy().getName());
		}

		if (enrollment.getCourse() != null) {
			dao.setCourseId(enrollment.getCourse().getId());
			dao.setCourseName(enrollment.getCourse().getTitle());
		}

		return dao;
	}

	// Helper method to convert PaymentLedger entity to DAO
	private PaymentLedgerDao convertToDAO(PaymentLedger ledger) {
		PaymentLedgerDao dao = new PaymentLedgerDao();

		// Use BeanUtils.copyProperties for basic field mapping
		BeanUtils.copyProperties(ledger, dao);

		// Manual mapping for related entity IDs and complex fields
		if (ledger.getUser() != null) {
			dao.setUserId(ledger.getUser().getId());
			dao.setUserName(ledger.getUser().getDisplayName()); // Assuming UserProfile has getName()
		}

		if (ledger.getEnrollment() != null) {
			dao.setEnrollmentId(ledger.getEnrollment().getId());
		}

		if (ledger.getAcademy() != null) {
			dao.setAcademyId(ledger.getAcademy().getId());
			dao.setAcademyName(ledger.getAcademy().getName()); // Assuming Academy has getName()
		}

		if (ledger.getProgram() != null) {
			dao.setCourseId(ledger.getProgram().getId());
			dao.setCourseName(ledger.getProgram().getTitle()); // Assuming Course has getName()
		}

		return dao;
	}

	@Override
	public ServiceResponse getDues(String userId, String courseId) {
		try {
			// 1️⃣ Fetch enrollments for the user in the course
			List<TraineeCourseEnrollment> enrollments = traineeCourseEnrollmentRepo
					.findByCourse_IdAndTraineeUserProfile_Id(courseId, userId);

			if (enrollments == null || enrollments.isEmpty()) {
				return ResponseBuilder.error("No enrollment found for user and course.", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			// 2️⃣ Filter active enrollments
			List<TraineeCourseEnrollment> activeEnrollments = enrollments.stream()
					.filter(e -> e.getStatus() == Status.ACTIVE).collect(Collectors.toList());

			if (activeEnrollments.size() > 1) {
				return ResponseBuilder.error("Multiple active enrollments found for same user and course.",
						ErrorCodes.INVALID_REQUEST, HttpStatus.CONFLICT);
			}

			Optional<TraineeCourseEnrollment> activeOpt = activeEnrollments.stream().findFirst();
			if (activeOpt.isEmpty()) {
				return ResponseBuilder.error("No active enrollment found.", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			String enrollmentId = activeOpt.get().getId();

			// 3️⃣ Initialize due amounts
			Long registrationPendingAmount = 0L;
			Long coursePendingAmount = 0L;

			// 🔹 REGISTRATION_FEE
			ServiceResponse regResponse = getPaymentDetails(enrollmentId, PaymentCategory.REGISTRATION_FEE);
			if (regResponse.getHttpStatus().is2xxSuccessful() && regResponse.getBody() != null) {
				PaymentDetailsDto regDetails = (PaymentDetailsDto) regResponse.getBody();
				Boolean isDue = regDetails.getIsDue();
				if (Boolean.TRUE.equals(isDue)) {
					registrationPendingAmount = regDetails.getPendingAmount() != null ? regDetails.getPendingAmount()
							: 0L;
				}
			} else {
				log.error("❌ Failed to fetch REGISTRATION_FEE pending amount for enrollmentId: {}", enrollmentId);
				return ResponseBuilder.error("No fee found", ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
			}

			// 🔹 COURSE_FEE
			ServiceResponse courseResponse = getPaymentDetails(enrollmentId, PaymentCategory.COURSE_FEE);
			if (courseResponse.getHttpStatus().is2xxSuccessful() && courseResponse.getBody() != null) {
				PaymentDetailsDto courseDetails = (PaymentDetailsDto) courseResponse.getBody();
				Boolean isDue = courseDetails.getIsDue();
				if (Boolean.TRUE.equals(isDue)) {
					coursePendingAmount = courseDetails.getPendingAmount() != null ? courseDetails.getPendingAmount()
							: 0L;
				}
			} else {
				log.error("❌ Failed to fetch COURSE_FEE pending amount for enrollmentId: {}", enrollmentId);
				return ResponseBuilder.error("No fee found", ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
			}

			// ✅ Prepare and return successful response
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("enrollmentId", enrollmentId);
			responseMap.put("registrationFee", registrationPendingAmount);
			responseMap.put("courseFee", coursePendingAmount);

			return ResponseBuilder.success(responseMap, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("❌ Error while fetching dues", e);
			return ResponseBuilder.error("Unexpected error while fetching payment details",
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse getAllDues(String enrollmentId) {
		try {
			// 1️⃣ Validate enrollment exists
			Optional<TraineeCourseEnrollment> enrollmentOpt = traineeCourseEnrollmentRepo.findById(enrollmentId);
			if (enrollmentOpt.isEmpty()) {
				log.warn("❌ Enrollment not found for ID: {}", enrollmentId);
				return ResponseBuilder.error("Enrollment not found", ApiResponse.ENROLLMENT_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			// 2️⃣ Define required categories
			List<PaymentCategory> requiredCategories = List.of(PaymentCategory.REGISTRATION_FEE,
					PaymentCategory.COURSE_FEE);

			Map<String, PaymentDetailsDto> paymentDetailsMap = new HashMap<>();

			// 3️⃣ Loop through categories
			for (PaymentCategory category : requiredCategories) {
				ServiceResponse response = getPaymentDetails(enrollmentId, category);

				if (!response.getHttpStatus().is2xxSuccessful() || response.getBody() == null) {
					log.warn("❌ Missing payment details for category: {}", category);
					return ResponseBuilder.error("Missing required payment details",
							ApiResponse.PAYMENT_DETAILS_NOT_FOUND, HttpStatus.NOT_FOUND);
				}

				paymentDetailsMap.put(category.name().toLowerCase(), (PaymentDetailsDto) response.getBody());
			}

			// 4️⃣ Prepare final response
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("enrollmentId", enrollmentId);
			responseMap.put("paymentDetails", paymentDetailsMap);

			return ResponseBuilder.success(responseMap, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("❌ Exception while fetching dues for enrollmentId: {}", enrollmentId, e);
			return ResponseBuilder.error("Unexpected error while fetching dues", ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional
	public ServiceResponse clearDues(String enrollmentId) {
		try {
			// ✅ 1️⃣ Validate enrollment exists
			Optional<TraineeCourseEnrollment> enrollmentOpt = traineeCourseEnrollmentRepo.findById(enrollmentId);
			if (enrollmentOpt.isEmpty()) {
				log.warn("❌ Enrollment not found for ID: {}", enrollmentId);
				return ResponseBuilder.error("Enrollment not found", ApiResponse.ENROLLMENT_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}
			TraineeCourseEnrollment savedEnrollment = enrollmentOpt.get();

			// ✅ 2️⃣ Fetch enrollment + course details
			Pair<TraineeCourseEnrollmentDto, CourseDto> courseEnrollmentDetails = courseService
					.getByEnrollmentId(enrollmentId);
			if (courseEnrollmentDetails == null || courseEnrollmentDetails.getLeft() == null
					|| courseEnrollmentDetails.getRight() == null) {
				throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE,
						"Unable to fetch enrollment or course details.");
			}

			TraineeCourseEnrollmentDto enrollmentDto = courseEnrollmentDetails.getLeft();
			CourseDto courseDto = courseEnrollmentDetails.getRight();

			// ✅ 3️⃣ Calculate installments
			Pair<List<InstallmentInfo>, List<LocalDate>> result = installmentUtil
					.calculateInstallmentsAndDueDates(enrollmentDto, courseDto, enrollmentDto.getPaymentSchedule());
			List<InstallmentInfo> allInstallments = result.getLeft();

			// ✅ 4️⃣ Get last installment due date
			LocalDate lastInstallmentDate = allInstallments.stream().map(InstallmentInfo::getDueDate)
					.max(LocalDate::compareTo).orElse(LocalDate.now());

			// TODO: SYSTEM does nothing — client confirmed 6–7 times
			//
			// // ✅ 5️⃣ Fetch existing ledger entries
			// List<PaymentLedger> paymentLedgers =
			// ledgerRepository.findByEnrollment_Id(enrollmentId);
			//
			// // ✅ 6️⃣ Calculate waiver ONLY for PAST_DUE
			// List<PaymentLedger> pastDueLedgers = paymentLedgers.stream()
			// .filter(ledger -> ledger.getEntryType() ==
			// PaymentLedger.PaymentEntryType.PAST_DUE
			// && (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
			// || ledger.getEntryStatus() ==
			// PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
			// .toList();
			//
			// double totalWaived =
			// pastDueLedgers.stream().mapToDouble(PaymentLedger::getRemainingAmount).sum();
			//
			// // ✅ 7️⃣ Create waiver ledger entry if needed
			// if (totalWaived > 0) {
			// PaymentLedger waiverLedger = new PaymentLedger();
			// waiverLedger.setEnrollment(savedEnrollment);
			// waiverLedger.setEntryType(PaymentLedger.PaymentEntryType.WAIVER);
			// waiverLedger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
			// waiverLedger.setAmount(totalWaived);
			// waiverLedger.setRemainingAmount(0.0);
			// waiverLedger.setEffectiveDate(LocalDate.now());
			// waiverLedger.setNotes("Waiver created for PAST_DUE entries during dues
			// clearance");
			// ledgerRepository.save(waiverLedger);
			// }
			//
			// // ✅ 8️⃣ Update existing ledgers
			// for (PaymentLedger ledger : paymentLedgers) {
			// PaymentLedger.PaymentEntryType type = ledger.getEntryType();
			// PaymentLedger.PaymentEntryStatus status = ledger.getEntryStatus();
			//
			// if ((type == PaymentLedger.PaymentEntryType.ADVANCE)
			// && (status == PaymentLedger.PaymentEntryStatus.PENDING
			// || status == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED)) {
			// ledger.setRemainingAmount(0.0);
			// ledger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
			// }
			//
			// if ((type == PaymentLedger.PaymentEntryType.DISCOUNT || type ==
			// PaymentLedger.PaymentEntryType.PAST_DUE)
			// && (status == PaymentLedger.PaymentEntryStatus.PENDING
			// || status == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED)) {
			// ledger.setRemainingAmount(0.0);
			// ledger.setEntryStatus(PaymentLedger.PaymentEntryStatus.CANCELLED);
			// }
			// }
			//
			// // ✅ 9️⃣ Save updated ledgers
			// ledgerRepository.saveAll(paymentLedgers);

			// ✅ 🔟 Final enrollment update
			savedEnrollment.setDuesOn(lastInstallmentDate);
			savedEnrollment.setFinalDueAmount(0L);
			savedEnrollment.setStatus(Status.INACTIVE);
			traineeCourseEnrollmentRepo.save(savedEnrollment);

			log.info("✅ Dues cleared, past dues waived, and enrollment updated for ID: {}", enrollmentId);
			return ResponseBuilder.success(ApiResponse.DUES_UPDATED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			log.error("❌ Error clearing dues for enrollmentId: {}", enrollmentId, e);
			return ResponseBuilder.error("Unexpected error while clearing dues", ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse getPaymentDetails(String enrollmentId, PaymentCategory paymentCategory) {
		try {
			// ✅ Validate enrollment exists
			if (!traineeCourseEnrollmentRepo.existsById(enrollmentId)) {
				return ResponseBuilder.error("Enrollment not found", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.BAD_REQUEST);
			}

			Pair<TraineeCourseEnrollmentDto, CourseDto> courseEnrollmentDetails = courseService
					.getByEnrollmentId(enrollmentId);

			if (courseEnrollmentDetails == null || courseEnrollmentDetails.getLeft() == null
					|| courseEnrollmentDetails.getRight() == null) {
				return ResponseBuilder.error("Unable to fetch enrollment and course details",
						ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			TraineeCourseEnrollmentDto enrollmentDto = courseEnrollmentDetails.getLeft();
			CourseDto courseDto = courseEnrollmentDetails.getRight();

			if (enrollmentDto.getDuesOn() == null) {
				return ResponseBuilder.error("No dues date found for the enrollment.", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.EXPECTATION_FAILED);
			}

			PaymentDetailsDto paymentDetailsDto = new PaymentDetailsDto();
			paymentDetailsDto.setEnrollmentId(enrollmentId);
			paymentDetailsDto.setOriginalAmount(enrollmentDto.getAmount());
			paymentDetailsDto.setPaymentSchedule(enrollmentDto.getPaymentSchedule());
			paymentDetailsDto.setUseForFuture(enrollmentDto.getUseForFuture());
			paymentDetailsDto.setPaymentCategory(paymentCategory);
			paymentDetailsDto.setCurrency(courseDto.getSchedule().getCurrency());

			// Get all payment ledgers and payments for this enrollment
			List<PaymentLedger> paymentsLedgers = ledgerRepository.findByEnrollment_Id(enrollmentId);
			List<Payment> payments = paymentRepo.findByTraineeCourseEnrollment_Id(enrollmentId);

			switch (paymentCategory) {
			case REGISTRATION_FEE:
				handleRegistrationFee(paymentDetailsDto, paymentsLedgers, enrollmentDto, payments);
				break;

			case COURSE_FEE:
				handleCourseFee(paymentDetailsDto, paymentsLedgers, enrollmentDto, courseDto, payments);
				break;

			default:
				return ResponseBuilder.error("Unsupported payment category", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			return ResponseBuilder.success(paymentDetailsDto, ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			return ResponseBuilder.error("Unexpected error while fetching payment details",
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void handleCourseFee(PaymentDetailsDto paymentDetailsDto, List<PaymentLedger> paymentsLedgers,
			TraineeCourseEnrollmentDto enrollmentDto, CourseDto courseDto, List<Payment> payments) {

		List<PaymentLedger> pastDueLedgers = paymentsLedgers.stream()
				.filter(ledger -> ledger.getCategory() == PaymentCategory.COURSE_FEE
						&& ledger.getEntryType() == PaymentLedger.PaymentEntryType.PAST_DUE
						&& (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
								|| ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
				.collect(Collectors.toList());

		List<PaymentLedger> advanceLedgers = paymentsLedgers.stream()
				.filter(ledger -> ledger.getCategory() == PaymentCategory.COURSE_FEE
						&& ledger.getEntryType() == PaymentLedger.PaymentEntryType.ADVANCE
						&& (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
								|| ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
				.collect(Collectors.toList());

		List<PaymentLedger> discountLedgers = paymentsLedgers.stream()
				.filter(ledger -> ledger.getCategory() == PaymentCategory.COURSE_FEE
						&& ledger.getEntryType() == PaymentLedger.PaymentEntryType.DISCOUNT
						&& (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
								|| ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
				.collect(Collectors.toList());

		List<PaymentLedger> adjustmentLedgers = paymentsLedgers.stream()
				.filter(ledger -> ledger.getCategory() == PaymentCategory.COURSE_FEE
						&& ledger.getEntryType() == PaymentLedger.PaymentEntryType.ADJUSTMENT
						&& (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
								|| ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
				.collect(Collectors.toList());

		Map<String, Long> pendingPayments = pastDueLedgers.stream()
				.collect(Collectors.groupingBy(ledger -> ledger.getEffectiveDate().toString(), LinkedHashMap::new,
						Collectors.summingLong(ledger -> ledger.getRemainingAmount().longValue())));

		long totalPendingAmount = pendingPayments.values().stream().mapToLong(Long::longValue).sum();
		long totalAdvances = advanceLedgers.stream().mapToLong(l -> l.getRemainingAmount().longValue()).sum();
		long totalDiscounts = discountLedgers.stream().mapToLong(l -> l.getRemainingAmount().longValue()).sum();

		long totalAdjustments = adjustmentLedgers.stream().mapToLong(l -> l.getRemainingAmount().longValue()).sum();

		Map<String, Long> calculatedFuturePayments = calculateFuturePayments(enrollmentDto, courseDto, paymentsLedgers,
				totalAdvances, totalDiscounts, totalAdjustments, enrollmentDto.getDuesOn());

		paymentDetailsDto.setAdvanceCredit(totalAdvances);
		paymentDetailsDto.setAdjustments(totalAdjustments);
		paymentDetailsDto.setDiscountAmount(enrollmentDto.getDiscountAmount() + totalDiscounts);

		long finalDueAmount = enrollmentDto.getFinalDueAmount() != null ? enrollmentDto.getFinalDueAmount() : 0L;
		totalPendingAmount += Math.max(0, finalDueAmount - paymentDetailsDto.getDiscountAmount());
		paymentDetailsDto.setPendingAmount(totalPendingAmount);

		long totalPayableAmount = calculatedFuturePayments.values().stream().mapToLong(Long::longValue).sum();
		paymentDetailsDto.setTotalPayableAmount(totalPayableAmount + totalPendingAmount);

		paymentDetailsDto.setFuturePayments(calculatedFuturePayments);
		paymentDetailsDto.setPendingPayments(pendingPayments);
		paymentDetailsDto.setPaymentHistory(buildHistory(payments, PaymentCategory.COURSE_FEE));

		paymentDetailsDto.setNextDueAt(enrollmentDto.getDuesOn().toString());
		paymentDetailsDto.setTotalPastDues(
				pendingPayments != null ? pendingPayments.values().stream().mapToLong(Long::longValue).sum() : 0L);
		paymentDetailsDto.setCurrentDue(Math.max(0, finalDueAmount - paymentDetailsDto.getDiscountAmount()));

		List<LocalDate> dueDates = installmentUtil.calculateDueDates(enrollmentDto, courseDto,
				enrollmentDto.getPaymentSchedule());

		Collections.sort(dueDates);
		LocalDate today = LocalDate.now();

		boolean isDue = false;
		LocalDate duesOn = enrollmentDto.getDuesOn();

		// Check if there's a pending amount and the due date has arrived or passed
		if (totalPendingAmount > 0 && duesOn != null) {
			// isDue should be true if today is on or after the due date
			isDue = today.isEqual(duesOn) || today.isAfter(duesOn);
		}

		paymentDetailsDto.setIsDue(isDue);
	}

	private void handleRegistrationFee(PaymentDetailsDto paymentDetailsDto, List<PaymentLedger> paymentsLedgers,
			TraineeCourseEnrollmentDto enrollmentDto, List<Payment> payments) {

		// Registration ledgers only
		List<PaymentLedger> registrationLedgers = paymentsLedgers.stream()
				.filter(ledger -> ledger.getCategory() == PaymentCategory.REGISTRATION_FEE
						&& ledger.getEntryType() == PaymentLedger.PaymentEntryType.REGISTRATION
						&& (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
								|| ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
				.collect(Collectors.toList());

		List<PaymentLedger> discountLedgers = paymentsLedgers.stream()
				.filter(ledger -> ledger.getCategory() == PaymentCategory.REGISTRATION_FEE
						&& ledger.getEntryType() == PaymentLedger.PaymentEntryType.DISCOUNT
						&& (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
								|| ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
				.collect(Collectors.toList());

		List<PaymentLedger> adjustmentLedgers = paymentsLedgers.stream()
				.filter(ledger -> ledger.getCategory() == PaymentCategory.REGISTRATION_FEE
						&& ledger.getEntryType() == PaymentLedger.PaymentEntryType.ADJUSTMENT
						&& (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
								|| ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
				.collect(Collectors.toList());

		long totalPendingAmount = registrationLedgers.stream()
				.mapToLong(l -> l.getRemainingAmount() != null ? l.getRemainingAmount().longValue() : 0L).sum();

		long totalDiscounts = discountLedgers.stream()
				.mapToLong(l -> l.getRemainingAmount() != null ? l.getRemainingAmount().longValue() : 0L).sum();

		long totalAdjustments = adjustmentLedgers.stream()
				.mapToLong(l -> l.getRemainingAmount() != null ? l.getRemainingAmount().longValue() : 0L).sum();

		// 🎯 Combine discount + adjustment and subtract from pending
		long adjustedPendingAmount = totalPendingAmount - (totalDiscounts + totalAdjustments);

		// Ensure pending doesn't go negative
		adjustedPendingAmount = Math.max(0, adjustedPendingAmount);

		log.info("✅ Total Pending: {}", totalPendingAmount);
		log.info("💸 Total Discount + Adjustment: {}", totalDiscounts + totalAdjustments);
		log.info("📌 Final Pending After Adjustments: {}", adjustedPendingAmount);

		paymentDetailsDto.setAdjustments(totalAdjustments);
		paymentDetailsDto.setDiscountAmount(totalDiscounts);

		paymentDetailsDto.setPendingAmount(adjustedPendingAmount);
		paymentDetailsDto.setTotalPayableAmount(adjustedPendingAmount);
		paymentDetailsDto.setNextDueAt(adjustedPendingAmount > 0 ? enrollmentDto.getJoiningDate().toString()
				: enrollmentDto.getDuesOn().toString());
		paymentDetailsDto.setPaymentHistory(buildHistory(payments, PaymentCategory.REGISTRATION_FEE));
		paymentDetailsDto.setPendingPayments(Map.of()); // No separate past due grouping needed
		paymentDetailsDto.setFuturePayments(Map.of());
		paymentDetailsDto.setTotalPastDues(0L);
		paymentDetailsDto.setCurrentDue(adjustedPendingAmount);
		paymentDetailsDto.setIsDue(adjustedPendingAmount > 0);
	}

	private Map<String, Long> calculateFuturePayments(TraineeCourseEnrollmentDto enrollment, CourseDto course,
			List<PaymentLedger> existingLedgers, long availableAdvances, long availableDiscounts,
			long availableAdjustments, LocalDate duesOn) {

		Map<String, Long> futurePayments = new LinkedHashMap<>();

		// Get payment schedule and dates
		PaymentSchedule paymentSchedule = enrollment.getPaymentSchedule();
//		LocalDate startDate = enrollment.getJoiningDate();
//		LocalDate dueDate = enrollment.getDueDate();
//		LocalDate courseEndDate = LocalDate.parse(course.getSchedule().getEndDate());
		LocalDate today = LocalDate.now();

//		LocalDate currentDuesOn = enrollment.getDuesOn();
//		Long finalDueAmount = enrollment.getFinalDueAmount();

		// Use dueDate as the actual start date for payment calculations
//		LocalDate paymentStartDate = dueDate != null ? dueDate : startDate;

		// Calculate total course amount (excluding registration fee)
		long totalCourseAmount = enrollment.getAmount() != null ? enrollment.getAmount() : 0L;

		if (totalCourseAmount <= 0 || paymentSchedule == null) {
			return futurePayments;
		}

		// Calculate installments with proration
		List<InstallmentInfo> installments = installmentUtil.calculateInstallments(enrollment, course, paymentSchedule);

		// Get already paid amounts from existing ledgers
		Set<LocalDate> paidDates = existingLedgers.stream()
				.filter(ledger -> ledger.getEntryType() == PaymentLedger.PaymentEntryType.PAYMENT
						&& ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.SETTLED)
				.map(PaymentLedger::getEffectiveDate).collect(Collectors.toSet());

		// Apply credits (advances and ledger discounts) to future installments
		long remainingCredits = availableAdvances + availableDiscounts + availableAdjustments;

		// Handle enrollment discount separately based on useForFuture flag
		long enrollmentDiscount = enrollment.getDiscountAmount() != null ? enrollment.getDiscountAmount() : 0L;
		boolean applyDiscountToAllFuture = enrollment.getUseForFuture() != null && enrollment.getUseForFuture();

		log.info("Enrollment discount amount: {}, useForFuture: {}, applyDiscountToAllFuture: {}", enrollmentDiscount,
				enrollment.getUseForFuture(), applyDiscountToAllFuture);

		// First, collect all future installments (not paid and not past due)
		List<InstallmentInfo> futureInstallments = new ArrayList<>();
		for (InstallmentInfo installment : installments) {
			LocalDate installmentDate = installment.getDueDate();

			// Skip if already paid
			if (paidDates.contains(installmentDate)) {
				continue;
			}

//			TODO ADD BACK AGAIN IN FUTURE ASAP
			// Skip past dates (these should be in pending payments)
			if (installmentDate.isBefore(today) || installmentDate.isEqual(today)) {
				continue;
			}

			futureInstallments.add(installment);
		}

		// When useForFuture is true, apply the full discount to each installment
		long perInstallmentDiscount = 0;
		if (applyDiscountToAllFuture && enrollmentDiscount > 0) {
			perInstallmentDiscount = enrollmentDiscount;
			log.info("Per-installment discount (useForFuture=true): {} applied to each installment",
					perInstallmentDiscount);
		}

		// Now process each future installment
		for (InstallmentInfo installment : futureInstallments) {
			LocalDate installmentDate = installment.getDueDate();

			long installmentAmount = installment.getAmount();

			// Apply available credits (advances and ledger discounts) first
			if (remainingCredits > 0) {
				if (remainingCredits >= installmentAmount) {
					// This installment is fully covered by credits
					remainingCredits -= installmentAmount;
					continue; // Skip adding to future payments
				} else {
					// Partially covered by credits
					installmentAmount -= remainingCredits;
					remainingCredits = 0;
				}
			}

			// Apply enrollment discount ONLY when useForFuture is true
			if (applyDiscountToAllFuture && enrollmentDiscount > 0) {
				// Apply the full discount to each future schedule
				// If discount is greater than installment amount, set to 0
				installmentAmount = Math.max(0, installmentAmount - enrollmentDiscount);
				log.info("Applied discount {} to installment {}, new amount: {}", enrollmentDiscount, installmentDate,
						installmentAmount);
			}
			// When useForFuture is false, enrollment discount should NOT be applied to
			// future payments
			// It should only be applied to current/pending payments

			// Add to future payments if amount > 0
			if (installmentAmount > 0 && (installmentDate.isAfter(duesOn))) {
				futurePayments.put(installmentDate.toString(), installmentAmount);
			}
		}

		log.info("Future payments calculated: {}", futurePayments);

		return futurePayments;
	}

	private static Map<String, PaymentDto> buildHistory(List<Payment> payments, PaymentCategory paymentCategory) {

		return payments.stream().filter(payment -> payment.getPaymentStatus() == PaymentStatus.SUCCESS
				&& payment.getPaymentCategory() == paymentCategory).collect(Collectors.toMap(payment -> {
					if (payment.getPaymentInstallmentDate() != null && !payment.getPaymentInstallmentDate().isEmpty()) {
						return payment.getPaymentInstallmentDate();
					} else if (payment.getCreatedAt() != null) {
						return payment.getCreatedAt().toLocalDateTime().toLocalDate().toString();
					} else {
						return "UNKNOWN_DATE";
					}
				}, payment -> {
					PaymentDto dto = new PaymentDto();
					dto.setId(payment.getId());
					dto.setExternalTransactionId(
							payment.getExternalTransactionId() != null ? payment.getExternalTransactionId() : "");
					dto.setPaymentMode(payment.getPaymentMode() != null ? payment.getPaymentMode() : "");
					dto.setTransactionTime(payment.getTransactionTime() != null ? payment.getTransactionTime()
							: payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : "");
					dto.setPaymentStatus(payment.getPaymentStatus());
					dto.setAmount(payment.getAmount() != null ? payment.getAmount() : 0L);
					dto.setCurrency(payment.getCurrency());
					dto.setPaymentSchedule(payment.getPaymentSchedule());
					dto.setPaymentInstallmentDate(
							payment.getPaymentInstallmentDate() != null ? payment.getPaymentInstallmentDate() : "");
					dto.setExtraArgs(payment.getExtraArgs() != null ? payment.getExtraArgs() : Map.of());
					dto.setReceiptId(payment.getReceiptId());
					dto.setReceiptUrl(payment.getReceiptUrl());
					return dto;
				}, (v1, v2) -> v1 // keep first if duplicate keys
		));
	}

	@Override
	public ServiceResponse settlePayment(SettlePaymentRequestDto requestDto) {
		try {
			Optional<TraineeCourseEnrollment> enrollmentOpt = traineeCourseEnrollmentRepo
					.findById(requestDto.enrollmentId());
			if (enrollmentOpt.isEmpty()) {
				log.warn("❌ Enrollment not found for ID: {}", requestDto.enrollmentId());
				return ResponseBuilder.error("Enrollment not found", ApiResponse.ENROLLMENT_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			Pair<TraineeCourseEnrollmentDto, CourseDto> courseEnrollmentDetails = courseService
					.getByEnrollmentId(requestDto.enrollmentId());

			if (courseEnrollmentDetails == null || courseEnrollmentDetails.getLeft() == null
					|| courseEnrollmentDetails.getRight() == null) {
				return ResponseBuilder.error("Unable to fetch enrollment and course details",
						ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			TraineeCourseEnrollmentDto enrollmentDto = courseEnrollmentDetails.getLeft();
			CourseDto courseDto = courseEnrollmentDetails.getRight();

			if (enrollmentDto.getDuesOn() == null) {
				return ResponseBuilder.error("No dues date found for the enrollment.", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.EXPECTATION_FAILED);
			}

			TraineeCourseEnrollment enrollment = enrollmentOpt.get();
			LocalDate today = LocalDate.now();

			// Apply registration discount
			if (requestDto.registrationFeeDiscount() != null && requestDto.registrationFeeDiscount() > 0) {
				PaymentLedger discountLedger = ledgerHelper.createDiscountLedger(courseDto.getAcademyId(),
						courseDto.getId(), enrollment, requestDto.registrationFeeDiscount(), today,
						PaymentCategory.REGISTRATION_FEE);
				ledgerRepository.save(discountLedger);
			}

			// Apply course discount
			if (requestDto.courseFeeDiscount() != null && requestDto.courseFeeDiscount() > 0) {
				enrollment.setDiscountAmount(requestDto.courseFeeDiscount());
				enrollment.setUseForFuture(requestDto.useForFuture());
			}

			// Apply overall discount
			if (requestDto.overallDiscount() != null && requestDto.overallDiscount() > 0) {
				PaymentLedger discountLedger = ledgerHelper.createDiscountLedger(courseDto.getAcademyId(),
						courseDto.getId(), enrollment, requestDto.registrationFeeDiscount(), today,
						PaymentCategory.COURSE_FEE);
				ledgerRepository.save(discountLedger);
			}

//			UserProfile userProfile = enrollment.getTraineeUserProfile();
//			String userId = userProfile.getId();

			// Settle REGISTRATION_FEE
			ServiceResponse regResponse = getPaymentDetails(requestDto.enrollmentId(),
					PaymentCategory.REGISTRATION_FEE);
			if (regResponse.getHttpStatus().is2xxSuccessful()
					&& regResponse.getBody() instanceof PaymentDetailsDto regDetails) {

			} else {
				log.error("❌ Failed to fetch REGISTRATION_FEE details for enrollmentId: {}", requestDto.enrollmentId());
				return ResponseBuilder.error("Failed to fetch registration fee", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			// Settle COURSE_FEE
			ServiceResponse courseResponse = getPaymentDetails(requestDto.enrollmentId(), PaymentCategory.COURSE_FEE);
			if (courseResponse.getHttpStatus().is2xxSuccessful()
					&& courseResponse.getBody() instanceof PaymentDetailsDto courseDetails) {

			} else {
				log.error("❌ Failed to fetch COURSE_FEE details for enrollmentId: {}", requestDto.enrollmentId());
				return ResponseBuilder.error("Failed to fetch course fee", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.NOT_FOUND);
			}

			return ResponseBuilder.success("Payment(s) settled successfully");

		} catch (Exception e) {
			log.error("❌ Exception during payment settlement: {}", e.getMessage(), e);
			return ResponseBuilder.error("Something went wrong while settling payment", ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ServiceResponse initializePayment(String userId, String enrollmentId, InitPaymentDto initPaymentDto) {
		try {
			// 1️⃣ Validate enrollment exists (change this to not throw if needed)
			if (!traineeCourseEnrollmentRepo.existsById(enrollmentId)) {
				return ResponseBuilder.error("Enrollment not found", ErrorCodes.RESOURCE_NOT_FOUND,
						HttpStatus.BAD_REQUEST);
			}

			// 2️⃣ Determine payment category
			PaymentCategory category = initPaymentDto.getPaymentCategory() != null ? initPaymentDto.getPaymentCategory()
					: PaymentCategory.COURSE_FEE;

			// 3️⃣ Get payment details for this enrollment & category
			ServiceResponse serviceResponse = getPaymentDetails(enrollmentId, category);
			PaymentDetailsDto paymentDetailsDto = (PaymentDetailsDto) serviceResponse.getBody();

			if (!Boolean.TRUE.equals(paymentDetailsDto.getIsDue())) {
				return ResponseBuilder.error("Invalid Payment: No due", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			// 4️⃣ Validate requested payment installment date
			String expectedInstallmentDate = paymentDetailsDto.getNextDueAt();
			String requestedInstallmentDate = initPaymentDto.getPaymentInstallmentDate();

			if (requestedInstallmentDate == null
					|| !expectedInstallmentDate.equalsIgnoreCase(requestedInstallmentDate)) {
				String invalidDateMessage = String.format(
						"Invalid Payment Installment date: Expected %s, but received %s", expectedInstallmentDate,
						requestedInstallmentDate != null ? requestedInstallmentDate : "null");
				return ResponseBuilder.error(invalidDateMessage, ErrorCodes.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
			}

			// 5️⃣ Validate payment amount
			Long pendingAmount = paymentDetailsDto.getPendingAmount();
			Long totalPayableAmount = paymentDetailsDto.getTotalPayableAmount();
			Long requestedAmount = initPaymentDto.getAmount();

			log.info("🔍 Validating payment: requestedAmount = {}, pendingAmount = {}, totalPayableAmount = {}",
					requestedAmount, pendingAmount, totalPayableAmount);

			if (requestedAmount == null || requestedAmount <= 0) {
				String invalidZeroAmountMessage = "Invalid Payment Amount: Amount must be greater than zero.";
				log.warn("❌ Validation failed: {}", invalidZeroAmountMessage);
				return ResponseBuilder.error(invalidZeroAmountMessage, ErrorCodes.RESOURCE_VALIDATION_FAILED,
						HttpStatus.BAD_REQUEST);
			}

			// ✅ Validate requested amount must match pending amount exactly
			if (requestedAmount.compareTo(pendingAmount) < 0) {
				String message = String.format(
						"Invalid Payment Amount: Requested amount (%d) must be greater than or equal to pending amount (%d). Partial payments are not allowed.",
						requestedAmount, pendingAmount);
				log.warn("❌ Validation failed: {}", message);
				return ResponseBuilder.error(message, ErrorCodes.RESOURCE_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
			}

			// ✅ Sanity check: requested amount should never exceed total payable amount
			if (requestedAmount > totalPayableAmount) {
				String invalidTotalPayableMessage = String.format(
						"Invalid Payment Amount: Requested amount (%d) cannot exceed total payable amount (%d).",
						requestedAmount, totalPayableAmount);
				log.warn("❌ Validation failed: {}", invalidTotalPayableMessage);
				return ResponseBuilder.error(invalidTotalPayableMessage, ErrorCodes.RESOURCE_VALIDATION_FAILED,
						HttpStatus.BAD_REQUEST);
			}

			log.info("✅ Payment validation passed for amount {}", requestedAmount);

			// 6️⃣ Build Payment entity
			Payment payment = buildPayment(enrollmentId, initPaymentDto);

			// 7️⃣ Set initiator & category
			UserProfile userProfile = new UserProfile();
			userProfile.setId(userId);
			payment.setPaymentInitiatedByUserProfile(userProfile);
			payment.setPaymentCategory(category);

			// 8️⃣ Save payment
			Payment savedPayment = paymentRepo.save(payment);

			// 9️⃣ Return success response
			return ResponseBuilder.success(modelMapper.map(savedPayment, PaymentDto.class),
					ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception e) {
			// Fallback catch, in case something unexpected happens
			return ResponseBuilder.error("Unexpected error during payment initialization",
					ErrorCodes.UNEXPECTED_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private Payment buildPayment(String enrollmentId, InitPaymentDto initPaymentDto) {
		Payment payment = modelMapper.map(initPaymentDto, Payment.class);
		payment.setId(UUID.randomUUID().toString());
		payment.setTraineeCourseEnrollment(TraineeCourseEnrollment.builder().id(enrollmentId).build());
		payment.setPaymentStatus(PaymentStatus.PENDING);
		payment.setCreatedAt(Timestamp.from(Instant.now()));
		payment.setPaymentMode(initPaymentDto.getPaymentMode());
		return payment;
	}

	// Extracted helper methods for better organization and reusability

	private PaymentLedgerDto createLedgerDto(String userId, String enrollmentId, String paymentId, String academyId,
			String courseId, PaymentCategory category, double amount, PaymentLedger.LedgerType ledgerType,
			PaymentLedger.PaymentEntryType entryType, LocalDate effectiveDate, String notes, String description) {
		PaymentLedgerDto ledgerDto = new PaymentLedgerDto();
		ledgerDto.setUserId(userId);
		ledgerDto.setEnrollmentId(enrollmentId);
		ledgerDto.setPaymentId(paymentId);
		ledgerDto.setAcademyId(academyId);
		ledgerDto.setProgramId(courseId);
		ledgerDto.setLedgerType(ledgerType);
		ledgerDto.setEntryType(entryType);
		ledgerDto.setEntryStatus(PaymentLedger.PaymentEntryStatus.PENDING);
		ledgerDto.setCategory(category);
		ledgerDto.setAmount(amount);
		ledgerDto.setRemainingAmount(amount);
		ledgerDto.setIsReversal(false);
		ledgerDto.setIsLocked(false);
		ledgerDto.setEffectiveDate(effectiveDate);
		ledgerDto.setNotes(notes);
		ledgerDto.setDescription(description);
		ledgerDto.setCreatedById(userId);
		return ledgerDto;
	}

	private PaymentLedger buildLedgerFromDto(Payment payment, UserProfile userProfile, PaymentLedgerDto dto,
			UserProfile createdBy) {
		PaymentLedger ledger = new PaymentLedger();
		ledger.setPayment(payment);
		// ✅ Add Academy
		if (dto.getAcademyId() != null) {
			ledger.setAcademy(Academy.builder().id(dto.getAcademyId()).build());
		}
		// ✅ Add Program
		if (dto.getProgramId() != null) {
			ledger.setProgram(Course.builder().id(dto.getProgramId()).build());
		}
		ledger.setEnrollment(TraineeCourseEnrollment.builder().id(dto.getEnrollmentId()).build());
		ledger.setUser(userProfile);
		ledger.setLedgerType(dto.getLedgerType());
		ledger.setEntryType(dto.getEntryType());
		ledger.setEntryStatus(dto.getEntryStatus());
		ledger.setCategory(dto.getCategory());
		ledger.setAmount(dto.getAmount());
		ledger.setRemainingAmount(dto.getRemainingAmount());
		ledger.setEffectiveDate(dto.getEffectiveDate());
		ledger.setIsReversal(dto.getIsReversal());
		ledger.setIsLocked(dto.getIsLocked());
		ledger.setNotes(dto.getNotes());
		ledger.setDescription(dto.getDescription());
		ledger.setCreatedBy(createdBy);
		return ledger;
	}

	private ServiceResponse validatePaymentUpdate(String enrollmentId, String paymentId, Payment payment,
			PaymentDetailsDto paymentDetailsDto) {
		// Validate enrollment exists
		if (!traineeCourseEnrollmentRepo.existsById(enrollmentId)) {
			return ResponseBuilder.error("Enrollment not found", ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.BAD_REQUEST);
		}

		// Validate payment exists
		if (payment == null) {
			return ResponseBuilder.error("Payment not found", ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.BAD_REQUEST);
		}

		// Validate installment date matches
		// if
		// (!paymentDetailsDto.getNextDueAt().equalsIgnoreCase(payment.getPaymentInstallmentDate()))
		// {
		// return ResponseBuilder.error("Invalid Payment Installment date: " +
		// payment.getPaymentInstallmentDate(),
		// ErrorCodes.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
		// }

		// ✅ All good
		return ResponseBuilder.success("Validation passed", ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);
	}

	private void updatePaymentFields(Payment payment, UpdatePaymentDto updatePaymentDto) {
		payment.setUpdatedAt(Timestamp.from(Instant.now()));
		payment.setPaymentMode(updatePaymentDto.getPaymentMode());
		payment.setPaymentStatus(updatePaymentDto.getPaymentStatus());
		payment.setExternalTransactionId(updatePaymentDto.getExternalTransactionId());
		payment.setTransactionTime(updatePaymentDto.getTransactionTime());
		payment.setExtraArgs(updatePaymentDto.getExtraArgs());
	}

	private void generateAndSetReceipt(Payment payment, String enrollmentId, boolean dataMigrate) {
		payment.setReceiptId(ChartUtils.generateReceiptNumber());

		LocalDateTime localDateTime = dataMigrate ? payment.getCreatedAt().toLocalDateTime() : LocalDateTime.now();

		ReceiptDto receipt = new ReceiptDto(
				payment.getTraineeCourseEnrollment().getTraineeUserProfile().getDisplayName(),
				"Playmo Technologies Pvt. Ltd.", localDateTime, "receipts/logo.png",
				Collections.singletonList(new ReceiptDto.TransactionItem(
						"Fee for period: " + payment.getPaymentInstallmentDate(), payment.getAmount())),
				payment.getAmount(), payment.getReceiptId());

		try {
			byte[] pdfBytes = pdfService.generateReceiptPdf(receipt);
			String prefix = "receipts/" + enrollmentId + "/" + UUID.randomUUID() + ".pdf";
			storageService.upload(paymentsBucket, prefix, pdfBytes, "application/pdf");
			payment.setReceiptUrl(paymentsBaseUrl + prefix);
		} catch (Exception e) {
			log.error("Error while generating receipt", e);
		}
	}

	private List<PaymentLedger> getPendingLedgers(String enrollmentId, PaymentCategory category) {
		return ledgerRepository.findByEnrollment_Id(enrollmentId).stream()
				.filter(ledger -> ledger.getCategory() == category
						&& (ledger.getEntryType() == PaymentLedger.PaymentEntryType.PAST_DUE
								|| ledger.getEntryType() == PaymentLedger.PaymentEntryType.REGISTRATION)
						&& (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
								|| ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
				.collect(Collectors.toList());
	}

	private double applyPaymentToLedger(PaymentLedger ledger, double availableAmount) {
		double ledgerRemaining = ledger.getRemainingAmount();
		double applied = Math.min(availableAmount, ledgerRemaining);

		double newRemaining = Math.max(0, ledgerRemaining - applied);
		ledger.setRemainingAmount(newRemaining);

		if (newRemaining <= 0) {
			ledger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
		} else {
			ledger.setEntryStatus(PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED);
		}

		return applied;
	}

	private double processPastDueLedgers(List<PaymentLedger> pendingLedgers, double remainingAmount,
			List<PaymentLedger> updatedLedgers) {
		// 1️⃣ Sort ledgers by effective date in ascending order (oldest first)
		List<PaymentLedger> sortedLedgers = pendingLedgers.stream()
				.sorted(Comparator.comparing(PaymentLedger::getEffectiveDate)) // Assuming getEffectiveDate() returns
																				// LocalDate
				.collect(Collectors.toList());

		// 2️⃣ Apply payments FIFO based on effective date
		for (PaymentLedger ledger : sortedLedgers) {
			if (remainingAmount <= 0)
				break;

			double applied = applyPaymentToLedger(ledger, remainingAmount);
			remainingAmount -= applied;
			updatedLedgers.add(ledger);
		}

		return remainingAmount;
	}

	private boolean hasCurrentDueLedger(List<PaymentLedger> pendingLedgers, String paymentInstallmentDate) {
		return pendingLedgers.stream()
				.anyMatch(ledger -> ledger.getEntryType() == PaymentLedger.PaymentEntryType.PAYMENT
						&& ledger.getEffectiveDate().equals(LocalDate.parse(paymentInstallmentDate)));
	}

	private PaymentLedger createCurrentDueLedger(Payment payment, String enrollmentId, String academyId,
			String courseId, PaymentCategory category, double appliedAmount) {

		PaymentLedgerDto currentDueLedgerDto = createLedgerDto(
				payment.getTraineeCourseEnrollment().getTraineeUserProfile().getId(), enrollmentId, payment.getId(),
				academyId, courseId, category, appliedAmount, PaymentLedger.LedgerType.DEBIT,
				PaymentLedger.PaymentEntryType.PAYMENT, LocalDate.parse(payment.getPaymentInstallmentDate()),
				"Current installment payment", "Payment for installment: " + payment.getPaymentInstallmentDate());

		return buildLedgerFromDto(payment, payment.getTraineeCourseEnrollment().getTraineeUserProfile(),
				currentDueLedgerDto, payment.getPaymentInitiatedByUserProfile());
	}

	private double processCurrentDueLedger(Payment payment, String enrollmentId, String academyId, String courseId,
			PaymentCategory category, PaymentDetailsDto paymentDetailsDto, List<PaymentLedger> pendingLedgers,
			double remainingAmount, List<PaymentLedger> updatedLedgers) {

		if (remainingAmount <= 0)
			return remainingAmount;

		if (!hasCurrentDueLedger(pendingLedgers, payment.getPaymentInstallmentDate())) {
			double currentDueAmount = paymentDetailsDto.getCurrentDue();

			if (currentDueAmount > 0) {
				double applied = Math.min(currentDueAmount, remainingAmount);

				PaymentLedger currentDueLedger = createCurrentDueLedger(payment, enrollmentId, academyId, courseId,
						category, applied); // Only the paid portion goes in

				// Set remaining amount to 0 in ledger
				currentDueLedger.setRemainingAmount(0.0);
				currentDueLedger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);

				updatedLedgers.add(currentDueLedger);

				// Decrease remainingAmount by what was applied
				remainingAmount -= applied;
			}
		}

		return remainingAmount; // Whatever is left after current due, will be used elsewhere (like advance)
	}

	private PaymentLedger createAdvanceLedger(Payment payment, String enrollmentId, String academyId, String courseId,
			PaymentCategory category, double remainingAmount, boolean fullyPaid) {
		PaymentLedgerDto advanceLedgerDto = createLedgerDto(
				payment.getTraineeCourseEnrollment().getTraineeUserProfile().getId(), enrollmentId, payment.getId(),
				academyId, courseId, category, remainingAmount, PaymentLedger.LedgerType.DEBIT,
				PaymentLedger.PaymentEntryType.ADVANCE, LocalDate.parse(payment.getPaymentInstallmentDate()),
				"Advance payment recorded", "Extra payment portion");

		PaymentLedger advanceLedger = buildLedgerFromDto(payment,
				payment.getTraineeCourseEnrollment().getTraineeUserProfile(), advanceLedgerDto,
				payment.getPaymentInitiatedByUserProfile());
		if (Boolean.TRUE.equals(fullyPaid)) {
			advanceLedger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
			advanceLedger.setRemainingAmount(0.0);
		} else {
			advanceLedger.setEntryStatus(PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED);
			advanceLedger.setRemainingAmount(0.0);
		}
		return advanceLedger;
	}

	private void processAdvancePayment(Payment payment, String enrollmentId, String academyId, String courseId,
			PaymentCategory category, double remainingAmount, List<PaymentLedger> updatedLedgers, boolean fullyPaid) {
		if (remainingAmount > 0) {
			PaymentLedger advanceLedger = createAdvanceLedger(payment, enrollmentId, academyId, courseId, category,
					remainingAmount, fullyPaid);
			updatedLedgers.add(advanceLedger);
		}
	}

	private void processCreditPayment(Payment payment, String enrollmentId, String academyId, String courseId,
			PaymentCategory category, List<PaymentLedger> updatedLedgers) {
		PaymentLedger creditLedger = createCreditPaymentLedger(payment, enrollmentId, academyId, courseId, category);
		updatedLedgers.add(creditLedger);
	}

	private PaymentLedger createCreditPaymentLedger(Payment payment, String enrollmentId, String academyId,
			String courseId, PaymentCategory category) {

		PaymentLedgerDto creditLedgerDto = createLedgerDto(
				payment.getTraineeCourseEnrollment().getTraineeUserProfile().getId(), enrollmentId, payment.getId(),
				academyId, courseId, category, payment.getAmount().doubleValue(), PaymentLedger.LedgerType.CREDIT,
				PaymentLedger.PaymentEntryType.PAYMENT, LocalDate.parse(payment.getPaymentInstallmentDate()),
				"Payment recorded successfully", "Amount received");

		PaymentLedger creditLedger = buildLedgerFromDto(payment,
				payment.getTraineeCourseEnrollment().getTraineeUserProfile(), creditLedgerDto,
				payment.getPaymentInitiatedByUserProfile());

		creditLedger.setRemainingAmount(0.0);
		creditLedger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);

		return creditLedger;
	}

	private void clearPendingCreditsAndDiscounts(String enrollmentId, List<PaymentLedger> updatedLedgers) {
		List<PaymentLedger> paymentsLedgers = ledgerRepository.findByEnrollment_Id(enrollmentId);

		paymentsLedgers.stream()
				.filter(ledger -> (ledger.getEntryType() == PaymentLedger.PaymentEntryType.ADVANCE
						|| ledger.getEntryType() == PaymentLedger.PaymentEntryType.DISCOUNT)
						&& (ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PENDING
								|| ledger.getEntryStatus() == PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED))
				.forEach(ledger -> {
					ledger.setRemainingAmount(0.0);
					ledger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
					updatedLedgers.add(ledger);
				});
	}

	private void processDiscountLedger(Payment payment, String enrollmentId, String academyId, String courseId,
			PaymentCategory category, double discountAmount, List<PaymentLedger> updatedLedgers) {
		if (discountAmount > 0) {
			PaymentLedger discountLedger = createDiscountLedger(payment, enrollmentId, academyId, courseId, category,
					discountAmount);
			updatedLedgers.add(discountLedger);
		}
	}

	private PaymentLedger createDiscountLedger(Payment payment, String enrollmentId, String academyId, String courseId,
			PaymentCategory category, double discountAmount) {

		PaymentLedgerDto discountLedgerDto = createLedgerDto(
				payment.getTraineeCourseEnrollment().getTraineeUserProfile().getId(), enrollmentId, payment.getId(),
				academyId, courseId, category, discountAmount, PaymentLedger.LedgerType.CREDIT,
				PaymentLedger.PaymentEntryType.DISCOUNT, LocalDate.parse(payment.getPaymentInstallmentDate()),
				"Discount recorded", "Discount applied to payment");

		PaymentLedger discountLedger = buildLedgerFromDto(payment,
				payment.getTraineeCourseEnrollment().getTraineeUserProfile(), discountLedgerDto,
				payment.getPaymentInitiatedByUserProfile());
		discountLedger.setEntryStatus(PaymentLedger.PaymentEntryStatus.SETTLED);
		discountLedger.setRemainingAmount(0.0);

		return discountLedger;
	}

	private void sendPaymentNotifications(Payment savedPayment, String enrollmentId) {
		try {
			Pair<TraineeCourseEnrollmentDto, CourseDto> enrollmentPair = courseService.getByEnrollmentId(enrollmentId);

			Map<String, String> extraArgs = Map.of("academyId", enrollmentPair.getRight().getAcademyId(),
					"traineeUserId", enrollmentPair.getLeft().getUserProfile().getId(), "courseId",
					enrollmentPair.getRight().getId());
			CompletableFuture.runAsync(() -> {
				try {
					// Send trainee notification
					sendTraineeNotification(savedPayment, enrollmentPair, extraArgs);

					// Send coach notification
					sendCoachNotification(savedPayment, enrollmentPair, extraArgs);

				} catch (Exception e) {
					log.error("Failed to send push notification for payment update.", e);
				}
			});
		} catch (ResourceException e) {
			log.error("Failed to send push notification for payment update.", e.getMessage());
		} catch (Exception e) {
			log.error("Failed to send push notification for payment update.", e.getMessage());
		}
	}

	private void sendTraineeNotification(Payment savedPayment,
			Pair<TraineeCourseEnrollmentDto, CourseDto> enrollmentPair, Map<String, String> extraArgs) {
		String title = savedPayment.getPaymentStatus() == PaymentStatus.SUCCESS ? PAYMENT_SUCCESS_TITLE
				: PAYMENT_FAILED_TITLE;
		String message = String
				.format(savedPayment.getPaymentStatus() == PaymentStatus.SUCCESS ? PAYMENT_SUCCESS_MESSAGE
						: PAYMENT_FAILED_MESSAGE, savedPayment.getAmount());

		pushNotificationService.sendMessageToPushToken(
				enrollmentPair.getLeft().getUserProfile().getAndroidFcmPushToken(), NotificationType.LIVE_NOTIFICATION,
				title, message, TRAINEE_PAYMENT_SCREEN, CtaType.SCREEN, extraArgs);

		pushNotificationService.addNotification(List.of(enrollmentPair.getLeft().getUserProfile().getId()), message,
				CtaType.SCREEN, TRAINEE_PAYMENT_SCREEN, extraArgs);
	}

	private void sendCoachNotification(Payment savedPayment, Pair<TraineeCourseEnrollmentDto, CourseDto> enrollmentPair,
			Map<String, String> extraArgs) throws ResourceException {
		Map<String, UserProfileDto> userProfiles = userProfileService
				.getUserProfileByIds(List.of(enrollmentPair.getLeft().getUserProfile().getId(),
						enrollmentPair.getRight().getCoachUserId()))
				.stream().collect(Collectors.toMap(UserProfileDto::getId, u -> u));

		String coachToken = userProfiles.get(enrollmentPair.getRight().getCoachUserId()).getAndroidFcmPushToken();
		String traineeDisplayName = userProfiles.get(enrollmentPair.getLeft().getUserProfile().getId())
				.getDisplayName();

		pushNotificationService.sendMessageToPushToken(coachToken, NotificationType.LIVE_NOTIFICATION,
				"Payment Received", "Payment received from " + traineeDisplayName, COACH_PAYMENT_SCREEN, CtaType.SCREEN,
				extraArgs);

		pushNotificationService.addNotification(
				List.of(userProfiles.get(enrollmentPair.getRight().getCoachUserId()).getId()),
				"Payment received from " + traineeDisplayName, CtaType.SCREEN, COACH_PAYMENT_SCREEN, extraArgs);
	}

	// PaymentProcessingResult wrapper class
	public class PaymentProcessingResult {
		private boolean currentDueLedgerProcessed;
		private double remainingAmount;
		private List<PaymentLedger> updatedLedgers;

		public PaymentProcessingResult(boolean currentDueLedgerProcessed, double remainingAmount,
				List<PaymentLedger> updatedLedgers) {
			this.currentDueLedgerProcessed = currentDueLedgerProcessed;
			this.remainingAmount = remainingAmount;
			this.updatedLedgers = updatedLedgers;
		}

		public boolean isCurrentDueLedgerProcessed() {
			return currentDueLedgerProcessed;
		}

		public double getRemainingAmount() {
			return remainingAmount;
		}

		public List<PaymentLedger> getUpdatedLedgers() {
			return updatedLedgers;
		}
	}

	private void processPaymentLedgers(Payment payment, String enrollmentId, String academyId, String courseId,
			PaymentCategory category, PaymentDetailsDto paymentDetailsDto) {

		log.info("🔁 Starting payment ledger processing for paymentId={}, enrollmentId={}, category={}",
				payment.getId(), enrollmentId, category);

		double remainingAmount = payment.getAmount();
		Long totalPayable = paymentDetailsDto.getTotalPayableAmount();
		boolean fullyPaid = Double.compare(remainingAmount, totalPayable) == 0;

		log.info("💰 Initial payment amount: {}, Total payable: {}, Fully paid: {}", remainingAmount, totalPayable,
				fullyPaid);

		List<PaymentLedger> pendingLedgers = getPendingLedgers(enrollmentId, category);
		log.info("📄 Found {} pending ledgers for enrollmentId={} and category={}", pendingLedgers.size(), enrollmentId,
				category);

		List<PaymentLedger> updatedLedgers = new ArrayList<>();

		// Process past due ledgers first (FIFO)
		remainingAmount = processPastDueLedgers(pendingLedgers, remainingAmount, updatedLedgers);
		log.info("🔁 Remaining amount after processing past dues: {}", remainingAmount);

		// Process current due ledger
		remainingAmount = processCurrentDueLedger(payment, enrollmentId, academyId, courseId, category,
				paymentDetailsDto, pendingLedgers, remainingAmount, updatedLedgers);
		log.info("🔁 Remaining amount after processing current due: {}", remainingAmount);

		// Process advance payment if any remaining amount
		processAdvancePayment(payment, enrollmentId, academyId, courseId, category, remainingAmount, updatedLedgers,
				fullyPaid);
		log.info("✅ Advance payment (if any) processed. Remaining amount: {}", remainingAmount);

		// ✅ Clear any pending advances or discounts
		clearPendingCreditsAndDiscounts(enrollmentId, updatedLedgers);
		log.info("🧹 Cleared any pending credits or discounts for enrollmentId={}", enrollmentId);

		// Process discount if any new discount amount is present
		if (paymentDetailsDto.getDiscountAmount() != null && paymentDetailsDto.getDiscountAmount() > 0) {
			processDiscountLedger(payment, enrollmentId, academyId, courseId, category,
					paymentDetailsDto.getDiscountAmount(), updatedLedgers);
			log.info("🎯 Processed new discount of amount {}", paymentDetailsDto.getDiscountAmount());
		}

		processCreditPayment(payment, enrollmentId, academyId, courseId, category, updatedLedgers);
		log.info("💳 Processed any credit payments");

		// Save all updated ledgers
		if (!updatedLedgers.isEmpty()) {
			ledgerRepository.saveAll(updatedLedgers);
			log.info("📦 Saved {} updated ledger entries to the database", updatedLedgers.size());
		} else {
			log.info("ℹ️ No ledger entries to save");
		}

		log.info("✅ Finished processing payment ledger for paymentId={}", payment.getId());
	}

	@Override
	public ServiceResponse updatePayment(String paymentId, String enrollmentId, UpdatePaymentDto updatePaymentDto,
			boolean dataMigrate) {
		try {
			// Get payment
			Payment payment = paymentRepo.findByIdAndTraineeCourseEnrollment_Id(paymentId, enrollmentId).orElse(null);

			// Determine payment category
			PaymentCategory category = payment.getPaymentCategory() != null ? payment.getPaymentCategory() : null;

			if (ObjectUtils.isEmpty(category)) {
				log.error("category is: {}", category);
				return ResponseBuilder.error("Payment category is null", ErrorCodes.INVALID_REQUEST,
						HttpStatus.BAD_REQUEST);
			}

			log.info("category is: {}", category.name());

			// Get enrollment and course details
			Pair<TraineeCourseEnrollmentDto, CourseDto> enrollmentPair = courseService.getByEnrollmentId(enrollmentId);
			String academyId = enrollmentPair.getRight().getAcademyId();
			String courseId = enrollmentPair.getRight().getId();

			// Get payment details
			ServiceResponse serviceResponse = getPaymentDetails(enrollmentId, category);
			PaymentDetailsDto paymentDetailsDto = (PaymentDetailsDto) serviceResponse.getBody();

			if (!dataMigrate) {
				if (!Boolean.TRUE.equals(paymentDetailsDto.getIsDue())) {
					return ResponseBuilder.error("Invalid Payment: No due", ErrorCodes.INVALID_REQUEST,
							HttpStatus.BAD_REQUEST);
				}
			}

			// Validate payment
			ServiceResponse validationResponse = validatePaymentUpdate(enrollmentId, paymentId, payment,
					paymentDetailsDto);
			if (!validationResponse.getHttpStatus().is2xxSuccessful()) {
				return validationResponse;
			}

			// Update fields
			updatePaymentFields(payment, updatePaymentDto);

			if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
				generateAndSetReceipt(payment, enrollmentId, dataMigrate);

				processPaymentLedgers(payment, enrollmentId, academyId, courseId, category, paymentDetailsDto);

				// Only update enrollment details if current due ledger was actually processed
				if (category == PaymentCategory.COURSE_FEE) {

					String nextDueAtStr = paymentDetailsDto.getNextDueAt();
					LocalDate nextDueAt = nextDueAtStr != null ? LocalDate.parse(nextDueAtStr) : null;

					// Pair<LocalDate, Long> dueAtAndAmount =
					// calculateNextDueAt(paymentDetailsDto.getPendingAmount(),
					// nextDueAt, paymentDetailsDto.getFuturePayments(), payment.getAmount());
					//
					// LocalDate updatedNextDueAt = dueAtAndAmount.getLeft();
					// Long leftoverAmount = dueAtAndAmount.getRight();

					Pair<LocalDate, Long> dueInfo = calculateNextDueAt(paymentDetailsDto.getTotalPastDues(),
							paymentDetailsDto.getCurrentDue(), nextDueAt, paymentDetailsDto.getFuturePayments(),
							payment.getAmount());

					LocalDate updatedNextDueAt = dueInfo.getLeft();
					Long leftoverAmount = dueInfo.getRight();

					TraineeCourseEnrollment enrollment = traineeCourseEnrollmentRepo.findById(enrollmentId)
							.orElse(null);

					if (enrollment != null) {

						enrollment.setDuesOn(updatedNextDueAt);
						enrollment.setFinalDueAmount(leftoverAmount + enrollment.getDiscountAmount());

						Boolean useForFuture = enrollment.getUseForFuture();
						if (useForFuture == null || !useForFuture) {
							enrollment.setDiscountAmount(0L);
						}

						traineeCourseEnrollmentRepo.save(enrollment);
					}
				}
			}

			Payment savedPayment = paymentRepo.save(payment);

			sendPaymentNotifications(savedPayment, enrollmentId);

			return ResponseBuilder.success(modelMapper.map(savedPayment, PaymentDto.class),
					ApiResponse.LIST_FETCHED_SUCCESSFULLY, HttpStatus.OK);

		} catch (Exception ex) {
			return ResponseBuilder.error("Unexpected error during payment update", ErrorCodes.UNEXPECTED_FAILURE,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private Pair<LocalDate, Long> calculateNextDueAt(Long totalPastDues, Long currentDue, LocalDate currentDueDate,
			Map<String, Long> futurePayments, long paidAmount) {

		long remaining = paidAmount;

		// 1️⃣ Subtract total past dues
		if (totalPastDues != null && totalPastDues > 0) {
			if (remaining >= totalPastDues) {
				remaining -= totalPastDues;
			} else {
				// Not enough to reach current due
				return Pair.of(currentDueDate, currentDue); // untouched current due
			}
		}

		// 2️⃣ Handle current due
		if (currentDue != null && currentDue > 0) {
			if (remaining >= currentDue) {
				remaining -= currentDue; // current due fully paid
			} else if (remaining > 0) {
				// Partially paid current due
				long finalDueAmt = currentDue - remaining;
				return Pair.of(currentDueDate, finalDueAmt);
			} else {
				// No remaining amount → untouched current due
				return Pair.of(currentDueDate, currentDue);
			}
		}

		// 3️⃣ If current due is null or zero
		if (currentDue == null || currentDue == 0L) {
			return Pair.of(currentDueDate, 0L);
		}

		// 4️⃣ Apply to future payments
		if (futurePayments != null && !futurePayments.isEmpty()) {
			TreeMap<LocalDate, Long> sortedFuture = new TreeMap<>();
			for (Map.Entry<String, Long> entry : futurePayments.entrySet()) {
				sortedFuture.put(LocalDate.parse(entry.getKey()), entry.getValue());
			}

			for (Map.Entry<LocalDate, Long> entry : sortedFuture.entrySet()) {
				LocalDate dueDate = entry.getKey();
				long dueAmount = entry.getValue();

				if (remaining >= dueAmount) {
					remaining -= dueAmount;
				} else {
					long finalDueAmt = dueAmount - remaining;
					return Pair.of(dueDate, finalDueAmt);
				}
			}

			// All future dues fully paid
			return Pair.of(sortedFuture.lastKey(), 0L);
		}

		// 5️⃣ No future dues → all covered
		return Pair.of(currentDueDate, 0L);
	}

//	private Pair<LocalDate, Long> calculateNextDueAt(Long pendingAmount, LocalDate oldestPendingDate,
//			Map<String, Long> futurePayments, long paidAmount) {
//		long remaining = paidAmount;
//
//		// 1️⃣ Subtract pending dues first
//		if (pendingAmount != null && pendingAmount > 0) {
//			if (remaining >= pendingAmount) {
//				remaining -= pendingAmount;
//			} else {
//				return Pair.of(oldestPendingDate, remaining); // partial pending
//			}
//		}
//
//		// 2️⃣ Parse and sort future payments
//		TreeMap<LocalDate, Long> sortedFuture = new TreeMap<>();
//		if (futurePayments != null) {
//			for (Map.Entry<String, Long> entry : futurePayments.entrySet()) {
//				sortedFuture.put(LocalDate.parse(entry.getKey()), entry.getValue());
//			}
//		}
//
//		// 3️⃣ Apply payments to future dues
//		for (Map.Entry<LocalDate, Long> entry : sortedFuture.entrySet()) {
//			LocalDate dueDate = entry.getKey();
//			long dueAmount = entry.getValue();
//
//			if (remaining >= dueAmount) {
//				remaining -= dueAmount;
//			} else {
//				return Pair.of(dueDate, remaining); // partial payment on this date
//			}
//		}
//
//		// 4️⃣ All dues covered
//		LocalDate lastDate = sortedFuture.isEmpty() ? oldestPendingDate : sortedFuture.lastKey();
//		return Pair.of(lastDate, 0L);
//	}

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

			ServiceResponse serviceResponse = getPaymentDetails(enrollment.getId(), category);
			PaymentDetailsDto paymentDetailsDto = (PaymentDetailsDto) serviceResponse.getBody();

			// ✅ Skip if payment details are missing
			if (paymentDetailsDto == null) {
				log.warn("No payment details found for enrollment ID {}", enrollment.getId());
				continue;
			}

			coursePaymentDetailsDto.setEnrollmentId(enrollment.getId());
			coursePaymentDetailsDto.setTraineeUserId(enrollment.getTraineeUserProfile().getId());
			coursePaymentDetailsDto.setPaymentHistory(paymentDetailsDto.getPaymentHistory());
			coursePaymentDetailsDto.setIsInProgress(paymentDetailsDto.getIsInProgress());

			if (userProfileDtoMap.containsKey(enrollment.getTraineeUserProfile().getId())) {
				coursePaymentDetailsDto.setUserProfile(modelMapper.map(
						userProfileDtoMap.get(enrollment.getTraineeUserProfile().getId()), UserProfileMinDto.class));
			}

			if (Boolean.TRUE.equals(paymentDetailsDto.getIsDue())) {
				coursePaymentDetailsDto.setDueAmount(paymentDetailsDto.getPendingAmount());
				coursePaymentDetailsDto.setCurrency(paymentDetailsDto.getCurrency());
				coursePaymentDetailsDto.setIsDuePending(true);
			}

			coursePaymentDetailsDtos.add(coursePaymentDetailsDto);
		}

		return sort(coursePaymentDetailsDtos);
	}

	private List<CoursePaymentDetailsDto> sort(List<CoursePaymentDetailsDto> coursePaymentDetailsDtos) {
		return coursePaymentDetailsDtos.stream().sorted(Comparator
				.comparing(coursePaymentDetailsDto -> coursePaymentDetailsDto.getUserProfile().getDisplayName()))
				.toList();
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
	public List<PaymentDetailsDto> getPaymentDetailsByAcademy(String academyId, List<String> courseIds,
			PaymentCategory category) {
		try {
			academyService.getAcademyById(academyId);

			List<TraineeCourseEnrollment> traineeCourseEnrollments = courseService.getEnrollmentsByAcademyId(academyId);
			if (!CollectionUtils.isEmpty(courseIds)) {
				traineeCourseEnrollments = traineeCourseEnrollments.stream().filter(
						traineeCourseEnrollment -> courseIds.contains(traineeCourseEnrollment.getCourse().getId()))
						.toList();
			}
			if (CollectionUtils.isEmpty(traineeCourseEnrollments)) {
				return List.of();
			}
			List<PaymentDetailsDto> paymentDetailsDtos = new ArrayList<>();
			for (TraineeCourseEnrollment traineeCourseEnrollment : traineeCourseEnrollments) {

				ServiceResponse serviceResponse = getPaymentDetails(traineeCourseEnrollment.getId(), category);
				PaymentDetailsDto paymentDetailsDto = (PaymentDetailsDto) serviceResponse.getBody();
				if (paymentDetailsDto != null) {
					paymentDetailsDtos.add(paymentDetailsDto);
				}
			}

			return paymentDetailsDtos;
		} catch (ResourceException e) {
			// TODO Auto-generated catch block
			return List.of();
		}
	}

	@Override
	public List<PaymentDueDto> getPaymentDueDto(String academyId, PaymentCategory category) throws ResourceException {

		// ✅ Validate academy exists
		academyService.getAcademyById(academyId);

		// ✅ Fetch payment details
		List<PaymentDetailsDto> paymentDetailsDtos = getPaymentDetailsByAcademy(academyId, new ArrayList<>(), category);

		if (CollectionUtils.isEmpty(paymentDetailsDtos)) {
			return List.of();
		}

		// ✅ Fetch enrollments for payment details
		List<TraineeCourseEnrollmentDto> enrollmentDtos = courseService
				.getByEnrollmentIdsByEID(paymentDetailsDtos.stream().map(PaymentDetailsDto::getEnrollmentId).toList());

		Map<String, TraineeCourseEnrollmentDto> enrollmentDtoMap = enrollmentDtos.stream()
				.collect(Collectors.toMap(TraineeCourseEnrollmentDto::getId, dto -> dto));

		Map<String, PaymentDueDto> paymentDueDtoMap = new HashMap<>();

		for (PaymentDetailsDto paymentDetailsDto : paymentDetailsDtos) {
			if (!Boolean.TRUE.equals(paymentDetailsDto.getIsDue())
					|| !enrollmentDtoMap.containsKey(paymentDetailsDto.getEnrollmentId())) {
				continue;
			}

			TraineeCourseEnrollmentDto enrollmentDto = enrollmentDtoMap.get(paymentDetailsDto.getEnrollmentId());
			String traineeUserId = enrollmentDto.getUserProfile().getId();

			PaymentDueDto paymentDueDto = paymentDueDtoMap.get(traineeUserId);
			if (paymentDueDto == null) {
				paymentDueDto = new PaymentDueDto();
				paymentDueDto.setTraineeUserId(traineeUserId);
				paymentDueDto.setUserProfile(modelMapper.map(enrollmentDto.getUserProfile(), UserProfileMinDto.class));
				paymentDueDto.setPendingAmount("0");
			}

			Long currentPending = Long.parseLong(paymentDueDto.getPendingAmount());
			Long paymentPending = paymentDetailsDto.getPendingAmount() != null ? paymentDetailsDto.getPendingAmount()
					: 0L;

			paymentDueDto.setPendingAmount(String.valueOf(currentPending + paymentPending));

			paymentDueDtoMap.put(traineeUserId, paymentDueDto);
		}

		List<PaymentDueDto> paymentDueDtos = new ArrayList<>(paymentDueDtoMap.values());

		// ✅ Format pending amounts
		paymentDueDtos = paymentDueDtos.stream()
				.peek(dueDto -> dueDto.setPendingAmount(
						CurrencyUtils.formatCurrency(Currency.INR, Long.parseLong(dueDto.getPendingAmount()))))
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

}