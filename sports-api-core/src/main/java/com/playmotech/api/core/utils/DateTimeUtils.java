package com.playmotech.api.core.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.AppConstants;

public final class DateTimeUtils {
	public static final DateTimeFormatter YYYY_MM_DD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final DateTimeFormatter TRANSACTION_TIME_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
	public static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM ''yy");

	public static List<String> dateBetween(String startDateStr, String endDateStr, boolean startDateInclusive,
			boolean endDateInclusive, boolean excludeWeekends, String timezone) {
		if (StringUtils.isEmpty(timezone)) {
			timezone = AppConstants.DEFAULT_TIMEZONE;
		}

		List<String> dates = new ArrayList<>();

		LocalDate startDate = LocalDate.parse(startDateStr, YYYY_MM_DD_FORMATTER);
		LocalDate endDate = LocalDate.parse(endDateStr, YYYY_MM_DD_FORMATTER);
		LocalDate currentDate = startDate;
		if (startDateInclusive) {
			if (excludeWeekends) {
				if (!(startDate.getDayOfWeek().getValue() == 6 || startDate.getDayOfWeek().getValue() == 7)) {
					dates.add(startDate.format(YYYY_MM_DD_FORMATTER));
				}
			} else {
				dates.add(startDate.format(YYYY_MM_DD_FORMATTER));
			}
		}

		while (currentDate.isBefore(endDate)
				&& currentDate.isBefore(ZonedDateTime.now(ZoneId.of(timezone)).toLocalDate())) {
			currentDate = currentDate.plusDays(1);
			if (excludeWeekends) {
				if (!(currentDate.getDayOfWeek().getValue() == 6 || currentDate.getDayOfWeek().getValue() == 7)) {
					dates.add(currentDate.format(YYYY_MM_DD_FORMATTER));
				}
			} else {
				dates.add(currentDate.format(YYYY_MM_DD_FORMATTER));
			}
		}

		if (endDateInclusive && currentDate.isBefore(ZonedDateTime.now(ZoneId.of(timezone)).toLocalDate())) {
			if (excludeWeekends) {
				if (!(endDate.getDayOfWeek().getValue() == 6 || endDate.getDayOfWeek().getValue() == 7)) {
					dates.add(endDate.format(YYYY_MM_DD_FORMATTER));
				}
			} else {
				dates.add(endDate.format(YYYY_MM_DD_FORMATTER));
			}
		}
		return dates;
	}

	public static List<String> weekendsBetween(String startDateStr, String endDateStr, boolean startDateInclusive,
			boolean endDateInclusive, String timezone) {
		if (StringUtils.isEmpty(timezone)) {
			timezone = AppConstants.DEFAULT_TIMEZONE;
		}

		List<String> dates = new ArrayList<>();
		LocalDate startDate = LocalDate.parse(startDateStr, YYYY_MM_DD_FORMATTER);
		LocalDate endDate = LocalDate.parse(endDateStr, YYYY_MM_DD_FORMATTER);

		LocalDate currentDate = startDate;
		if (startDateInclusive) {
			if (startDate.getDayOfWeek().getValue() == 6 || startDate.getDayOfWeek().getValue() == 7) {
				dates.add(startDate.format(YYYY_MM_DD_FORMATTER));
			}
		}

		while (!currentDate.isAfter(endDate)
				&& currentDate.isBefore(ZonedDateTime.now(ZoneId.of(timezone)).toLocalDate())) {
			currentDate = currentDate.plusDays(1);
			if (currentDate.getDayOfWeek().getValue() == 6 || currentDate.getDayOfWeek().getValue() == 7) {
				dates.add(currentDate.format(YYYY_MM_DD_FORMATTER));
			}
		}

		if (endDateInclusive && currentDate.isBefore(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDate())) {
			if (endDate.getDayOfWeek().getValue() == 6 || endDate.getDayOfWeek().getValue() == 7) {
				dates.add(endDate.format(YYYY_MM_DD_FORMATTER));
			}
		}
		return dates;
	}

	public static String convertLongDateToString(Long dateLong) {
		LocalDate date = LocalDate.parse(String.valueOf(dateLong), DateTimeFormatter.ofPattern("yyyyMMdd"));
		return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	}

