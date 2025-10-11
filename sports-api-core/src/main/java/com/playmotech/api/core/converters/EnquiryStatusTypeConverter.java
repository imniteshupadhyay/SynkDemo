package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.EnquiryStatus;

public class EnquiryStatusTypeConverter implements DynamoDBTypeConverter<String, EnquiryStatus> {

	@Override
	public String convert(EnquiryStatus enquiryStatus) {
		return enquiryStatus.name();
	}

	@Override
	public EnquiryStatus unconvert(String s) {
		return EnquiryStatus.valueOf(s);
	}
}