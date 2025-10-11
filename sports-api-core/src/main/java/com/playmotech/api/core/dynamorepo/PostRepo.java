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
import com.playmotech.api.core.constants.PostStatus;
import com.playmotech.api.core.constants.Visibility;
import com.playmotech.api.core.dao.Post;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PostRepo {

    private final AmazonDynamoDB dynamoDB;
    private final String tableName;

    public PostRepo(final AmazonDynamoDB dynamoDB, @Value("${aws.dynamodb.posts.table-name}") final String tableName) {
        this.dynamoDB = dynamoDB;
        this.tableName = tableName;
    }

    public Post save(Post entity) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", new AttributeValue(entity.getId()));
        if (entity.getTitle() == null) {
            entity.setTitle("");
        }
        item.put("title", new AttributeValue(entity.getTitle()));
        item.put("body", new AttributeValue(entity.getBody()));
        item.put("visibility", new AttributeValue(entity.getVisibility().name()));
        if (CollectionUtils.isEmpty(entity.getMediaUrls())) {
            entity.setMediaUrls(new ArrayList<>());
        } else {
            item.put("mediaUrls", new AttributeValue().withSS(entity.getMediaUrls()));
        }

        if (CollectionUtils.isEmpty(entity.getThumbnailUrls())) {
            entity.setThumbnailUrls(new ArrayList<>());
        } else {
            item.put("thumbnailUrls", new AttributeValue().withSS(entity.getThumbnailUrls()));
        }

        item.put("academyId", new AttributeValue(entity.getAcademyId()));
        item.put("userId", new AttributeValue(entity.getUserId()));
        item.put("createdOn", new AttributeValue(entity.getCreatedOn()));
        item.put("updatedOn", new AttributeValue(entity.getUpdatedOn()));
        item.put("status", new AttributeValue(entity.getStatus().name()));
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
        return convertItemToPost(item);
    }

    public Optional<Post> findById(String id) {
        Map<String, AttributeValue> key = Map.of("id", new AttributeValue().withS(id));
        Map<String, AttributeValue> item = dynamoDB.getItem(tableName, key).getItem();

        return Optional.ofNullable(item != null ? convertItemToPost(item) : null);
    }

    public void delete(Post entity) {
        Map<String, AttributeValue> key = Map.of("id", new AttributeValue().withS(entity.getId()));
        DeleteItemRequest deleteRequest = new DeleteItemRequest().withTableName(tableName).withKey(key);

        dynamoDB.deleteItem(deleteRequest);
    }

    public List<Post> finalByVisibility(Visibility visibilityValue) {
        Map<String, AttributeValue> expressionValues = Map.of(":visibility",
                new AttributeValue().withS(visibilityValue.name()));
        ScanRequest scanRequest = new ScanRequest().withTableName(tableName)
                .withFilterExpression("visibility = :visibility").withExpressionAttributeValues(expressionValues);

        ScanResult scanResult = dynamoDB.scan(scanRequest);

        return convertItemsToPosts(scanResult.getItems());
    }

    public List<Post> finalByAcademyIdInAndVisibility(List<String> academyIds, Visibility visibility) {
        List<Post> result = new ArrayList<>();

        for (String academyId : academyIds) {
            Map<String, AttributeValue> expressionValues = Map.of(":academyId", new AttributeValue().withS(academyId),
                    ":visibility", new AttributeValue().withS(visibility.name()));

            ScanRequest scanRequest = new ScanRequest().withTableName(tableName)
                    .withFilterExpression("academyId = :academyId and visibility = :visibility")
                    .withExpressionAttributeValues(expressionValues);

            ScanResult scanResult = dynamoDB.scan(scanRequest);
            result.addAll(convertItemsToPosts(scanResult.getItems()));
        }
        return result;
    }

    private Post convertItemToPost(Map<String, AttributeValue> item) {
        Post post = new Post();
        post.setId(item.get("id").getS());
        post.setTitle(item.get("title").getS());
        post.setBody(item.get("body").getS());
        post.setVisibility(Visibility.valueOf(item.get("visibility").getS()));
        if (item.containsKey("mediaUrls")) {
            post.setMediaUrls(item.get("mediaUrls").getSS());
        }

        if (item.containsKey("thumbnailUrls")) {
            post.setThumbnailUrls(item.get("thumbnailUrls").getSS());
        }

        post.setAcademyId(item.get("academyId").getS());
        post.setUserId(item.get("userId").getS());
        post.setCreatedOn(item.get("createdOn").getS());
        post.setUpdatedOn(item.get("updatedOn").getS());
        post.setStatus(PostStatus.valueOf(item.get("status").getS()));
        if (item.containsKey("likes")) {
            post.setLikes(item.get("likes").getSS());
        }
        if (item.containsKey("dislikes")) {
            post.setDislikes(item.get("dislikes").getSS());
        }
        return post;
    }

    private List<Post> convertItemsToPosts(List<Map<String, AttributeValue>> items) {
        List<Post> posts = new ArrayList<>();
        for (Map<String, AttributeValue> item : items) {
            posts.add(convertItemToPost(item));
        }
        return posts;
    }

    public List<Post> findByVisibilityWithPagination(Visibility visibility, int page, int size,
            String sortField, String sortDirection) {
        Map<String, AttributeValue> expressionValues = Map.of(":visibility",
                new AttributeValue().withS(visibility.name()));

        // First scan all matching items using internal pagination with a scan segment
        // limit
        List<Post> allMatchingPosts = new ArrayList<>();
        Map<String, AttributeValue> lastKeyEvaluated = null;

        // Set a reasonable limit for each scan segment to force pagination
        final int scanSegmentSize = 20;

        do {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(tableName)
                    .withFilterExpression("visibility = :visibility")
                    .withExpressionAttributeValues(expressionValues)
                    .withLimit(scanSegmentSize); // Force pagination by limiting items evaluated

            // If we have a lastKeyEvaluated from a previous scan, use it
            if (lastKeyEvaluated != null) {
                scanRequest.setExclusiveStartKey(lastKeyEvaluated);
            }

            ScanResult scanResult = dynamoDB.scan(scanRequest);
            List<Post> currentBatch = convertItemsToPosts(scanResult.getItems());
            allMatchingPosts.addAll(currentBatch);

            // Update lastKeyEvaluated for the next iteration
            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            log.debug("Scan segment complete: found {} items, lastEvaluatedKey: {}",
                    currentBatch.size(), lastKeyEvaluated != null ? "present" : "null");

        } while (lastKeyEvaluated != null);

        log.info("Found {} posts with visibility {}", allMatchingPosts.size(), visibility);

        // Sort posts based on the specified field and direction
        sortPosts(allMatchingPosts, sortField, sortDirection);

        // Apply pagination
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allMatchingPosts.size());

        if (startIndex >= allMatchingPosts.size()) {
            return new ArrayList<>();
        }

        return allMatchingPosts.subList(startIndex, endIndex);
    }

    public List<Post> findByAcademyIdsAndVisibilityWithPagination(List<String> academyIds,
            Visibility visibility,
            int page, int size,
            String sortField, String sortDirection) {
        // Build filter expression for multiple academy IDs
        StringBuilder filterExpression = new StringBuilder("(");
        Map<String, AttributeValue> expressionValues = new HashMap<>();

        for (int i = 0; i < academyIds.size(); i++) {
            String academyKey = ":academyId" + i;
            if (i > 0)
                filterExpression.append(" OR ");
            filterExpression.append("academyId = ").append(academyKey);
            expressionValues.put(academyKey, new AttributeValue().withS(academyIds.get(i)));
        }

        filterExpression.append(") AND visibility = :visibility");
        expressionValues.put(":visibility", new AttributeValue().withS(visibility.name()));

        // Scan all matching items using internal pagination with a scan segment limit
        List<Post> allMatchingPosts = new ArrayList<>();
        Map<String, AttributeValue> lastKeyEvaluated = null;

        // Set a reasonable limit for each scan segment to force pagination
        final int scanSegmentSize = 20;

        do {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(tableName)
                    .withFilterExpression(filterExpression.toString())
                    .withExpressionAttributeValues(expressionValues)
                    .withLimit(scanSegmentSize); // Force pagination by limiting items evaluated

            // If we have a lastKeyEvaluated from a previous scan, use it
            if (lastKeyEvaluated != null) {
                scanRequest.setExclusiveStartKey(lastKeyEvaluated);
            }

            ScanResult scanResult = dynamoDB.scan(scanRequest);
            List<Post> currentBatch = convertItemsToPosts(scanResult.getItems());
            allMatchingPosts.addAll(currentBatch);

            // Update lastKeyEvaluated for the next iteration
            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            log.debug("Scan segment complete: found {} items, lastEvaluatedKey: {}",
                    currentBatch.size(), lastKeyEvaluated != null ? "present" : "null");

        } while (lastKeyEvaluated != null);

        log.info("Found {} posts with visibility {} for academy IDs {}", allMatchingPosts.size(), visibility,
                academyIds);

        // Sort posts based on the specified field and direction
        sortPosts(allMatchingPosts, sortField, sortDirection);

        // Apply pagination
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allMatchingPosts.size());

        if (startIndex >= allMatchingPosts.size()) {
            return new ArrayList<>();
        }

        return allMatchingPosts.subList(startIndex, endIndex);
    }

    private void sortPosts(List<Post> posts, String field, String direction) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        boolean isAscending = "asc".equalsIgnoreCase(direction);

        switch (field) {
            case "createdOn":
                posts.sort((p1, p2) -> {
                    int result = p1.getCreatedOn().compareTo(p2.getCreatedOn());
                    return isAscending ? result : -result;
                });
                break;
            case "updatedOn":
                posts.sort((p1, p2) -> {
                    int result = p1.getUpdatedOn().compareTo(p2.getUpdatedOn());
                    return isAscending ? result : -result;
                });
                break;
            case "title":
                posts.sort((p1, p2) -> {
                    int result = p1.getTitle().compareTo(p2.getTitle());
                    return isAscending ? result : -result;
                });
                break;
            default:
                posts.sort((p1, p2) -> {
                    int result = p1.getCreatedOn().compareTo(p2.getCreatedOn());
                    return isAscending ? result : -result;
                });
        }
    }

    public List<Post> findPostsForUserWithPagination(List<String> academyIds,
            int page, int size,
            String sortField, String sortDirection
    // ,List<String> statusFilter
    ) {
        // Build filter expression to get both public posts and private posts for user's
        // academies
        StringBuilder filterExpression = new StringBuilder("visibility = :publicVisibility");
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":publicVisibility", new AttributeValue().withS(Visibility.PUBLIC.name()));

        // Add private posts condition if user has academies
        if (!CollectionUtils.isEmpty(academyIds)) {
            filterExpression.append(" OR (visibility = :privateVisibility AND (");
            expressionValues.put(":privateVisibility", new AttributeValue().withS(Visibility.PRIVATE.name()));

            for (int i = 0; i < academyIds.size(); i++) {
                String academyKey = ":academyId" + i;
                if (i > 0)
                    filterExpression.append(" OR ");
                filterExpression.append("academyId = ").append(academyKey);
                expressionValues.put(academyKey, new AttributeValue().withS(academyIds.get(i)));
            }
            filterExpression.append("))");
        }

        // Add status filter if provided
        Map<String, String> expressionAttributeNames = new HashMap<>();
        // if (!CollectionUtils.isEmpty(statusFilter)) {
        // filterExpression.append(" AND (");
        // for (int i = 0; i < statusFilter.size(); i++) {
        // String statusKey = ":status" + i;
        // if (i > 0) filterExpression.append(" OR ");
        // filterExpression.append("#status = ").append(statusKey);
        // expressionValues.put(statusKey, new
        // AttributeValue().withS(statusFilter.get(i)));
        // }
        // filterExpression.append(")");
        // expressionAttributeNames.put("#status", "status");
        // }

        // Scan all matching items using internal pagination
        List<Post> allMatchingPosts = new ArrayList<>();
        Map<String, AttributeValue> lastKeyEvaluated = null;
        final int scanSegmentSize = 50; // Increased for better performance

        do {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(tableName)
                    .withFilterExpression(filterExpression.toString())
                    .withExpressionAttributeValues(expressionValues)
                    .withLimit(scanSegmentSize);

            // Add expression attribute names if needed (for reserved keywords like
            // 'status')
            if (!expressionAttributeNames.isEmpty()) {
                scanRequest.withExpressionAttributeNames(expressionAttributeNames);
            }

            // If we have a lastKeyEvaluated from a previous scan, use it
            if (lastKeyEvaluated != null) {
                scanRequest.withExclusiveStartKey(lastKeyEvaluated);
            }

            ScanResult scanResult = dynamoDB.scan(scanRequest);
            List<Post> currentBatch = convertItemsToPosts(scanResult.getItems());
            allMatchingPosts.addAll(currentBatch);

            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            log.debug("Scan segment complete: found {} items, lastEvaluatedKey: {}",
                    currentBatch.size(), lastKeyEvaluated != null ? "present" : "null");

        } while (lastKeyEvaluated != null);

        log.info("Found {} posts for user with academies {}", allMatchingPosts.size(), academyIds);

        // Sort posts based on the specified field and direction
        sortPosts(allMatchingPosts, sortField, sortDirection);

        // Apply pagination
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allMatchingPosts.size());

        if (startIndex >= allMatchingPosts.size()) {
            return new ArrayList<>();
        }

        return allMatchingPosts.subList(startIndex, endIndex);
    }

    public List<Post> findPostsByAcademyIds(List<String> academyIds,
            int page, int size,
            String sortField, String sortDirection
    // ,List<String> statusFilter
    ) {

        if (CollectionUtils.isEmpty(academyIds)) {
            return new ArrayList<>();
        }

        // Build filter expression to get ALL posts (public + private) from specified
        // academies only
        StringBuilder filterExpression = new StringBuilder("(");
        Map<String, AttributeValue> expressionValues = new HashMap<>();

        // Add academy filter - get posts from these academies regardless of visibility
        for (int i = 0; i < academyIds.size(); i++) {
            String academyKey = ":academyId" + i;
            if (i > 0)
                filterExpression.append(" OR ");
            filterExpression.append("academyId = ").append(academyKey);
            expressionValues.put(academyKey, new AttributeValue().withS(academyIds.get(i)));
        }
        filterExpression.append(")");

        // Add status filter if provided
        Map<String, String> expressionAttributeNames = new HashMap<>();
        // if (!CollectionUtils.isEmpty(statusFilter)) {
        // filterExpression.append(" AND (");
        // for (int i = 0; i < statusFilter.size(); i++) {
        // String statusKey = ":status" + i;
        // if (i > 0) filterExpression.append(" OR ");
        // filterExpression.append("#status = ").append(statusKey);
        // expressionValues.put(statusKey, new
        // AttributeValue().withS(statusFilter.get(i)));
        // }
        // filterExpression.append(")");
        // expressionAttributeNames.put("#status", "status");
        // }

        // Scan all matching items using internal pagination
        List<Post> allMatchingPosts = new ArrayList<>();
        Map<String, AttributeValue> lastKeyEvaluated = null;
        final int scanSegmentSize = 50;

        do {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(tableName)
                    .withFilterExpression(filterExpression.toString())
                    .withExpressionAttributeValues(expressionValues)
                    .withLimit(scanSegmentSize);

            // Add expression attribute names if needed
            if (!expressionAttributeNames.isEmpty()) {
                scanRequest.withExpressionAttributeNames(expressionAttributeNames);
            }

            if (lastKeyEvaluated != null) {
                scanRequest.withExclusiveStartKey(lastKeyEvaluated);
            }

            ScanResult scanResult = dynamoDB.scan(scanRequest);
            List<Post> currentBatch = convertItemsToPosts(scanResult.getItems());
            allMatchingPosts.addAll(currentBatch);

            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            log.debug("Scan segment complete for academy filtering: found {} items, lastEvaluatedKey: {}",
                    currentBatch.size(), lastKeyEvaluated != null ? "present" : "null");

        } while (lastKeyEvaluated != null);

        log.info("Found {} posts for academies {} with org/package filtering", allMatchingPosts.size(), academyIds);

        // Sort posts based on the specified field and direction
        sortPosts(allMatchingPosts, sortField, sortDirection);

        // Apply pagination
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allMatchingPosts.size());

        if (startIndex >= allMatchingPosts.size()) {
            return new ArrayList<>();
        }

        return allMatchingPosts.subList(startIndex, endIndex);
    }

    public long countPostsForUser(List<String> academyIds) {
        // Build filter expression to get both public posts and private posts for user's
        // academies
        StringBuilder filterExpression = new StringBuilder("visibility = :publicVisibility");
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":publicVisibility", new AttributeValue().withS(Visibility.PUBLIC.name()));

        // Add private posts condition if user has academies
        if (!CollectionUtils.isEmpty(academyIds)) {
            filterExpression.append(" OR (visibility = :privateVisibility AND (");
            expressionValues.put(":privateVisibility", new AttributeValue().withS(Visibility.PRIVATE.name()));

            for (int i = 0; i < academyIds.size(); i++) {
                String academyKey = ":academyId" + i;
                if (i > 0)
                    filterExpression.append(" OR ");
                filterExpression.append("academyId = ").append(academyKey);
                expressionValues.put(academyKey, new AttributeValue().withS(academyIds.get(i)));
            }
            filterExpression.append("))");
        }

        // Scan all matching items using internal pagination
        long count = 0;
        Map<String, AttributeValue> lastKeyEvaluated = null;
        final int scanSegmentSize = 100; // Increased for better performance

        do {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(tableName)
                    .withFilterExpression(filterExpression.toString())
                    .withExpressionAttributeValues(expressionValues)
                    .withSelect("COUNT") // Just get count, not the items
                    .withLimit(scanSegmentSize);

            // If we have a lastKeyEvaluated from a previous scan, use it
            if (lastKeyEvaluated != null) {
                scanRequest.withExclusiveStartKey(lastKeyEvaluated);
            }

            ScanResult scanResult = dynamoDB.scan(scanRequest);
            count += scanResult.getCount();

            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            log.debug("Count scan segment complete: found {} items, lastEvaluatedKey: {}",
                    scanResult.getCount(), lastKeyEvaluated != null ? "present" : "null");

        } while (lastKeyEvaluated != null);

        log.info("Counted {} posts for user with academies {}", count, academyIds);

        return count;
    }

    public long countPostsByAcademyIds(List<String> academyIds) {
        if (CollectionUtils.isEmpty(academyIds)) {
            return 0;
        }

        // Build filter expression to get ALL posts (public + private) from specified
        // academies only
        StringBuilder filterExpression = new StringBuilder("(");
        Map<String, AttributeValue> expressionValues = new HashMap<>();

        // Add academy filter - get posts from these academies regardless of visibility
        for (int i = 0; i < academyIds.size(); i++) {
            String academyKey = ":academyId" + i;
            if (i > 0)
                filterExpression.append(" OR ");
            filterExpression.append("academyId = ").append(academyKey);
            expressionValues.put(academyKey, new AttributeValue().withS(academyIds.get(i)));
        }
        filterExpression.append(")");

        // Scan all matching items using internal pagination
        long count = 0;
        Map<String, AttributeValue> lastKeyEvaluated = null;
        final int scanSegmentSize = 100;

        do {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(tableName)
                    .withFilterExpression(filterExpression.toString())
                    .withExpressionAttributeValues(expressionValues)
                    .withSelect("COUNT") // Just get count, not the items
                    .withLimit(scanSegmentSize);

            if (lastKeyEvaluated != null) {
                scanRequest.withExclusiveStartKey(lastKeyEvaluated);
            }

            ScanResult scanResult = dynamoDB.scan(scanRequest);
            count += scanResult.getCount();

            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            log.debug("Count scan segment complete for academy filtering: found {} items, lastEvaluatedKey: {}",
                    scanResult.getCount(), lastKeyEvaluated != null ? "present" : "null");

        } while (lastKeyEvaluated != null);

        log.info("Counted {} posts for academies {} with org/package filtering", count, academyIds);

        return count;
    }
}
