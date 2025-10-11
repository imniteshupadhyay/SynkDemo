package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.ScheduleType;

public class ScheduleTypeConverter implements DynamoDBTypeConverter<String, ScheduleType> {

	@Override
	public String convert(ScheduleType scheduleType) {
		return scheduleType.name();
	}

	@Override
	public ScheduleType unconvert(String s) {
		return ScheduleType.valueOf(s);
	}
}
