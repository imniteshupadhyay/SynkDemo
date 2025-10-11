package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.PostStatus;

public class PostStatusTypeConverter implements DynamoDBTypeConverter<String, PostStatus> {

	@Override
	public String convert(PostStatus postStatus) {
		return postStatus.name();
	}

	@Override
	public PostStatus unconvert(String s) {
		return PostStatus.valueOf(s);
	}
}
