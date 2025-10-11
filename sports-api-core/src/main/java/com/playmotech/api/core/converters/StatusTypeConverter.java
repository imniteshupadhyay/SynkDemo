package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.Status;

public class StatusTypeConverter implements DynamoDBTypeConverter<String, Status> {

	@Override
	public String convert(Status status) {
		return status.name();
	}

	@Override
	public Status unconvert(String s) {
		return Status.valueOf(s);
	}
}