	public static String epochToMonthName(Long epoch, String timezone) {
		Instant instant = Instant.ofEpochSecond(epoch);
		// Convert Instant to ZonedDateTime with the specified time zone
		ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(timezone));
		// Get the month name
		String month = zonedDateTime.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
		// Get the last two digits of the year
		String year = String.valueOf(zonedDateTime.getYear()).substring(2);
		// Return the month and year in the desired format
		return month + " '" + year;
	}

	public static List<String> getStartDateMonthly(String startDateStr, String endDateStr) {
		List<String> dates = new ArrayList<>();
		LocalDate startDate = LocalDate.parse(startDateStr);
		LocalDate endDate = LocalDate.parse(endDateStr);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate currentDate = startDate;
		while (!currentDate.isAfter(endDate)) {
			dates.add(currentDate.format(formatter));
			currentDate = currentDate.plusMonths(1).withDayOfMonth(1);
		}
		return dates;
	}

	public static List<String> getStartDateYearly(String startDateStr, String endDateStr) {
		List<String> dates = new ArrayList<>();
		LocalDate startDate = LocalDate.parse(startDateStr);
		LocalDate endDate = LocalDate.parse(endDateStr);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate currentDate = startDate;
		while (!currentDate.isAfter(endDate)) {
			dates.add(currentDate.format(formatter));
			currentDate = currentDate.plusYears(1).withDayOfYear(1);
		}
		return dates;
	}

	public static List<Long> getFirstDayOfMonthEpochs(String startDateStr, String endDateStr, String timezone) {
		LocalDate startDate = LocalDate.parse(startDateStr, YYYY_MM_DD_FORMATTER); // Example: Jan 1, 2023
		LocalDate endDate = LocalDate.parse(endDateStr, YYYY_MM_DD_FORMATTER); // Example: Jan 1, 2023
		List<Long> epochList = new ArrayList<>();
		// Start from the first day of the start month
		LocalDate current = startDate.withDayOfMonth(1);

		while (!current.isAfter(endDate)) {
			// Convert to epoch seconds and add to the list
			long epoch = current.atStartOfDay().toEpochSecond(ZonedDateTime
					.now(ZoneId.of(org.apache.commons.lang3.StringUtils.isEmpty(timezone) ? "Asia/Kolkata" : timezone))
					.getOffset());
			epochList.add(epoch);
			// Move to the first day of the next month
			current = current.plusMonths(1);
		}
		return epochList;
	}

	public static String formatTransactionTimeToDailyDateFormat(String transactionTime) {
		LocalDateTime dateTime = LocalDateTime.parse(transactionTime, TRANSACTION_TIME_FORMATTER);
		// Format the LocalDateTime to the desired output format
		return dateTime.format(YYYY_MM_DD_FORMATTER);
	}

	public static String formatTransactionTimeToMonthlyFormat(String transactionTime) {
		LocalDateTime dateTime = LocalDateTime.parse(transactionTime, TRANSACTION_TIME_FORMATTER);
		// Format the LocalDateTime to the desired output format
		return dateTime.format(MONTH_YEAR_FORMATTER);
	}

	public static Pair<Long, Long> yesterdayAndTodayStartOfTheDayEpoch() {
		LocalDate today = LocalDate.now();
		// Get yesterday's date
		LocalDate yesterday = today.minusDays(1);
		// Convert yesterday's date to start of the day (midnight) and get epoch seconds
		ZonedDateTime yesterdayStartOfDay = yesterday.atStartOfDay(ZoneId.systemDefault());
		long yesterdayStartEpoch = yesterdayStartOfDay.toEpochSecond();
		// Convert today's date to start of the day (midnight) and get epoch seconds
		ZonedDateTime todayStartOfDay = today.atStartOfDay(ZoneId.systemDefault());
		long todayStartEpoch = todayStartOfDay.toEpochSecond();
		return Pair.of(yesterdayStartEpoch, todayStartEpoch);
	}

	public static Long getCurrentMonthsFirstDayEpoch() {
		LocalDate today = LocalDate.now();

		// Get the first day of the current month
		LocalDate firstDayOfMonth = today.withDayOfMonth(1);

		// Convert the first day of the month to start of the day (midnight) and get
		// epoch seconds
		ZonedDateTime firstDayStartOfDay = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault());
		return firstDayStartOfDay.toEpochSecond();
	}

	public static long daysBetweenDate(String startDate, String endDate) {
		return ChronoUnit.DAYS.between(LocalDate.parse(startDate), LocalDate.parse(endDate));
	}

	public static String getReportCurrentDate() {
		LocalDate today = LocalDate.now();
		int dayOfMonth = today.getDayOfMonth();
		String daySuffix = getDaySuffix(dayOfMonth);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
		return dayOfMonth + daySuffix + " " + today.format(formatter);
	}

	private static String getDaySuffix(int day) {
		if (day >= 11 && day <= 13) {
			return "th";
		}
		return switch (day % 10) {
		case 1 -> "st";
		case 2 -> "nd";
		case 3 -> "rd";
		default -> "th";
		};
	}

	public static boolean isExpired(String endDate, String timezone) {

		// Parse the input string into a LocalDate object
		LocalDate inputDate = LocalDate.parse(endDate, YYYY_MM_DD_FORMATTER);

		// Get the current date in the specified timezone
		ZoneId zoneId = ZoneId.of(timezone);
		ZonedDateTime currentDateTime = ZonedDateTime.now(zoneId);
		LocalDate currentDate = currentDateTime.toLocalDate();
		return inputDate.isBefore(currentDate);
	}

	public static String getTodayDate() {
		Instant instant = Instant.now();
		LocalDate localDate = instant.atZone(ZoneId.of(AppConstants.DEFAULT_TIMEZONE)).toLocalDate();
		return localDate.format(YYYY_MM_DD_FORMATTER);
	}

	/**
	 * Generates monthly due dates between a start and end date, preserving the day
	 * of the month. Adjusts for months with fewer days (e.g., February).
	 */
	public static List<LocalDate> getMonthlyDueDates(LocalDate start, LocalDate end) {
		List<LocalDate> dueDates = new ArrayList<>();
		if (start.isAfter(end)) {
			return dueDates;
		}

		int originalDay = start.getDayOfMonth();
		LocalDate current = start;

		while (current.isBefore(end)) {
			dueDates.add(current);
			current = current.plusMonths(1);

			// Adjust day if the new month has fewer days
			int lastDayOfMonth = current.lengthOfMonth();
			int adjustedDay = Math.min(originalDay, lastDayOfMonth);
			current = LocalDate.of(current.getYear(), current.getMonth(), adjustedDay);
		}

		return dueDates;
	}

	/**
	 * Generates quarterly due dates between a start and end date, preserving the
	 * day of the month. Adjusts for months with fewer days (e.g., February).
	 */
	public static List<LocalDate> getQuarterlyDueDates(LocalDate start, LocalDate end) {
		List<LocalDate> dueDates = new ArrayList<>();
		if (start.isAfter(end)) {
			return dueDates;
		}

		int originalDay = start.getDayOfMonth();
		LocalDate current = start;

		while (current.isBefore(end)) {
			dueDates.add(current);
			current = current.plusMonths(3);

			// Adjust day if the new month has fewer days
			int lastDayOfMonth = current.lengthOfMonth();
			int adjustedDay = Math.min(originalDay, lastDayOfMonth);
			current = LocalDate.of(current.getYear(), current.getMonth(), adjustedDay);
		}

		return dueDates;
	}
	
	
	/**
	 * Generates quarterly due dates between a start and end date, preserving the
	 * day of the month. Adjusts for months with fewer days (e.g., February).
	 */
	public static List<LocalDate> getHalfYearlyDueDates(LocalDate start, LocalDate end) {
		List<LocalDate> dueDates = new ArrayList<>();
		if (start.isAfter(end)) {
			return dueDates;
		}

		int originalDay = start.getDayOfMonth();
		LocalDate current = start;

		while (current.isBefore(end)) {
			dueDates.add(current);
			current = current.plusMonths(6);

			// Adjust day if the new month has fewer days
			int lastDayOfMonth = current.lengthOfMonth();
			int adjustedDay = Math.min(originalDay, lastDayOfMonth);
			current = LocalDate.of(current.getYear(), current.getMonth(), adjustedDay);
		}

		return dueDates;
	}


	/**
	 * Generates yearly due dates between a start and end date, preserving the month
	 * and day. Handles leap years (e.g., Feb 29 -> Feb 28 in non-leap years).
	 */
	public static List<LocalDate> getYearlyDueDates(LocalDate start, LocalDate end) {
		List<LocalDate> dueDates = new ArrayList<>();
		if (start.isAfter(end)) {
			return dueDates;
		}

		int originalDay = start.getDayOfMonth();
		Month originalMonth = start.getMonth();
		LocalDate current = start;

		while (current.isBefore(end)) {
			dueDates.add(current);
			current = current.plusYears(1);

			// Adjust day if the target year’s month has fewer days
			int lastDayOfMonth = current.withMonth(originalMonth.getValue()).lengthOfMonth();
			int adjustedDay = Math.min(originalDay, lastDayOfMonth);
			current = LocalDate.of(current.getYear(), originalMonth, adjustedDay);
		}

		return dueDates;
	}

}
