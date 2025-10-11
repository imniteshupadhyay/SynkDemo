package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.IncentiveSourceType;
import com.playmotech.api.core.constants.PostStatus;
import com.playmotech.api.core.constants.ReportStatus;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.constants.Visibility;
import com.playmotech.api.core.dao.Comment;
import com.playmotech.api.core.dao.Post;
import com.playmotech.api.core.dao_postgres.CoachAcademyMapping;
import com.playmotech.api.core.dao_postgres.ReportedPost;
import com.playmotech.api.core.dao_postgres.TraineeAcademyMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.AcademyMinDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.PostDto;
import com.playmotech.api.core.dto.PostRequestDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.dynamorepo.CommentRepo;
import com.playmotech.api.core.dynamorepo.PostRepo;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.CoachAcademyMappingRepo;
import com.playmotech.api.core.repo.ReportedPostRepo;
import com.playmotech.api.core.repo.TraineeAcademyMappingRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IPostService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.validation.CoachRoleValidator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PostService implements IPostService {
    @Value("${post-media-base-url}")
    private String postMediaBaseUrl;

    @Value("${storage.post-media-bucket}")
    private String postMediaBucket;

    private final PostRepo postRepository;
    private final IUserProfileService userProfileService;
    private final TraineeAcademyMappingRepo traineeAcademyMappingRepo;
    private final CoachAcademyMappingRepo coachAcademyMappingRepo;
    private final IStorageService storageService;
    private final IAcademyService academyService;
    private final ModelMapper modelMapper;
    private final CommentRepo commentRepo;
    private final ReportedPostRepo reportedPostRepo;
    private final CoachIncentiveService coachIncentiveService;

    public PostService(PostRepo postRepository, IUserProfileService userProfileService,
            TraineeAcademyMappingRepo traineeAcademyMappingRepo, CoachAcademyMappingRepo coachAcademyMappingRepo,
            IStorageService storageService, IAcademyService academyService, ModelMapper modelMapper,
            CommentRepo commentRepo, ReportedPostRepo reportedPostRepo, CoachIncentiveService coachIncentiveService) {
        this.postRepository = postRepository;
        this.userProfileService = userProfileService;
        this.traineeAcademyMappingRepo = traineeAcademyMappingRepo;
        this.coachAcademyMappingRepo = coachAcademyMappingRepo;
        this.storageService = storageService;
        this.academyService = academyService;
        this.modelMapper = modelMapper;
        this.commentRepo = commentRepo;
        this.reportedPostRepo = reportedPostRepo;
        this.coachIncentiveService = coachIncentiveService;
    }

    @Override
    public PostDto createPost(PostRequestDto createPostDto, List<FileObjectDto> fileObjectDtos, UserDetail userDetail,
            List<FileObjectDto> thumbnailFileObjectDtos) throws ResourceException {
        Optional<CoachAcademyMapping> coachAcademyMapping = Optional.empty();
        Post post = new Post();
        post.setId(UUID.randomUUID().toString());
        post.setCreatedOn(Instant.now().toString());
        post.setUpdatedOn(Instant.now().toString());
        UserProfileDto userProfileDto = userProfileService.getUserProfileById(userDetail.getUserId());
        // if (userProfileDto == null ||
        // !userProfileDto.getId().equalsIgnoreCase(userDetail.getUserId())) {
        // throw new ResourceException(ErrorCodes.USER_DOES_NOT_EXIST, "User not
        // found");
        // }
        if (StringUtils.hasText(createPostDto.getAcademyId())) {
            if (userProfileDto.getUserType() == UserType.PLAYER) {
                Optional<TraineeAcademyMapping> traineeAcademyMappings = traineeAcademyMappingRepo
                        .findByAcademy_IdAndTraineeUserProfile_Id(createPostDto.getAcademyId(), userDetail.getUserId());
                if (traineeAcademyMappings.isEmpty()) {
                    throw new ResourceException(ErrorCodes.USER_NOT_ASSOCIATED_WITH_ACADEMY,
                            "User not associated with academy");
                }
            }
            if (userProfileDto.getUserType() == UserType.COACH) {
                coachAcademyMapping = coachAcademyMappingRepo
                        .findByAcademy_IdAndCoachUserProfile_Id(createPostDto.getAcademyId(), userDetail.getUserId());
                if (coachAcademyMapping.isEmpty()) {
                    throw new ResourceException(ErrorCodes.USER_NOT_ASSOCIATED_WITH_ACADEMY,
                            "User not associated with academy");
                }
            }
            post.setAcademyId(createPostDto.getAcademyId());
        }
        post.setDislikes(new ArrayList<>());
        post.setLikes(new ArrayList<>());
        post.setTitle(createPostDto.getTitle());
        post.setBody(createPostDto.getBody());
        post.setStatus(PostStatus.APPROVED);
        post.setVisibility(createPostDto.getVisibility());
        post.setUserId(userDetail.getUserId());

        // media fileObjects
        if (!CollectionUtils.isEmpty(fileObjectDtos)) {
            List<String> mediaUrlPaths = new ArrayList<>();
            List<String> thumbnailUrlPaths = new ArrayList<>();
            UUID randomUuid = null;
            for (FileObjectDto fileObjectDto : fileObjectDtos) {
                randomUuid = UUID.randomUUID();
                String prefix = "post-media-urls/" + userDetail.getUserId() + "/" + createPostDto.getAcademyId() + "/"
                        + randomUuid + "_" + fileObjectDto.getOriginalFilename();
                storageService.upload(postMediaBucket, prefix, fileObjectDto.getContent(),
                        fileObjectDto.getContentType());
                mediaUrlPaths.add(prefix);
            }
            post.setMediaUrls(mediaUrlPaths);

            // thumbnail fileObjects
            if (!CollectionUtils.isEmpty(thumbnailFileObjectDtos)) {
                for (FileObjectDto fileObjectDto : thumbnailFileObjectDtos) {
                    String thumbnailPrefix = "post-media-urls/" + userDetail.getUserId() + "/"
                            + createPostDto.getAcademyId() + "/" + "thumbnail/" + randomUuid + "_"
                            + fileObjectDto.getOriginalFilename();

                    storageService.upload(postMediaBucket, thumbnailPrefix, fileObjectDto.getContent(),
                            fileObjectDto.getContentType());
                    thumbnailUrlPaths.add(thumbnailPrefix);
                }
            }
            post.setThumbnailUrls(thumbnailUrlPaths);
        }
        Post post1 = postRepository.save(post);
        AcademyMinDto academyDto = null;
        if (StringUtils.hasText(post.getAcademyId())) {
            academyDto = modelMapper.map(academyService.getAcademyById(createPostDto.getAcademyId()),
                    AcademyMinDto.class);
        }

        // Updated to pass the current user ID
        PostDto postDto = new PostDto(post1, modelMapper.map(userProfileDto, UserProfileMinDto.class), academyDto, 0,
                userDetail.getUserId());

        if (!CollectionUtils.isEmpty(post.getMediaUrls())) {
            postDto.setMediaUrls(post.getMediaUrls().stream().map(mediaUrl -> postMediaBaseUrl + mediaUrl)
                    .collect(Collectors.toList()));
        }

        if (!CollectionUtils.isEmpty(post.getThumbnailUrls())) {
            postDto.setThumbnailUrls(post.getThumbnailUrls().stream()
                    .map(thumbnailUrl -> postMediaBaseUrl + thumbnailUrl).collect(Collectors.toList()));
        }

        if (coachAcademyMapping.isPresent() &&
                coachAcademyMapping.get().getRoleId() != null &&
                CoachRoleValidator.ACCEPTABLE_ROLE_IDS.contains(coachAcademyMapping.get().getRoleId())) {
            String coachUserIdFinal = coachAcademyMapping.get().getCoachUserProfile().getId();
            log.info(
                    "[PostService] The user is indeed a coach in the provided academy!!! Rewarding the coach with some points :)");

            CompletableFuture.runAsync(() -> {
                try {
                    rewardCoach(coachUserIdFinal, post1);
                } catch (ResourceException e) {
                    log.error("Unable to reward coach: coachId: {}", coachUserIdFinal, e);
                    log.error("[PostService] Check the (coach_point_transaction_error) table");
                }
            });
        } else {
            log.warn(
                    "No coach academy present found. Cannot determine if the user is coach in the academy. Skipping the coach incentive.");
        }
        return postDto;
    }

    private void rewardCoach(String coachUserId, Post post) throws ResourceException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("postId", post.getId());
        metadata.put("createdBy", post.getUserId());
        metadata.put("academyId", post.getAcademyId() != null ? post.getAcademyId() : null);
        metadata.put("createdOn", post.getCreatedOn() != null ? post.getCreatedOn() : null);

        coachIncentiveService.awardPoints(
                coachUserId,
                IncentiveSourceType.TIMELINE,
                post.getId(),
                post.getAcademyId(),
                metadata);
    }

    @Override
    public PostDto updatePost(String postId, PostRequestDto updatePostDto, List<FileObjectDto> fileObjectDtos,
            UserDetail userDetail, List<FileObjectDto> thumbnailFileObjectDtos) throws ResourceException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        post.setUpdatedOn(Instant.now().toString());

        UserProfileDto userProfileDto = userProfileService.getUserProfileById(userDetail.getUserId());
        if (userProfileDto == null || !userProfileDto.getId().equalsIgnoreCase(userDetail.getUserId())) {
            throw new ResourceException(ErrorCodes.USER_DOES_NOT_EXIST, "User not found");
        }

        // Only check academy association if academyId is provided
        if (StringUtils.hasText(updatePostDto.getAcademyId())) {
            if (userProfileDto.getUserType() == UserType.PLAYER) {
                Optional<TraineeAcademyMapping> traineeAcademyMappings = traineeAcademyMappingRepo
                        .findByAcademy_IdAndTraineeUserProfile_Id(updatePostDto.getAcademyId(), userDetail.getUserId());
                if (traineeAcademyMappings.isEmpty()) {
                    throw new ResourceException(ErrorCodes.USER_NOT_ASSOCIATED_WITH_ACADEMY,
                            "User not associated with academy");
                }
            } else if (userProfileDto.getUserType() == UserType.COACH) {
                Optional<CoachAcademyMapping> coachAcademyMapping = coachAcademyMappingRepo
                        .findByAcademy_IdAndCoachUserProfile_Id(updatePostDto.getAcademyId(), userDetail.getUserId());
                if (coachAcademyMapping.isEmpty()) {
                    throw new ResourceException(ErrorCodes.USER_NOT_ASSOCIATED_WITH_ACADEMY,
                            "User not associated with academy");
                }
            }
        }

        post.setAcademyId(updatePostDto.getAcademyId());
        post.setTitle(updatePostDto.getTitle());
        post.setBody(updatePostDto.getBody());
        post.setVisibility(updatePostDto.getVisibility());

        if (!CollectionUtils.isEmpty(fileObjectDtos)) {
            List<String> mediaUrlPaths = new ArrayList<>();
            List<String> thumbnailUrlPaths = new ArrayList<>();
            UUID randomUuid = null;
            for (FileObjectDto fileObjectDto : fileObjectDtos) {
                randomUuid = UUID.randomUUID();
                // Fixed: Use consistent prefix pattern with createPost
                String prefix = "post-media-urls/" + userDetail.getUserId() + "/" + updatePostDto.getAcademyId() + "/"
                        + randomUuid + "_" + fileObjectDto.getOriginalFilename();
                storageService.upload(postMediaBucket, prefix, fileObjectDto.getContent(),
                        fileObjectDto.getContentType());
                mediaUrlPaths.add(prefix);
            }
            post.setMediaUrls(mediaUrlPaths);

            if (!CollectionUtils.isEmpty(thumbnailFileObjectDtos)) {
                for (FileObjectDto fileObjectDto : thumbnailFileObjectDtos) {
                    String thumbnailPrefix = "post-media-urls/" + userDetail.getUserId() + "/"
                            + updatePostDto.getAcademyId() + "/" + "thumbnail/" + randomUuid + "_"
                            + fileObjectDto.getOriginalFilename();

                    storageService.upload(postMediaBucket, thumbnailPrefix, fileObjectDto.getContent(),
                            fileObjectDto.getContentType());
                    thumbnailUrlPaths.add(thumbnailPrefix);
                }
            }
            post.setThumbnailUrls(thumbnailUrlPaths);
        }

        Post updatedPost = postRepository.save(post);
        AcademyMinDto academyDto = null;
        if (StringUtils.hasText(post.getAcademyId())) {
            academyDto = modelMapper.map(academyService.getAcademyById(updatedPost.getAcademyId()),
                    AcademyMinDto.class);
        }
        List<Comment> comments = commentRepo.findByPostId(updatedPost.getId());

        // Updated to pass the current user ID
        PostDto postDto = new PostDto(updatedPost, modelMapper.map(userProfileDto, UserProfileMinDto.class), academyDto,
                CollectionUtils.isEmpty(comments) ? 0 : comments.size(), userDetail.getUserId());

        if (!CollectionUtils.isEmpty(post.getMediaUrls())) {
            postDto.setMediaUrls(post.getMediaUrls().stream().map(mediaUrl -> postMediaBaseUrl + mediaUrl)
                    .collect(Collectors.toList()));
        }

        // Added missing thumbnail URLs mapping
        if (!CollectionUtils.isEmpty(post.getThumbnailUrls())) {
            postDto.setThumbnailUrls(post.getThumbnailUrls().stream()
                    .map(thumbnailUrl -> postMediaBaseUrl + thumbnailUrl).collect(Collectors.toList()));
        }

        return postDto;
    }

    @Override
    public List<PostDto> getAllPostsForUser(String userId, String orgId, String packageName) throws ResourceException {
        UserProfileDto userProfileDto = userProfileService.getUserProfileById(userId);

        // if (userProfileDto == null ||
        // !userProfileDto.getId().equalsIgnoreCase(userId)) {
        // throw new ResourceException(ErrorCodes.USER_DOES_NOT_EXIST, "User not
        // found");
        // }

        List<Post> publicPosts = postRepository.finalByVisibility(Visibility.PUBLIC);

        List<Post> academyPosts = new ArrayList<>();
        List<Post> allPosts = new ArrayList<>();
        if (userProfileDto.getUserType() == UserType.PLAYER) {
            List<TraineeAcademyMapping> traineeAcademyMappings = traineeAcademyMappingRepo
                    .findByTraineeUserProfile_Id(userId);
            // Commenting this out for PLAYERS who are not mapped with academy
            // if (traineeAcademyMappings.isEmpty()) {
            // throw new ResourceException(ErrorCodes.USER_NOT_ASSOCIATED_WITH_ACADEMY,
            // "User not associated with academy");
            // }
            List<String> academyIds = traineeAcademyMappings.stream()
                    .map(traineeAcademyMapping -> traineeAcademyMapping.getAcademy().getId())
                    .collect(Collectors.toList());
            academyPosts = postRepository.finalByAcademyIdInAndVisibility(academyIds, Visibility.PRIVATE);
        } else if (userProfileDto.getUserType() == UserType.COACH) {
            List<CoachAcademyMapping> coachAcademyMapping = coachAcademyMappingRepo.findByCoachUserProfile_Id(userId);
            if (coachAcademyMapping.isEmpty()) {
                throw new ResourceException(ErrorCodes.USER_NOT_ASSOCIATED_WITH_ACADEMY,
                        "User not associated with academy");
            }
            List<String> academyIds = coachAcademyMapping.stream()
                    .map(coachAcademyMappingTmp -> coachAcademyMappingTmp.getAcademy().getId())
                    .collect(Collectors.toList());
            academyPosts = postRepository.finalByAcademyIdInAndVisibility(academyIds, Visibility.PRIVATE);
        }

        // Combine the public and academy-specific posts

        allPosts.addAll(publicPosts);
        allPosts.addAll(academyPosts);
        if (allPosts.isEmpty()) {
            return new ArrayList<>();
        }

        if (StringUtils.hasText(orgId)) {
            List<AcademyDto> academiesByOrg = academyService.getAcademiesByOrgId(orgId);
            Set<String> validAcademyIds = academiesByOrg.stream().map(AcademyDto::getId).collect(Collectors.toSet());

            allPosts = allPosts.stream().filter(post -> {
                String academyId = post.getAcademyId();

                return StringUtils.hasText(academyId) ? validAcademyIds.contains(academyId) : false;
            }).collect(Collectors.toList());
        } else if (StringUtils.hasText(packageName)) {
            List<AcademyDto> academiesByOrg = academyService.getAcademiesByAppPackageName(packageName);
            Set<String> validAcademyIds = academiesByOrg.stream().map(AcademyDto::getId).collect(Collectors.toSet());

            allPosts = allPosts.stream().filter(post -> {
                String academyId = post.getAcademyId();
                // exclude public posts not linked to the org
                return StringUtils.hasText(academyId) ? validAcademyIds.contains(academyId) : false;
            }).collect(Collectors.toList());
        }

        allPosts.sort((c1, c2) -> c2.getCreatedOn().compareTo(c1.getCreatedOn()));
        List<String> userIds = allPosts.stream().map(Post::getUserId)
                .filter(postUserId -> StringUtils.hasText(postUserId)).toList();
        List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(userIds);
        Map<String, UserProfileDto> userProfileDtoMap = userProfileDtos.stream()
                .collect(Collectors.toMap(UserProfileDto::getId, postUserProfileDto -> postUserProfileDto));
        List<String> postIds = allPosts.stream().map(Post::getId).toList();
        List<Comment> allPostComments = commentRepo.findByPostIdIn(postIds);
        Map<String, List<Comment>> commentMap = allPostComments.stream()
                .collect(Collectors.groupingBy(Comment::getPostId));

        return allPosts.stream().map(post -> {
            try {
                AcademyMinDto academyMinDto = null;
                if (StringUtils.hasText(post.getAcademyId())) {
                    academyMinDto = modelMapper.map(academyService.getAcademyById(post.getAcademyId()),
                            AcademyMinDto.class);
                }
                List<Comment> comments = commentMap.get(post.getId());
                PostDto postDto = new PostDto(post,
                        modelMapper.map(userProfileDtoMap.get(post.getUserId()), UserProfileMinDto.class),
                        academyMinDto, CollectionUtils.isEmpty(comments) ? 0 : comments.size(), userId);
                if (!CollectionUtils.isEmpty(post.getMediaUrls())) {
                    postDto.setMediaUrls(post.getMediaUrls().stream().map(mediaUrl -> postMediaBaseUrl + mediaUrl)
                            .collect(Collectors.toList()));
                }

                if (!CollectionUtils.isEmpty(post.getThumbnailUrls())) {
                    postDto.setThumbnailUrls(post.getThumbnailUrls().stream()
                            .map(thumbnailUrl -> postMediaBaseUrl + thumbnailUrl).collect(Collectors.toList()));
                }
                return postDto;
            } catch (ResourceException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

    }

    @Override
    public void deletePost(String postId, UserDetail userDetail) throws ResourceException {
        // Fetch the post by ID
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        // Ensure the user is authorized to delete the post
        if (!post.getUserId().equals(userDetail.getUserId())) {
            throw new ResourceException(ErrorCodes.UNAUTHORIZED_ACTION, "You are not allowed to delete this post");
        }

        // Perform the deletion
        postRepository.delete(post);
    }

    @Override
    public void likePost(String postId, String userId) {
        // Fetch the post by ID, throw exception if not found
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (CollectionUtils.isEmpty(post.getLikes())) {
            post.setLikes(new ArrayList<>());
        }

        // Add userId to likes if not already present
        if (!post.getLikes().contains(userId)) {
            post.getLikes().add(userId);

            // Remove userId from dislikes if present
            if (!CollectionUtils.isEmpty(post.getDislikes())) {
                post.getDislikes().remove(userId);
            }
        }

        // Save the updated post
        postRepository.save(post);
    }

    @Override
    public void dislikePost(String postId, String userId) {
        // Fetch the post by ID, throw exception if not found
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (CollectionUtils.isEmpty(post.getDislikes())) {
            post.setDislikes(new ArrayList<>());
        }

        // Add userId to dislikes if not already present
        if (!post.getDislikes().contains(userId)) {
            post.getDislikes().add(userId);

            // Remove userId from likes if present
            if (!CollectionUtils.isEmpty(post.getLikes())) {
                post.getLikes().remove(userId);
            }
        }

        // Save the updated post
        postRepository.save(post);
    }

    @Override
    public List<UserProfileMinDto> getAllLikedUserForPost(String userId, String postId) throws ResourceException {
        // Fetch the post by ID, throw exception if not found
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        // Add userId to likes if not already present
        if (CollectionUtils.isEmpty(post.getLikes())) {
            return List.of();
        }

        List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(post.getLikes());
        return userProfileDtos.stream().map(userProfileDto -> modelMapper.map(userProfileDto, UserProfileMinDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserProfileMinDto> getAllDisLikedUserForPost(String userId, String postId) throws ResourceException {
        // Fetch the post by ID, throw exception if not found
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        // Add userId to likes if not already present
        if (CollectionUtils.isEmpty(post.getDislikes())) {
            return List.of();
        }

        List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(post.getDislikes());
        return userProfileDtos.stream().map(userProfileDto -> modelMapper.map(userProfileDto, UserProfileMinDto.class))
                .collect(Collectors.toList());
    }

    @Async
    @Override
    public void reportPost(String reportedByUserId, String postId) {
        ReportedPost reportedPost = new ReportedPost();
        reportedPost.setPostId(postId);
        reportedPost.setReportedByUserProfile(UserProfile.builder().id(reportedByUserId).build());
        reportedPost.setStatus(ReportStatus.PENDING);
        reportedPost.setCreatedOn(Timestamp.from(Instant.now()));
        reportedPostRepo.save(reportedPost);
    }

    @Override
    public List<PostDto> getPostsForUserPaginated(String userId, String orgId, String packageName,
            Integer page, Integer size, String sortBy, String sortDir
    // , List<String> statusFilter
    )
            throws ResourceException {
        // Default values
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "createdOn";
        String direction = StringUtils.hasText(sortDir) ? sortDir : "desc";

        UserProfileDto userProfileDto = userProfileService.getUserProfileById(userId);

        // Get academy IDs for the user
        Set<String> academyIds = new HashSet<>();
        if (userProfileDto.getUserType() == UserType.PLAYER) {
            List<TraineeAcademyMapping> traineeAcademyMappings = traineeAcademyMappingRepo
                    .findByTraineeUserProfile_Id(userId);
            academyIds = traineeAcademyMappings.stream()
                    .map(traineeAcademyMapping -> traineeAcademyMapping.getAcademy().getId())
                    .collect(Collectors.toSet());
        } else if (userProfileDto.getUserType() == UserType.COACH) {
            List<CoachAcademyMapping> coachAcademyMappings = coachAcademyMappingRepo.findByCoachUserProfile_Id(userId);
            if (!coachAcademyMappings.isEmpty()) {
                academyIds = coachAcademyMappings.stream()
                        .map(coachAcademyMapping -> coachAcademyMapping.getAcademy().getId())
                        .collect(Collectors.toSet());
            }
        }
        List<Post> allPosts;

        // Apply organization/package filtering to academy IDs if needed
        if (StringUtils.hasText(orgId) || StringUtils.hasText(packageName)) {
            allPosts = getPostsByPackageName(orgId,
                    packageName,
                    page,
                    pageSize,
                    sortField,
                    direction
            // ,statusFilter
            );
        } else {
            // Single query to get all posts (public + user's academy private posts)
            allPosts = postRepository.findPostsForUserWithPagination(
                    new ArrayList<>(academyIds),
                    pageNum,
                    pageSize,
                    sortField,
                    direction
            // ,statusFilter
            );
        }

        // Batch fetch user profiles for all posts in one operation
        Map<String, UserProfileDto> userProfileDtoMap = batchFetchUserProfiles(allPosts);

        // Batch fetch all comment counts in one operation
        Map<String, Integer> commentCountMap = batchFetchCommentCounts(allPosts);

        // Batch fetch all academy info in one operation
        Map<String, AcademyMinDto> academyDtoMap = batchFetchAcademyInfo(allPosts);

        // Transform to DTOs with the pre-fetched data
        return allPosts.stream().map(post -> {
            try {
                AcademyMinDto academyMinDto = academyDtoMap.get(post.getAcademyId());
                Integer commentCount = commentCountMap.getOrDefault(post.getId(), 0);
                UserProfileMinDto postUserProfile = modelMapper.map(
                        userProfileDtoMap.getOrDefault(post.getUserId(), new UserProfileDto()),
                        UserProfileMinDto.class);

                PostDto postDto = new PostDto(post, postUserProfile, academyMinDto, commentCount, userId);
                addMediaUrls(post, postDto);

                return postDto;
            } catch (Exception e) {
                log.error("Error mapping post to DTO", e);
                return null;
            }
        })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Post> getPostsByPackageName(String orgId, String packageName,
            Integer page, Integer size, String sortBy, String direction
    // , List<String> statusFilter
    )
            throws ResourceException {
        Set<String> academyIds = new HashSet<>();
        if (StringUtils.hasText(orgId) || StringUtils.hasText(packageName)) {
            academyIds = getValidAcademyIds(orgId, packageName);
        }

        return postRepository.findPostsByAcademyIds(
                new ArrayList<>(academyIds),
                page,
                size,
                sortBy,
                direction
        // ,statusFilter
        );
    }

    private Set<String> getValidAcademyIds(String orgId, String packageName) throws ResourceException {
        Set<String> validAcademyIds = new HashSet<>();
        try {
            if (StringUtils.hasText(orgId)) {
                List<AcademyDto> academiesByOrg = academyService.getAcademiesByOrgId(orgId);
                validAcademyIds.addAll(academiesByOrg.stream()
                        .map(AcademyDto::getId)
                        .collect(Collectors.toSet()));
            } else if (StringUtils.hasText(packageName)) {
                List<AcademyDto> academiesByPackage = academyService.getAcademiesByAppPackageName(packageName);
                validAcademyIds.addAll(academiesByPackage.stream()
                        .map(AcademyDto::getId)
                        .collect(Collectors.toSet()));
            }
        } catch (Exception e) {
            log.error("Error fetching academy IDs", e);
        }
        return validAcademyIds;
    }

    private Map<String, UserProfileDto> batchFetchUserProfiles(List<Post> posts) {
        List<String> userIds = posts.stream()
                .map(Post::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            List<UserProfileDto> userProfiles = userProfileService.getUserProfileByIds(userIds);
            return userProfiles.stream()
                    .collect(Collectors.toMap(UserProfileDto::getId, userProfile -> userProfile,
                            (existing, replacement) -> existing));
        } catch (ResourceException e) {
            log.error("Error batch fetching user profiles", e);
            return new HashMap<>();
        }
    }

    private Map<String, Integer> batchFetchCommentCounts(List<Post> posts) {
        List<String> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        if (postIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Comment> allComments = commentRepo.findByPostIdIn(postIds);

        // Group by postId and count
        Map<String, Integer> commentCounts = new HashMap<>();
        for (Comment comment : allComments) {
            String postId = comment.getPostId();
            commentCounts.put(postId, commentCounts.getOrDefault(postId, 0) + 1);
        }

        return commentCounts;
    }

    private Map<String, AcademyMinDto> batchFetchAcademyInfo(List<Post> posts) {
        List<String> academyIds = posts.stream()
                .map(Post::getAcademyId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (academyIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, AcademyMinDto> result = new HashMap<>();
        for (String academyId : academyIds) {
            try {
                AcademyMinDto academyDto = modelMapper.map(
                        academyService.getAcademyById(academyId), AcademyMinDto.class);
                result.put(academyId, academyDto);
            } catch (ResourceException e) {
                log.warn("Failed to fetch academy with ID: {}", academyId, e);
            }
        }
        return result;
    }

    private void addMediaUrls(Post post, PostDto postDto) {
        if (!CollectionUtils.isEmpty(post.getMediaUrls())) {
            postDto.setMediaUrls(post.getMediaUrls().stream()
                    .map(mediaUrl -> postMediaBaseUrl + mediaUrl)
                    .collect(Collectors.toList()));
        }

        if (!CollectionUtils.isEmpty(post.getThumbnailUrls())) {
            postDto.setThumbnailUrls(post.getThumbnailUrls().stream()
                    .map(thumbnailUrl -> postMediaBaseUrl + thumbnailUrl)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public Pair<Integer, Long> getTotalPagesAndElements(String userId, String orgId, String packageName,
            Integer pageSize) {
        try {
            UserProfileDto userProfileDto = userProfileService.getUserProfileById(userId);

            Set<String> academyIds = new HashSet<>();
            long totalElements;

            if (userProfileDto.getUserType() == UserType.PLAYER) {
                List<TraineeAcademyMapping> traineeAcademyMappings = traineeAcademyMappingRepo
                        .findByTraineeUserProfile_Id(userId);
                academyIds = traineeAcademyMappings.stream()
                        .map(traineeAcademyMapping -> traineeAcademyMapping.getAcademy().getId())
                        .collect(Collectors.toSet());
            } else if (userProfileDto.getUserType() == UserType.COACH) {
                List<CoachAcademyMapping> coachAcademyMappings = coachAcademyMappingRepo
                        .findByCoachUserProfile_Id(userId);
                if (!coachAcademyMappings.isEmpty()) {
                    academyIds = coachAcademyMappings.stream()
                            .map(coachAcademyMapping -> coachAcademyMapping.getAcademy().getId())
                            .collect(Collectors.toSet());
                }
            }

            if (StringUtils.hasText(orgId) || StringUtils.hasText(packageName)) {
                Set<String> validAcademyIds = getValidAcademyIds(orgId, packageName);
                totalElements = postRepository.countPostsByAcademyIds(new ArrayList<>(validAcademyIds));
            } else {
                totalElements = postRepository.countPostsForUser(new ArrayList<>(academyIds));
            }

            // Default page size if not provided
            int size = (pageSize != null && pageSize > 0) ? pageSize : 10;

            // Calculated total pages
            int totalPages = (int) Math.ceil((double) totalElements / size);

            return Pair.of(totalPages, totalElements);
        } catch (ResourceException e) {
            log.warn("Something unexpected happened while getting total pages and elemets for timeline list: {}",
                    e.getMessage());
            log.warn("Sending totalPages and totalElements as 0");
            return Pair.of(0, 0l);
        }
    }
}
