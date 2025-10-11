package com.playmotech.api.core.dynamorepo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.playmotech.api.core.dao.Comment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CommentRepo {

	private final AmazonDynamoDB dynamoDB;
	private final String tableName;

	public CommentRepo(final AmazonDynamoDB dynamoDB,
			@Value("${aws.dynamodb.comments.table-name}") final String tableName) {
		this.dynamoDB = dynamoDB;
		this.tableName = tableName;
	}

	public Comment save(Comment entity) {
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", new AttributeValue(entity.getId()));
		item.put("postId", new AttributeValue(entity.getPostId()));
		item.put("userId", new AttributeValue(entity.getUserId()));
		item.put("text", new AttributeValue(entity.getText()));
		item.put("createdOn", new AttributeValue(entity.getCreatedOn()));

		if (CollectionUtils.isEmpty(entity.getLikes())) {
			entity.setLikes(new ArrayList<>());
		} else {
			item.put("likes", new AttributeValue().withSS(entity.getLikes()));
		}

		if (CollectionUtils.isEmpty(entity.getDislikes())) {
			entity.setDislikes(new ArrayList<>());
		} else {
			item.put("dislikes", new AttributeValue().withSS(entity.getDislikes()));
		}

		PutItemRequest request = new PutItemRequest().withTableName(tableName).withItem(item);

		dynamoDB.putItem(request);
		return convertItemToComment(item);
	}

	public Optional<Comment> findById(String id) {
		Map<String, AttributeValue> key = Map.of("id", new AttributeValue().withS(id));
		Map<String, AttributeValue> item = dynamoDB.getItem(tableName, key).getItem();

		return Optional.ofNullable(item != null ? convertItemToComment(item) : null);
	}

	public void delete(Comment entity) {
		Map<String, AttributeValue> key = Map.of("id", new AttributeValue().withS(entity.getId()));
		DeleteItemRequest deleteRequest = new DeleteItemRequest().withTableName(tableName).withKey(key);

		dynamoDB.deleteItem(deleteRequest);
	}

	public List<Comment> findByPostId(String postId) {
		Map<String, AttributeValue> expressionValues = Map.of(":postId", new AttributeValue().withS(postId));
		ScanRequest scanRequest = new ScanRequest().withTableName(tableName).withFilterExpression("postId = :postId")
				.withExpressionAttributeValues(expressionValues);

		ScanResult scanResult = dynamoDB.scan(scanRequest);

		return convertItemsToComments(scanResult.getItems());
	}

	public List<Comment> findByPostIdIn(List<String> postIds) {
		List<Comment> result = new ArrayList<>();
		for (String postId : postIds) {
			Map<String, AttributeValue> expressionValues = Map.of(":postId", new AttributeValue().withS(postId));

			ScanRequest scanRequest = new ScanRequest().withTableName(tableName)
					.withFilterExpression("postId = :postId").withExpressionAttributeValues(expressionValues);

			ScanResult scanResult = dynamoDB.scan(scanRequest);
			result.addAll(convertItemsToComments(scanResult.getItems()));
		}
		return result;
	}

	private Comment convertItemToComment(Map<String, AttributeValue> item) {
		Comment comment = new Comment();
		comment.setId(item.get("id").getS());
		comment.setPostId(item.get("postId").getS());
		comment.setUserId(item.get("userId").getS());
		comment.setText(item.get("text").getS());
		comment.setCreatedOn(item.get("createdOn").getS());
		if (item.containsKey("likes")) {
			comment.setLikes(item.get("likes").getSS());
		}
		if (item.containsKey("dislikes")) {
			comment.setDislikes(item.get("dislikes").getSS());
		}

		return comment;
	}

	private List<Comment> convertItemsToComments(List<Map<String, AttributeValue>> items) {
		List<Comment> comments = new ArrayList<>();
		for (Map<String, AttributeValue> item : items) {
			comments.add(convertItemToComment(item));
		}
		return comments;
	}
}
