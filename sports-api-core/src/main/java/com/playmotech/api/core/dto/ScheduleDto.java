package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.DayOfWeek;
import com.playmotech.api.core.constants.ScheduleType;

import lombok.Data;

@Data
public class ScheduleDto {
	private ScheduleType type;
	private List<String> customDates;
	private List<DayOfWeek> weekdays;
	private String startDate;
	private String endDate;
	private String startTime;
	private String endTime;
	private Long amount;
	private Currency currency;
	private List<String> rulesAndRegulations;
	private String timezone;
}
