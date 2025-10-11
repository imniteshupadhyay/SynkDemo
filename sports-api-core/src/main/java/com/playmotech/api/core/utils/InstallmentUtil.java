package com.playmotech.api.core.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.internal.Pair;
import org.springframework.stereotype.Component;

import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.InstallmentInfo;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;

@Component
public class InstallmentUtil {

	public LocalDate getLastPassedDueDate(List<LocalDate> dueDates, LocalDate today) {
		return dueDates.stream().filter(d -> !d.isAfter(today)).max(Comparator.naturalOrder()).orElse(null);
	}

	public List<LocalDate> calculateDueDates(TraineeCourseEnrollmentDto enrollment, CourseDto course,
			PaymentSchedule schedule) {
		List<InstallmentInfo> installments = calculateInstallments(enrollment, course, schedule);
		return installments.stream().map(InstallmentInfo::getDueDate).collect(Collectors.toList());
	}

	public Pair<List<InstallmentInfo>, List<LocalDate>> calculateInstallmentsAndDueDates(
			TraineeCourseEnrollmentDto enrollment, CourseDto course, PaymentSchedule schedule) {
		List<InstallmentInfo> installments = calculateInstallments(enrollment, course, schedule);

		List<LocalDate> dueDates = installments.stream().map(InstallmentInfo::getDueDate).collect(Collectors.toList());

		return Pair.of(installments, dueDates);
	}

	public List<InstallmentInfo> calculateInstallments(TraineeCourseEnrollmentDto enrollment, CourseDto course,
			PaymentSchedule paymentSchedule) {

		List<InstallmentInfo> installments = new ArrayList<>();
		Long scheduleAmount = enrollment.getAmount(); // This is the schedule-specific amount (e.g., 100 for monthly)
		LocalDate startDate = enrollment.getJoiningDate();
		LocalDate dueDate = enrollment.getDueDate();
		LocalDate endDate = LocalDate.parse(course.getSchedule().getEndDate());

		switch (paymentSchedule) {
		case FULL:
			installments.add(new InstallmentInfo(1, scheduleAmount, startDate));
			break;

		case MONTHLY:
			installments = generateMonthlyInstallments(scheduleAmount, startDate, dueDate, endDate);
			break;

		case QUARTERLY:
			installments = generateQuarterlyInstallments(scheduleAmount, startDate, dueDate, endDate);
			break;

		case HALFYEARLY:
			installments = generateSemiAnnualInstallments(scheduleAmount, startDate, dueDate, endDate);
			break;

		case YEARLY:
			installments = generateYearlyInstallments(scheduleAmount, startDate, dueDate, endDate);
			break;

		default:
			// Default to full payment
			installments.add(new InstallmentInfo(1, scheduleAmount, startDate));
			break;
		}

		return installments;
	}

	/**
	 * Helper method to calculate prorated amount with HALF_UP rounding
	 */
	private long calculateProratedAmount(long baseAmount, long actualDays, long expectedDays) {
		BigDecimal base = new BigDecimal(baseAmount);
		BigDecimal actual = new BigDecimal(actualDays);
		BigDecimal expected = new BigDecimal(expectedDays);
		
		return base.multiply(actual).divide(expected, 0, RoundingMode.HALF_UP).longValue();
	}

	private List<InstallmentInfo> generateMonthlyInstallments(Long monthlyAmount, LocalDate startDate,
			LocalDate dueDate, LocalDate endDate) {
		List<InstallmentInfo> installments = new ArrayList<>();
		LocalDate currentDate = startDate;
		int installmentNumber = 1;

		if (dueDate != null && dueDate.isAfter(startDate)) {
			// First prorata: exclude due date from calculation
			LocalDate partialEnd = dueDate.minusDays(1);

			long actualDays = ChronoUnit.DAYS.between(currentDate, partialEnd.plusDays(1));
			long expectedDays = ChronoUnit.DAYS.between(currentDate, calculateNextMonthlyDate(currentDate));

			long proratedAmount = calculateProratedAmount(monthlyAmount, actualDays, expectedDays);

			installments.add(new InstallmentInfo(installmentNumber, proratedAmount, currentDate));

			currentDate = dueDate;
			installmentNumber++;
		}

		while (!currentDate.isAfter(endDate)) {
			LocalDate nextInstallmentDate = calculateNextMonthlyDate(currentDate);
			long installmentAmount = monthlyAmount;

			LocalDate periodStart = currentDate;
			LocalDate periodEnd = nextInstallmentDate.minusDays(1);
			if (periodEnd.isAfter(endDate)) {
				periodEnd = endDate;
			}

			long actualDays = ChronoUnit.DAYS.between(periodStart, periodEnd.plusDays(1));
			long expectedDays = ChronoUnit.DAYS.between(periodStart, calculateNextMonthlyDate(periodStart));

			// Last prorata: include course end date in calculation
			if (actualDays < expectedDays) {
				installmentAmount = calculateProratedAmount(monthlyAmount, actualDays, expectedDays);
			}

			installments.add(new InstallmentInfo(installmentNumber, installmentAmount, currentDate));

			currentDate = nextInstallmentDate;
			installmentNumber++;
		}

		return installments;
	}

