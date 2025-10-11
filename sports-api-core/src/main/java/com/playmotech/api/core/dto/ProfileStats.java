package com.playmotech.api.core.dto;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import lombok.Data;

/**
 * Created By: deep.patel
 **/

@DynamoDBDocument
@Data
public class ProfileStats {
	private long following;
	private long followers;
	private long profileViews;
}
