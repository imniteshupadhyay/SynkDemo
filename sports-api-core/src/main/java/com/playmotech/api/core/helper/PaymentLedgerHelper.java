package com.playmotech.api.core.helper;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.Payment;
import com.playmotech.api.core.dao_postgres.PaymentLedger;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.InstallmentInfo;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentLedgerHelper {

	private String generateDescription(LedgerCreationRequest request) {
		return switch (request.getEntryType()) {
		case REGISTRATION -> "Course registration fee";
		case PAYMENT -> {
			if (request.getPayment() != null) {
				yield "Payment received - ID: " + request.getPayment().getId();
			}
			yield "Payment received";
		}
		case DISCOUNT -> "Discount applied" + (request.getNotes() != null ? " - " + request.getNotes() : "");
		case ADVANCE -> "Advance payment received";
		case CREDIT -> "Credit balance adjustment";
		case PAST_DUE -> "Past due payment";
		case WAIVER -> "Fee waiver applied" + (request.getNotes() != null ? " - " + request.getNotes() : "");
		case ADJUSTMENT -> "Manual adjustment" + (request.getNotes() != null ? " - " + request.getNotes() : "");
		case REFUND -> "Refund processed" + (request.getNotes() != null ? " - " + request.getNotes() : "");
		case REVERSAL -> "Payment reversal" + (request.getNotes() != null ? " - " + request.getNotes() : "");
		default -> "Payment ledger entry";
		};
	}

	/**
	 * Dynamic ledger creation method that handles all types of ledgers
	 */
	public PaymentLedger createLedger(LedgerCreationRequest request) {
		log.debug("Creating ledger of type: {} for trainee: {}", request.getEntryType(), request.getTraineeUserId());

		PaymentLedger ledger = new PaymentLedger();

		// Set common fields
		ledger.setUser(UserProfile.builder().id(request.getTraineeUserId()).build());
		ledger.setEnrollment(TraineeCourseEnrollment.builder().id(request.getEnrollmentId()).build());
		ledger.setAcademy(Academy.builder().id(request.getAcademyId()).build());
		ledger.setProgram(Course.builder().id(request.getCourseId()).build());
		ledger.setEffectiveDate(request.getEffectiveDate());
		ledger.setLedgerType(request.getLedgerType());
		ledger.setEntryType(request.getEntryType());
		ledger.setEntryStatus(request.getEntryStatus());
		ledger.setCategory(request.getCategory());
		ledger.setIsReversal(request.getIsReversal() != null ? request.getIsReversal() : Boolean.FALSE);
		ledger.setIsLocked(request.getIsLocked() != null ? request.getIsLocked() : Boolean.FALSE);
		ledger.setNotes(request.getNotes());
		ledger.setParentEntryId(request.getParentEntryId());

		// Set payment reference if provided
		if (request.getPayment() != null) {
			ledger.setPayment(request.getPayment());
		}

		ledger.setAmount(request.getAmount().doubleValue());
		ledger.setRemainingAmount(request.getRemainingAmount().doubleValue());

		// Set description based on type
		ledger.setDescription(generateDescription(request));

		// Set audit fields
		if (request.getCreatedBy() != null) {
			ledger.setCreatedBy(UserProfile.builder().id(request.getCreatedBy()).build());
		}
		if (request.getUpdatedBy() != null) {
			ledger.setUpdatedBy(UserProfile.builder().id(request.getUpdatedBy()).build());
		}
		if (request.getApprovedBy() != null) {
			ledger.setApprovedBy(UserProfile.builder().id(request.getApprovedBy()).build());
		}

		log.info("Created {} ledger for trainee {} with amount {}", request.getEntryType(), request.getTraineeUserId(),
				request.getAmount().doubleValue());

		return ledger;
	}

	/**
	 * Create multiple ledgers and return them for batch saving
	 */
	public List<PaymentLedger> createLedgers(List<LedgerCreationRequest> requests) {
		return requests.stream().map(this::createLedger).toList();
	}

	/**
	 * Create registration fee ledger
	 */
	public PaymentLedger createRegistrationFeeLedger(String academyId, String courseId,
			TraineeCourseEnrollment enrollment, CourseDto courseDto) {

		Long regFeeAmount = courseDto.getRegistrationFee() != null ? courseDto.getRegistrationFee() : 0L;
		boolean hasRegFee = regFeeAmount > 0;

		UserProfile traineeUser = enrollment.getTraineeUserProfile();

		LocalDate joiningDate = enrollment.getJoiningDate();

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(traineeUser.getId()).enrollmentId(enrollment.getId()).effectiveDate(joiningDate)
				.ledgerType(PaymentLedger.LedgerType.DEBIT).entryType(PaymentLedger.PaymentEntryType.REGISTRATION)
				.entryStatus(
						hasRegFee ? PaymentLedger.PaymentEntryStatus.PENDING : PaymentLedger.PaymentEntryStatus.SETTLED)
				.category(PaymentCategory.REGISTRATION_FEE).amount(regFeeAmount).remainingAmount(regFeeAmount).build();

		return createLedger(request);
	}

	public PaymentLedger createRegistrationFeeLedger(String academyId, String courseId,
			TraineeCourseEnrollment enrollment, CourseDto courseDto, LocalDate effectiveDate, PaymentCategory category,
			Long amount, Long remainingAmount, String notes, String createdBy,
			PaymentLedger.PaymentEntryStatus status) {

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(enrollment.getTraineeUserProfile().getId()).enrollmentId(enrollment.getId())
				.effectiveDate(effectiveDate).ledgerType(PaymentLedger.LedgerType.DEBIT)
				.entryType(PaymentLedger.PaymentEntryType.REGISTRATION).entryStatus(status).category(category)
				.amount(amount).remainingAmount(remainingAmount).notes(notes).createdBy(createdBy).build();

		return createLedger(request);
	}

	public PaymentLedger createRegistrationCreditFeeLedger(String academyId, String courseId,
			TraineeCourseEnrollment enrollment, CourseDto courseDto, LocalDate effectiveDate, PaymentCategory category,
			Long amount, Long remainingAmount, String notes, String createdBy,
			PaymentLedger.PaymentEntryStatus status) {

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(enrollment.getTraineeUserProfile().getId()).enrollmentId(enrollment.getId())
				.effectiveDate(effectiveDate).ledgerType(PaymentLedger.LedgerType.CREDIT)
				.entryType(PaymentLedger.PaymentEntryType.PAYMENT).entryStatus(status).category(category).amount(amount)
				.remainingAmount(remainingAmount).notes(notes).createdBy(createdBy).build();

		return createLedger(request);
	}

	/**
	 * Create discount ledger entry
	 */
	public PaymentLedger createDiscountLedger(String academyId, String courseId, TraineeCourseEnrollment enrollment,
			Long discountAmount, LocalDate effectiveDate) {

		UserProfile traineeUser = enrollment.getTraineeUserProfile();

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(traineeUser.getId()).enrollmentId(enrollment.getId()).effectiveDate(effectiveDate)
				.ledgerType(PaymentLedger.LedgerType.CREDIT) // CREDIT because discount reduces amount owed
				.entryType(PaymentLedger.PaymentEntryType.DISCOUNT)
				.entryStatus(PaymentLedger.PaymentEntryStatus.PENDING).category(PaymentCategory.COURSE_FEE)
				.amount(discountAmount).remainingAmount(discountAmount) // Discount is fully applied
				.build();

		return createLedger(request);
	}

	public PaymentLedger createDiscountLedger(String academyId, String courseId, TraineeCourseEnrollment enrollment,
			Long discountAmount, LocalDate effectiveDate, PaymentCategory category) {

		UserProfile traineeUser = enrollment.getTraineeUserProfile();

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(traineeUser.getId()).enrollmentId(enrollment.getId()).effectiveDate(effectiveDate)
				.ledgerType(PaymentLedger.LedgerType.CREDIT).entryType(PaymentLedger.PaymentEntryType.DISCOUNT)
				.entryStatus(PaymentLedger.PaymentEntryStatus.PENDING).category(category).amount(discountAmount)
				.remainingAmount(discountAmount).build();

		return createLedger(request);
	}

	/**
	 * Create past due ledger
	 */
	public PaymentLedger createPastDueLedger(String academyId, String courseId, TraineeCourseEnrollment enrollment,
			InstallmentInfo installment) {

		UserProfile traineeUser = enrollment.getTraineeUserProfile();

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(traineeUser.getId()).enrollmentId(enrollment.getId())
				.effectiveDate(installment.getDueDate()).ledgerType(PaymentLedger.LedgerType.DEBIT)
				.entryType(PaymentLedger.PaymentEntryType.PAST_DUE)
				.entryStatus(PaymentLedger.PaymentEntryStatus.PENDING).category(PaymentCategory.COURSE_FEE)
				.amount(installment.getAmount()).remainingAmount(installment.getAmount()).build();

		return createLedger(request);
	}

	/**
	 * Create payment ledger (normal payment received)
	 */
	public PaymentLedger createPaymentLedger(String academyId, String courseId, UserProfile traineeUser,
			TraineeCourseEnrollment enrollment, Long amount, LocalDate effectiveDate, Payment payment,
			PaymentCategory category) {

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(traineeUser.getId()).enrollmentId(enrollment.getId()).effectiveDate(effectiveDate)
				.ledgerType(PaymentLedger.LedgerType.DEBIT).entryType(PaymentLedger.PaymentEntryType.PAYMENT)
				.entryStatus(PaymentLedger.PaymentEntryStatus.SETTLED)
				.category(category != null ? category : PaymentCategory.COURSE_FEE).amount(amount).remainingAmount(0L)
				.payment(payment).build();

		return createLedger(request);
	}

	public PaymentLedger createDiscountLedger(String academyId, String courseId, UserProfile traineeUser,
			TraineeCourseEnrollment enrollment, Long discountAmount, Long remainingAmount, LocalDate effectiveDate,
			PaymentCategory category, String reason, String createdBy) {

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(traineeUser.getId()).enrollmentId(enrollment.getId()).effectiveDate(effectiveDate)
				.ledgerType(PaymentLedger.LedgerType.CREDIT).entryType(PaymentLedger.PaymentEntryType.DISCOUNT)
				.entryStatus(PaymentLedger.PaymentEntryStatus.SETTLED).category(category).amount(discountAmount)
				.remainingAmount(remainingAmount).notes(reason).createdBy(createdBy).build();

		return createLedger(request);
	}

	/**
	 * Create discount ledger
	 */
	public PaymentLedger createDiscountLedger(String academyId, String courseId, UserProfile traineeUser,
			TraineeCourseEnrollment enrollment, Long discountAmount, LocalDate effectiveDate, PaymentCategory category,
			String notes, UserProfile createdBy) {

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(traineeUser.getId()).enrollmentId(enrollment.getId()).effectiveDate(effectiveDate)
				.ledgerType(PaymentLedger.LedgerType.CREDIT).entryType(PaymentLedger.PaymentEntryType.DISCOUNT)
				.entryStatus(PaymentLedger.PaymentEntryStatus.SETTLED)
				.category(category != null ? category : PaymentCategory.COURSE_FEE).amount(discountAmount)
				.remainingAmount(discountAmount).notes(notes).createdBy(createdBy.getId()).build();

		return createLedger(request);
	}

	/**
	 * Create discount ledger
	 */
	public PaymentLedger createCreditLedger(String academyId, String courseId, UserProfile traineeUser,
			TraineeCourseEnrollment enrollment, Long discountAmount, LocalDate effectiveDate, PaymentCategory category,
			String notes, String createdBy) {

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(traineeUser.getId()).enrollmentId(enrollment.getId()).effectiveDate(effectiveDate)
				.ledgerType(PaymentLedger.LedgerType.CREDIT).entryType(PaymentLedger.PaymentEntryType.CREDIT)
				.entryStatus(PaymentLedger.PaymentEntryStatus.PENDING).category(category).amount(discountAmount)
				.remainingAmount(discountAmount).notes(notes).createdBy(createdBy).build();

		return createLedger(request);
	}

	/**
	 * Create advance payment ledger
	 */
	public PaymentLedger createAdvancePaymentLedger(String academyId, String courseId, UserProfile traineeUser,
			TraineeCourseEnrollment enrollment, Long amount, LocalDate effectiveDate, Payment payment) {

		LedgerCreationRequest request = LedgerCreationRequest.builder().academyId(academyId).courseId(courseId)
				.traineeUserId(traineeUser.getId()).enrollmentId(enrollment.getId()).effectiveDate(effectiveDate)
				.ledgerType(PaymentLedger.LedgerType.DEBIT).entryType(PaymentLedger.PaymentEntryType.ADVANCE)
				.entryStatus(PaymentLedger.PaymentEntryStatus.PARTIALLY_SETTLED).category(PaymentCategory.COURSE_FEE)
				.amount(amount).remainingAmount(amount).payment(payment).build();

		return createLedger(request);
	}

	// Builder class for ledger creation requests
	@Data
	@Builder
	public static class LedgerCreationRequest {
		private String academyId;
		private String courseId;
		private String traineeUserId;
		private String enrollmentId;
		private Payment payment;
		private LocalDate effectiveDate;
		private PaymentLedger.LedgerType ledgerType;
		private PaymentLedger.PaymentEntryType entryType;
		private PaymentLedger.PaymentEntryStatus entryStatus;
		private PaymentCategory category;
		private Long amount;
		private Long remainingAmount;
		private Boolean isReversal;
		private Boolean isLocked;
		private Long parentEntryId;
		private String notes;
		private String createdBy;
		private String updatedBy;
		private String approvedBy;
	}

}