	private List<InstallmentInfo> generateQuarterlyInstallments(Long quarterlyAmount, LocalDate startDate,
			LocalDate dueDate, LocalDate endDate) {
		List<InstallmentInfo> installments = new ArrayList<>();
		LocalDate currentDate = startDate;
		int installmentNumber = 1;

		if (dueDate != null && dueDate.isAfter(startDate)) {
			// First prorata: exclude due date from calculation
			LocalDate partialEnd = dueDate.minusDays(1);

			long actualDays = ChronoUnit.DAYS.between(currentDate, partialEnd.plusDays(1));
			long expectedDays = ChronoUnit.DAYS.between(currentDate, calculateNextQuarterlyDate(currentDate));

			long proratedAmount = calculateProratedAmount(quarterlyAmount, actualDays, expectedDays);

			installments.add(new InstallmentInfo(installmentNumber, proratedAmount, currentDate));

			currentDate = dueDate;
			installmentNumber++;
		}

		while (!currentDate.isAfter(endDate)) {
			LocalDate nextInstallmentDate = calculateNextQuarterlyDate(currentDate);
			long installmentAmount = quarterlyAmount;

			LocalDate periodStart = currentDate;
			LocalDate periodEnd = nextInstallmentDate.minusDays(1);
			if (periodEnd.isAfter(endDate)) {
				periodEnd = endDate;
			}

			long actualDays = ChronoUnit.DAYS.between(periodStart, periodEnd.plusDays(1));
			long expectedDays = ChronoUnit.DAYS.between(periodStart, calculateNextQuarterlyDate(periodStart));

			// Last prorata: include course end date in calculation
			if (actualDays < expectedDays) {
				installmentAmount = calculateProratedAmount(quarterlyAmount, actualDays, expectedDays);
			}

			installments.add(new InstallmentInfo(installmentNumber, installmentAmount, currentDate));

			currentDate = nextInstallmentDate;
			installmentNumber++;
		}

		return installments;
	}

	private List<InstallmentInfo> generateSemiAnnualInstallments(Long semiAnnualAmount, LocalDate startDate,
			LocalDate dueDate, LocalDate endDate) {
		List<InstallmentInfo> installments = new ArrayList<>();
		LocalDate currentDate = startDate;
		int installmentNumber = 1;

		if (dueDate != null && dueDate.isAfter(startDate)) {
			// First prorata: exclude due date from calculation
			LocalDate partialEnd = dueDate.minusDays(1);

			long actualDays = ChronoUnit.DAYS.between(currentDate, partialEnd.plusDays(1));
			long expectedDays = ChronoUnit.DAYS.between(currentDate, calculateNextSemiAnnualDate(currentDate));

			long proratedAmount = calculateProratedAmount(semiAnnualAmount, actualDays, expectedDays);

			installments.add(new InstallmentInfo(installmentNumber, proratedAmount, currentDate));

			currentDate = dueDate;
			installmentNumber++;
		}

		while (!currentDate.isAfter(endDate)) {
			LocalDate nextInstallmentDate = calculateNextSemiAnnualDate(currentDate);
			long installmentAmount = semiAnnualAmount;

			LocalDate periodStart = currentDate;
			LocalDate periodEnd = nextInstallmentDate.minusDays(1);
			if (periodEnd.isAfter(endDate)) {
				periodEnd = endDate;
			}

			long actualDays = ChronoUnit.DAYS.between(periodStart, periodEnd.plusDays(1));
			long expectedDays = ChronoUnit.DAYS.between(periodStart, calculateNextSemiAnnualDate(periodStart));

			// Last prorata: include course end date in calculation
			if (actualDays < expectedDays) {
				installmentAmount = calculateProratedAmount(semiAnnualAmount, actualDays, expectedDays);
			}

			installments.add(new InstallmentInfo(installmentNumber, installmentAmount, currentDate));

			currentDate = nextInstallmentDate;
			installmentNumber++;
		}

		return installments;
	}

