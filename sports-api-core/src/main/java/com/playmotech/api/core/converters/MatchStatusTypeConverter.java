package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.MatchStatus;

public class MatchStatusTypeConverter implements DynamoDBTypeConverter<String, MatchStatus> {

	@Override
	public String convert(MatchStatus matchStatus) {
		return matchStatus.name();
	}

	@Override
	public MatchStatus unconvert(String s) {
		return MatchStatus.valueOf(s);
	}
}
