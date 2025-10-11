package com.playmotech.api.core.dynamorepo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

//import com.playmotech.api.core.dao.AcademyLead;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.playmotech.api.core.dao.GroupMessage;
import com.playmotech.api.core.dao.Post;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GroupMessageRepo {

	private final AmazonDynamoDB dynamoDB;
	private final String tableName;

	public GroupMessageRepo(final AmazonDynamoDB dynamoDB,
			@Value("${aws.dynamodb.group-messages.table-name}") final String tableName) {
		this.dynamoDB = dynamoDB;
		this.tableName = tableName;
	}

	public GroupMessage save(GroupMessage entity) {
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", new AttributeValue(entity.getId()));
		item.put("groupId", new AttributeValue(entity.getGroupId()));
		item.put("academyId", new AttributeValue(entity.getAcademyId()));
		item.put("time", new AttributeValue().withN(Long.toString(entity.getTime())));
		item.put("senderUserId", new AttributeValue(entity.getSenderUserId()));
		item.put("message", new AttributeValue(entity.getMessage()));
		item.put("mediaUrl", new AttributeValue(entity.getMediaUrl()).withNULL(true));

		PutItemRequest request = new PutItemRequest().withTableName(tableName).withItem(item);

		dynamoDB.putItem(request);
		return convertItemToGroupMessage(item);
	}

	public void delete(Post entity) {
		Map<String, AttributeValue> key = Map.of("id", new AttributeValue().withS(entity.getId()));
		DeleteItemRequest deleteRequest = new DeleteItemRequest().withTableName(tableName).withKey(key);

		dynamoDB.deleteItem(deleteRequest);
	}

	public List<GroupMessage> findByAcademyIdAndGroupId(String academyId, String groupId) {
		Map<String, AttributeValue> expressionValues = Map.of(":academyId", new AttributeValue().withS(academyId),
				":groupId", new AttributeValue().withS(groupId));

		ScanRequest scanRequest = new ScanRequest().withTableName(tableName)
				.withFilterExpression("academyId = :academyId and groupId = :groupId")
				.withExpressionAttributeValues(expressionValues);

		ScanResult scanResult = dynamoDB.scan(scanRequest);
		if (scanResult != null && scanResult.getCount() > 0) {
			return convertItemsToGroupMessages(scanResult.getItems());
		}
		return new ArrayList<>();
	}

//    // Converts List<Message> to List<AttributeValue>
//    private List<AttributeValue> convertMessagesToAttributeValues(List<Message> messages) {
//        return messages.stream()
//                .map(this::convertMessageToAttributeValue)
//                .toList();
//    }

	// Converts a Message object to AttributeValue
//    private AttributeValue convertMessageToAttributeValue(Message message) {
//        Map<String, AttributeValue> messageMap = new HashMap<>();
//        messageMap.put("time", new AttributeValue().withN(Long.toString(message.getTime())));
//        messageMap.put("senderUserId", new AttributeValue(message.getSenderUserId()));
//        messageMap.put("message", new AttributeValue(message.getMessage()));
//        messageMap.put("mediaUrl", new AttributeValue(message.getMediaUrl()));
//        return new AttributeValue().withM(messageMap); // Wrap in an AttributeValue Map
//    }

	public List<GroupMessage> convertItemsToGroupMessages(List<Map<String, AttributeValue>> items) {
		List<GroupMessage> groupMessages = new ArrayList<>();
		for (Map<String, AttributeValue> item : items) {
			groupMessages.add(convertItemToGroupMessage(item));
		}
		return groupMessages;
	}

	public GroupMessage convertItemToGroupMessage(Map<String, AttributeValue> item) {
		GroupMessage groupMessage = new GroupMessage();
		groupMessage.setId(item.get("id").getS());
		groupMessage.setGroupId(item.get("groupId").getS());
		groupMessage.setAcademyId(item.get("academyId").getS());
		groupMessage.setMediaUrl(item.get("mediaUrl").getS());
		groupMessage.setMessage(item.get("message").getS());
		groupMessage.setSenderUserId(item.get("senderUserId").getS());
		groupMessage.setTime(Long.parseLong(item.get("time").getN()));
		return groupMessage;
	}

	// Converts List<AttributeValue> to List<Message>
//    private List<Message> convertAttributeValuesToMessages(List<AttributeValue> messageAttributes) {
//        List<Message> messages = new ArrayList<>();
//        for (AttributeValue attributeValue : messageAttributes) {
//            messages.add(convertAttributeValueToMessage(attributeValue.getM()));
//        }
//        return messages;
//    }

	// Converts a Map<String, AttributeValue> to a Message
//    private Message convertAttributeValueToMessage(Map<String, AttributeValue> messageMap) {
//        Message message = new Message();
//        message.setTime(Long.parseLong(messageMap.get("time").getN()));
//        message.setSenderUserId(messageMap.get("senderUserId").getS());
//        message.setMessage(messageMap.get("message").getS());
//        message.setMediaUrl(messageMap.get("mediaUrl").getS());
//        return message;
//    }
}