	private List<InstallmentInfo> generateYearlyInstallments(Long yearlyAmount, LocalDate startDate, LocalDate dueDate,
			LocalDate endDate) {
		List<InstallmentInfo> installments = new ArrayList<>();
		LocalDate currentDate = startDate;
		int installmentNumber = 1;

		if (dueDate != null && dueDate.isAfter(startDate)) {
			// First prorata: exclude due date from calculation
			LocalDate partialEnd = dueDate.minusDays(1);

			long actualDays = ChronoUnit.DAYS.between(currentDate, partialEnd.plusDays(1));
			long expectedDays = ChronoUnit.DAYS.between(currentDate, calculateNextYearlyDate(currentDate));

			long proratedAmount = calculateProratedAmount(yearlyAmount, actualDays, expectedDays);

			installments.add(new InstallmentInfo(installmentNumber, proratedAmount, currentDate));

			currentDate = dueDate;
			installmentNumber++;
		}

		while (!currentDate.isAfter(endDate)) {
			LocalDate nextInstallmentDate = calculateNextYearlyDate(currentDate);
			long installmentAmount = yearlyAmount;

			LocalDate periodStart = currentDate;
			LocalDate periodEnd = nextInstallmentDate.minusDays(1);
			if (periodEnd.isAfter(endDate)) {
				periodEnd = endDate;
			}

			long actualDays = ChronoUnit.DAYS.between(periodStart, periodEnd.plusDays(1));
			long expectedDays = ChronoUnit.DAYS.between(periodStart, calculateNextYearlyDate(periodStart));

			// Last prorata: include course end date in calculation
			if (actualDays < expectedDays) {
				installmentAmount = calculateProratedAmount(yearlyAmount, actualDays, expectedDays);
			}

			installments.add(new InstallmentInfo(installmentNumber, installmentAmount, currentDate));

			currentDate = nextInstallmentDate;
			installmentNumber++;
		}

		return installments;
	}

	// Helper method to handle month-end dates properly
	private LocalDate calculateNextMonthlyDate(LocalDate currentDate) {
		int dayOfMonth = currentDate.getDayOfMonth();
		LocalDate nextMonth = currentDate.plusMonths(1);

		// Handle month-end edge cases
		if (dayOfMonth > nextMonth.lengthOfMonth()) {
			// If current date is 31st and next month has only 30 days, use last day of next
			// month
			return nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
		} else {
			return nextMonth.withDayOfMonth(dayOfMonth);
		}
	}

	// Helper method to handle quarter-end dates properly
	private LocalDate calculateNextQuarterlyDate(LocalDate currentDate) {
		int dayOfMonth = currentDate.getDayOfMonth();
		LocalDate nextQuarter = currentDate.plusMonths(3);

		// Handle month-end edge cases
		if (dayOfMonth > nextQuarter.lengthOfMonth()) {
			return nextQuarter.withDayOfMonth(nextQuarter.lengthOfMonth());
		} else {
			return nextQuarter.withDayOfMonth(dayOfMonth);
		}
	}

	// Helper method to handle semi-annual dates properly
	private LocalDate calculateNextSemiAnnualDate(LocalDate currentDate) {
		int dayOfMonth = currentDate.getDayOfMonth();
		LocalDate nextPeriod = currentDate.plusMonths(6);

		// Handle month-end edge cases
		if (dayOfMonth > nextPeriod.lengthOfMonth()) {
			return nextPeriod.withDayOfMonth(nextPeriod.lengthOfMonth());
		} else {
			return nextPeriod.withDayOfMonth(dayOfMonth);
		}
	}

	// Helper method to handle yearly dates properly
	private LocalDate calculateNextYearlyDate(LocalDate currentDate) {
		// Handle leap year edge case for Feb 29
		if (currentDate.getMonthValue() == 2 && currentDate.getDayOfMonth() == 29) {
			LocalDate nextYear = currentDate.plusYears(1);
			if (!nextYear.isLeapYear()) {
				return nextYear.withDayOfMonth(28); // Feb 28 in non-leap year
			}
		}
		return currentDate.plusYears(1);
	}

}
