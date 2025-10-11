package com.playmotech.api.core.services.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.playmotech.api.core.constants.AppConstants;
import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentStatus;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dao_postgres.Payment;
import com.playmotech.api.core.dao_postgres.TraineeAcademyMapping;
import com.playmotech.api.core.dto.CourseAttendanceDto;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.PaymentDetailsDto;
import com.playmotech.api.core.dto.ReportResponseDto;
import com.playmotech.api.core.dto.ReportSummaryDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IAttendanceService;
import com.playmotech.api.core.services.ICourseService;
import com.playmotech.api.core.services.IPaymentService;
import com.playmotech.api.core.services.IReportService;
import com.playmotech.api.core.services.ITraineeService;
import com.playmotech.api.core.services.NewPaymentService;
import com.playmotech.api.core.utils.CurrencyUtils;
import com.playmotech.api.core.utils.DateTimeUtils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class ReportService implements IReportService {
	private final IAttendanceService attendanceService;
	private final ITraineeService traineeService;
	private final IAcademyService academyService;
	private final IPaymentService paymentService;
	private final ICourseService courseService;

	private final NewPaymentService newPaymentService;

	@Override
	public List<ReportResponseDto> getEnrolledTraineesMonthly(String academyId, String startDate, String endDate,
			String timezone) throws ResourceException {
		List<ReportResponseDto> responseDtos = new ArrayList<>();

		List<Long> epochsStartOfMonth = DateTimeUtils.getFirstDayOfMonthEpochs(startDate, endDate, timezone);

		if (epochsStartOfMonth.isEmpty()) {
			return List.of();
		}

		List<TraineeAcademyMapping> enrollments = traineeService.getEnrollmentsByAcademyId(academyId,
				epochsStartOfMonth.get(0), epochsStartOfMonth.get(epochsStartOfMonth.size() - 1));
		for (int i = 0; i < epochsStartOfMonth.size(); i++) {
			ReportResponseDto responseDto = new ReportResponseDto();

			Long startEpoch = epochsStartOfMonth.get(i);
			Long endEpoch = i == epochsStartOfMonth.size() - 1 ? null : epochsStartOfMonth.get(i + 1);

			List<TraineeAcademyMapping> enrollmentsForMonth = enrollments.stream()
					.filter(enrollment -> enrollment.getCreatedOn().toInstant().getEpochSecond() >= startEpoch
							&& (endEpoch == null || enrollment.getCreatedOn().toInstant().getEpochSecond() < endEpoch))
					.toList();
			responseDto.setDimension(Map.of("month", DateTimeUtils.epochToMonthName(startEpoch, timezone)));
			responseDto.setMeasure((long) enrollmentsForMonth.size());
			responseDtos.add(responseDto);
		}
		return responseDtos;
	}

	@Override
	public List<ReportResponseDto> getTraineesAttendanceDaily(String academyId, String startDate, String endDate,
			String timezone, List<String> courseId, Sports sport) throws ResourceException {
		List<ReportResponseDto> responseDtos = new ArrayList<>();
		List<CourseAttendanceDto> courseAttendanceDtos = attendanceService.getAttendanceBetweenDates(academyId,
				startDate, endDate);

		if (sport != null) {
			List<String> courseIds = courseAttendanceDtos.stream().map(CourseAttendanceDto::getCourseId).toList();
			List<CourseDto> courseDtos = courseService.getCourses(academyId, courseIds, null, null);
			courseIds = courseDtos.stream().filter(courseDto -> courseDto.getSport().equals(sport))
					.map(CourseDto::getId).toList();
			if (CollectionUtils.isEmpty(courseId)) {
				courseId = courseIds;
			} else {
				courseId = courseId.stream().filter(courseIds::contains).toList();
			}
		}

		boolean isDaily = DateTimeUtils.daysBetweenDate(startDate, endDate) < 89;
		Map<String, Map<String, Long>> attendedCount = new HashMap<>();
		for (CourseAttendanceDto courseAttendanceDto : courseAttendanceDtos) {
			if (!attendedCount.containsKey(courseAttendanceDto.getCourseId())) {
				attendedCount.put(courseAttendanceDto.getCourseId(), new HashMap<>());
			}
			for (Map.Entry<String, Boolean> entry : courseAttendanceDto.getAttendance().entrySet()) {
				if (!attendedCount.get(courseAttendanceDto.getCourseId()).containsKey(entry.getKey())) {
					attendedCount.get(courseAttendanceDto.getCourseId()).put(entry.getKey(), 0L);
				}
				if (entry.getValue()) {
					attendedCount.get(courseAttendanceDto.getCourseId()).put(entry.getKey(),
							attendedCount.get(courseAttendanceDto.getCourseId()).get(entry.getKey()) + 1);
				}
			}
		}

		for (Map.Entry<String, Map<String, Long>> entry : attendedCount.entrySet()) {
			if (!CollectionUtils.isEmpty(courseId) && !courseId.contains(entry.getKey())) {
				continue;
			}
			for (Map.Entry<String, Long> entry1 : entry.getValue().entrySet()) {
				Optional<ReportResponseDto> responseDto1 = responseDtos.stream()
						.filter(responseDto2 -> responseDto2.getDimension().get("date").equalsIgnoreCase(
								!isDaily ? LocalDate.parse(entry1.getKey(), DateTimeUtils.YYYY_MM_DD_FORMATTER)
										.format(DateTimeUtils.MONTH_YEAR_FORMATTER) : entry1.getKey()))
						.findFirst();

				ReportResponseDto responseDto = responseDto1.orElseGet(ReportResponseDto::new);
				if (responseDto1.isEmpty()) {
					responseDto.setMeasure(0L);
					responseDtos.add(responseDto);
				}
				Map<String, String> dimensions = new HashMap<>();

				dimensions.put("date", !isDaily ? LocalDate.parse(entry1.getKey(), DateTimeUtils.YYYY_MM_DD_FORMATTER)
						.format(DateTimeUtils.MONTH_YEAR_FORMATTER) : entry1.getKey());
				if (CollectionUtils.isEmpty(courseId)) {
					if (responseDto1.isPresent()) {
						responseDto1.get().setMeasure(responseDto1.get().getMeasure() + entry1.getValue());
					} else {
						responseDto.setDimension(dimensions);
						responseDto.setMeasure(entry1.getValue() + responseDto.getMeasure());
//                        responseDtos.add(responseDto);
					}
				} else {
					dimensions.put("courseId", entry.getKey());
					responseDto.setDimension(dimensions);
					responseDto.setMeasure(entry1.getValue() + responseDto.getMeasure());
//                    responseDtos.add(responseDto);
				}
			}
		}

		return getRemainingDate(startDate, endDate, isDaily, responseDtos);
	}

	@Override
	public List<ReportResponseDto> getTotalPaymentReceived(String academyId, String startDate, String endDate,
			String timezone, List<String> courseIds, Sports sport) throws ResourceException {
		academyService.getAcademyById(academyId);
		List<Payment> payments = paymentService.getPaymentsBetween(academyId, startDate, endDate, courseIds).stream()
				.filter(payment -> payment.getPaymentStatus() == PaymentStatus.SUCCESS).toList();
		boolean isDaily = DateTimeUtils.daysBetweenDate(startDate, endDate) < 89;
		Map<String, Long> paymentsByDay = payments.stream().collect(Collectors.groupingBy(
				payment -> isDaily ? DateTimeUtils.formatTransactionTimeToDailyDateFormat(payment.getTransactionTime())
						: DateTimeUtils.formatTransactionTimeToMonthlyFormat(payment.getTransactionTime()),
				Collectors.summingLong(Payment::getAmount)));
		List<ReportResponseDto> responseDtos = new ArrayList<>();
		for (Map.Entry<String, Long> entry : paymentsByDay.entrySet()) {
			ReportResponseDto responseDto = new ReportResponseDto();
			responseDto.setDimension(Map.of("date", entry.getKey()));
			responseDto.setMeasure(entry.getValue());
			responseDtos.add(responseDto);
		}
		return getRemainingDate(startDate, endDate, isDaily, responseDtos);
	}

	@Override
	public List<ReportResponseDto> getPendingDuesCount(String academyId, List<String> courseIds)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		List<PaymentDetailsDto> paymentDetailsDtos = paymentService.getPaymentDetailsByAcademy(academyId, courseIds);
		if (CollectionUtils.isEmpty(paymentDetailsDtos)) {
			return List.of();
		}

		paymentDetailsDtos = paymentDetailsDtos.stream().filter(PaymentDetailsDto::getIsDue).toList();
		List<ReportResponseDto> responseDtos = new ArrayList<>();
		ReportResponseDto responseDto = new ReportResponseDto();
		responseDto.setDimension(Map.of("dueAsOf", "Today"));
		responseDto.setMeasure((long) paymentDetailsDtos.size());
		responseDtos.add(responseDto);
		return responseDtos;
	}

	@Override
	public List<ReportResponseDto> getNewPendingDuesCount(String academyId, List<String> courseIds,
			PaymentCategory paymentCategory) throws ResourceException {
		academyService.getAcademyById(academyId);
		List<PaymentDetailsDto> paymentDetailsDtos = newPaymentService.getPaymentDetailsByAcademy(academyId, courseIds,
				paymentCategory);
		if (CollectionUtils.isEmpty(paymentDetailsDtos)) {
			return List.of();
		}

		paymentDetailsDtos = paymentDetailsDtos.stream().filter(PaymentDetailsDto::getIsDue).toList();
		List<ReportResponseDto> responseDtos = new ArrayList<>();
		ReportResponseDto responseDto = new ReportResponseDto();
		responseDto.setDimension(Map.of("dueAsOf", "Today"));
		responseDto.setMeasure((long) paymentDetailsDtos.size());
		responseDtos.add(responseDto);
		return responseDtos;
	}

	@Override
	public List<ReportResponseDto> getPendingDuesAmount(String academyId, List<String> courseIds)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		List<PaymentDetailsDto> paymentDetailsDtos = paymentService.getPaymentDetailsByAcademy(academyId, courseIds);
		if (CollectionUtils.isEmpty(paymentDetailsDtos)) {
			return List.of();
		}
		paymentDetailsDtos = paymentDetailsDtos.stream().filter(PaymentDetailsDto::getIsDue).toList();
		List<ReportResponseDto> responseDtos = new ArrayList<>();
		Long totalDues = paymentDetailsDtos.stream().mapToLong(PaymentDetailsDto::getNextDueAmount).sum();
		ReportResponseDto responseDto = new ReportResponseDto();
		responseDto.setDimension(Map.of("dueAsOf", "Today"));
		responseDto.setMeasure(totalDues);
		responseDtos.add(responseDto);
		return responseDtos;
	}

	@Override
	public List<ReportResponseDto> getNewPendingDuesAmount(String academyId, List<String> courseIds,
			PaymentCategory category) throws ResourceException {
		academyService.getAcademyById(academyId);
		List<PaymentDetailsDto> paymentDetailsDtos = newPaymentService.getPaymentDetailsByAcademy(academyId, courseIds,
				category);
		if (CollectionUtils.isEmpty(paymentDetailsDtos)) {
			return List.of();
		}
		paymentDetailsDtos = paymentDetailsDtos.stream().filter(PaymentDetailsDto::getIsDue).toList();
		List<ReportResponseDto> responseDtos = new ArrayList<>();
		Long totalDues = paymentDetailsDtos.stream().mapToLong(PaymentDetailsDto::getPendingAmount).sum();
		ReportResponseDto responseDto = new ReportResponseDto();
		responseDto.setDimension(Map.of("dueAsOf", "Today"));
		responseDto.setMeasure(totalDues);
		responseDtos.add(responseDto);
		return responseDtos;
	}

	@Override
	public List<ReportSummaryDto> getReportSummaries(String academyId) throws ResourceException {
		List<ReportSummaryDto> reportSummaryDtos = new ArrayList<>();
		List<ReportResponseDto> getPendingDues = getPendingDuesAmount(academyId, new ArrayList<>());
		if (!CollectionUtils.isEmpty(getPendingDues)) {
			ReportSummaryDto reportSummaryDto = new ReportSummaryDto();
			reportSummaryDto.setTitle("Fees Pending");
			reportSummaryDto.setCta("PENDING_FEES_TRAINEES");
			reportSummaryDto.setValue(CurrencyUtils.formatCurrency(Currency.INR, getPendingDues.get(0).getMeasure()));
			reportSummaryDtos.add(reportSummaryDto);
		} else {
			ReportSummaryDto reportSummaryDto = new ReportSummaryDto();
			reportSummaryDto.setTitle("Fees Pending");
			reportSummaryDto.setCta("PENDING_FEES_TRAINEES");
			reportSummaryDto.setValue(CurrencyUtils.formatCurrency(Currency.INR, 0L));
			reportSummaryDtos.add(reportSummaryDto);
		}

		Long currentMonthsFirstDayEpoch = DateTimeUtils.getCurrentMonthsFirstDayEpoch();
		List<TraineeAcademyMapping> enrollments = traineeService.getEnrollmentsByAcademyId(academyId,
				currentMonthsFirstDayEpoch, null);
		if (!CollectionUtils.isEmpty(enrollments)) {
			ReportSummaryDto reportSummaryDto = new ReportSummaryDto();
			reportSummaryDto.setTitle("Current Month's Registrations");
			reportSummaryDto.setValue(String.valueOf(enrollments.size()));
			reportSummaryDto.setCta("CURR_MONTHS_REG_USERS");
			reportSummaryDtos.add(reportSummaryDto);
		} else {
			ReportSummaryDto reportSummaryDto = new ReportSummaryDto();
			reportSummaryDto.setTitle("Current Month's Registrations");
			reportSummaryDto.setValue(String.valueOf(0));
			reportSummaryDto.setCta("CURR_MONTHS_REG_USERS");
			reportSummaryDtos.add(reportSummaryDto);
		}

		return reportSummaryDtos;
	}

	@Override
	public List<ReportSummaryDto> getNewReportSummaries(String academyId, PaymentCategory paymentCategory)
			throws ResourceException {
		List<ReportSummaryDto> reportSummaryDtos = new ArrayList<>();
		List<ReportResponseDto> getPendingDues = getNewPendingDuesAmount(academyId, new ArrayList<>(), paymentCategory);
		if (!CollectionUtils.isEmpty(getPendingDues)) {
			ReportSummaryDto reportSummaryDto = new ReportSummaryDto();
			reportSummaryDto.setTitle("Fees Pending");
			reportSummaryDto.setCta("PENDING_FEES_TRAINEES");
			reportSummaryDto.setValue(CurrencyUtils.formatCurrency(Currency.INR, getPendingDues.get(0).getMeasure()));
			reportSummaryDtos.add(reportSummaryDto);
		} else {
			ReportSummaryDto reportSummaryDto = new ReportSummaryDto();
			reportSummaryDto.setTitle("Fees Pending");
			reportSummaryDto.setCta("PENDING_FEES_TRAINEES");
			reportSummaryDto.setValue(CurrencyUtils.formatCurrency(Currency.INR, 0L));
			reportSummaryDtos.add(reportSummaryDto);
		}

		Long currentMonthsFirstDayEpoch = DateTimeUtils.getCurrentMonthsFirstDayEpoch();
		List<TraineeAcademyMapping> enrollments = traineeService.getEnrollmentsByAcademyId(academyId,
				currentMonthsFirstDayEpoch, null);
		if (!CollectionUtils.isEmpty(enrollments)) {
			ReportSummaryDto reportSummaryDto = new ReportSummaryDto();
			reportSummaryDto.setTitle("Current Month's Registrations");
			reportSummaryDto.setValue(String.valueOf(enrollments.size()));
			reportSummaryDto.setCta("CURR_MONTHS_REG_USERS");
			reportSummaryDtos.add(reportSummaryDto);
		} else {
			ReportSummaryDto reportSummaryDto = new ReportSummaryDto();
			reportSummaryDto.setTitle("Current Month's Registrations");
			reportSummaryDto.setValue(String.valueOf(0));
			reportSummaryDto.setCta("CURR_MONTHS_REG_USERS");
			reportSummaryDtos.add(reportSummaryDto);
		}

		return reportSummaryDtos;
	}

	private List<ReportResponseDto> getRemainingDate(String startDate, String endDate, boolean isDaily,
			List<ReportResponseDto> data) {
		List<ReportResponseDto> responseDtos = new ArrayList<>();
		if (isDaily) {
			List<String> dates = DateTimeUtils.dateBetween(startDate, endDate, true, true, false,
					AppConstants.DEFAULT_TIMEZONE);
			for (String date : dates) {
				Optional<ReportResponseDto> responseDto1 = data.stream()
						.filter(responseDto2 -> responseDto2.getDimension().get("date").equalsIgnoreCase(date))
						.findFirst();
				if (responseDto1.isEmpty()) {
					ReportResponseDto responseDto = new ReportResponseDto();
					responseDto.setDimension(Map.of("date", date));
					responseDto.setMeasure(0L);
					responseDtos.add(responseDto);
				} else {
					responseDtos.add(responseDto1.get());
				}
			}
		} else {
			List<Long> epochsStartOfMonth = DateTimeUtils.getFirstDayOfMonthEpochs(startDate, endDate,
					AppConstants.DEFAULT_TIMEZONE);
			for (Long epoch : epochsStartOfMonth) {
				Optional<ReportResponseDto> responseDto1 = data.stream()
						.filter(responseDto2 -> responseDto2.getDimension().get("date")
								.equalsIgnoreCase(DateTimeUtils.epochToMonthName(epoch, AppConstants.DEFAULT_TIMEZONE)))
						.findFirst();
				if (responseDto1.isEmpty()) {
					ReportResponseDto responseDto = new ReportResponseDto();
					responseDto.setDimension(
							Map.of("date", DateTimeUtils.epochToMonthName(epoch, AppConstants.DEFAULT_TIMEZONE)));
					responseDto.setMeasure(0L);
					responseDtos.add(responseDto);
				} else {
					responseDtos.add(responseDto1.get());
				}
			}
		}
		return responseDtos;
	}
}
