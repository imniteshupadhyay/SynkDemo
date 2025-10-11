package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.UserType;

public class UserTypeTypeConverter implements DynamoDBTypeConverter<String, UserType> {

	@Override
	public String convert(UserType userType) {
		return userType.name();
	}

	@Override
	public UserType unconvert(String s) {
		return UserType.valueOf(s);
	}
}
