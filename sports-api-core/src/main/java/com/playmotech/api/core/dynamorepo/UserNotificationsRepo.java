package com.playmotech.api.core.dynamorepo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

//import com.playmotech.api.core.dao.AcademyLead;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.playmotech.api.core.constants.CtaType;
//import com.playmotech.api.core.dao.Message;
import com.playmotech.api.core.dao.Notification;
import com.playmotech.api.core.dao.UserNotification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserNotificationsRepo {

	private final AmazonDynamoDB dynamoDB;
	private final String tableName;

	public UserNotificationsRepo(final AmazonDynamoDB dynamoDB,
			@Value("${aws.dynamodb.user-notifications.table-name}") final String tableName) {
		this.dynamoDB = dynamoDB;
		this.tableName = tableName;
	}

	public UserNotification save(UserNotification entity) {
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", new AttributeValue(entity.getId()));
		if (CollectionUtils.isEmpty(entity.getNotifications())) {
			entity.setNotifications(new ArrayList<>());
		} else {
			item.put("notifications",
					new AttributeValue().withL(convertNotificationsToAttributeValues(entity.getNotifications())));
		}

		PutItemRequest request = new PutItemRequest().withTableName(tableName).withItem(item);

		dynamoDB.putItem(request);
		return convertItemToUserNotification(item);
	}

	public Optional<UserNotification> findById(String id) {

		Map<String, AttributeValue> key = new HashMap<>();
		key.put("id", new AttributeValue(id));

		GetItemRequest request = new GetItemRequest().withTableName(tableName).withKey(key);

		// Fetch the item from DynamoDB
		GetItemResult result = dynamoDB.getItem(request);
		if (result.getItem() == null) {
			return Optional.empty();
		}
		return Optional.of(convertItemToUserNotification(result.getItem()));
	}

	// Converts List<Message> to List<AttributeValue>
	private List<AttributeValue> convertNotificationsToAttributeValues(List<Notification> notifications) {
		return notifications.stream().map(this::convertNotificationToAttributeValue).toList();
	}

	// Converts a Message object to AttributeValue
	private AttributeValue convertNotificationToAttributeValue(Notification notification) {
		Map<String, AttributeValue> notificationMap = new HashMap<>();
		notificationMap.put("notification", new AttributeValue(notification.getNotification()));
		notificationMap.put("ctaType", new AttributeValue(notification.getCtaType().name()));
		notificationMap.put("cta", new AttributeValue(notification.getCta()));
		notificationMap.put("extraArgs", new AttributeValue().withM(getExtraArgs(notification.getExtraArgs())));
		notificationMap.put("epochTimeInMillis",
				new AttributeValue().withN(Long.toString(notification.getEpochTimeInMillis())));
		return new AttributeValue().withM(notificationMap); // Wrap in an AttributeValue Map
	}

	private Map<String, AttributeValue> getExtraArgs(Map<String, String> extraArgs) {
		Map<String, AttributeValue> extraArgsM = new HashMap<>();
		extraArgs.forEach((k, v) -> extraArgsM.put(k, new AttributeValue(v)));
		return extraArgsM;
	}

	private Map<String, String> getExtraArgsRev(Map<String, AttributeValue> extraArgsM) {
		Map<String, String> extraArgs = new HashMap<>();
		extraArgsM.forEach((k, v) -> extraArgs.put(k, v.getS()));
		return extraArgs;
	}

	public UserNotification convertItemToUserNotification(Map<String, AttributeValue> item) {
		UserNotification userNotification = new UserNotification();
		userNotification.setId(item.get("id").getS());
		// Handle the list of messages
		if (item.containsKey("notifications")) {
			userNotification.setNotifications(convertAttributeValuesToNotifications(item.get("notifications").getL()));
		}

		return userNotification;
	}

	// Converts List<AttributeValue> to List<Notification>
	private List<Notification> convertAttributeValuesToNotifications(List<AttributeValue> notificationAttributes) {
		List<Notification> notifications = new ArrayList<>();
		for (AttributeValue attributeValue : notificationAttributes) {
			notifications.add(convertAttributeValueToNotification(attributeValue.getM()));
		}
		return notifications;
	}

	// Converts a Map<String, AttributeValue> to a Notification
	private Notification convertAttributeValueToNotification(Map<String, AttributeValue> messageMap) {
		Notification notification = new Notification();
		notification.setEpochTimeInMillis(Long.parseLong(messageMap.get("epochTimeInMillis").getN()));
		notification.setNotification(messageMap.get("notification").getS());
		notification.setCtaType(CtaType.valueOf(messageMap.get("ctaType").getS()));
		notification.setCta(messageMap.get("cta").getS());
		notification.setExtraArgs(getExtraArgsRev(messageMap.get("extraArgs").getM()));
		return notification;
	}

}
