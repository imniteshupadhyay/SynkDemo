package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.PerformanceReportStatus;

public class PerformanceReportStatusTypeConverter implements DynamoDBTypeConverter<String, PerformanceReportStatus> {

	@Override
	public String convert(PerformanceReportStatus performanceReportStatus) {
		return performanceReportStatus.name();
	}

	@Override
	public PerformanceReportStatus unconvert(String s) {
		return PerformanceReportStatus.valueOf(s);
	}
}
