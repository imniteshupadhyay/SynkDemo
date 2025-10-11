package com.playmotech.api.core.services.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao.Comment;
import com.playmotech.api.core.dto.CommentDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.dynamorepo.CommentRepo;
import com.playmotech.api.core.dynamorepo.PostRepo;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.ICommentService;
import com.playmotech.api.core.services.IPostService;
import com.playmotech.api.core.services.IUserProfileService;

@Service
public class CommentService implements ICommentService {

	@Autowired
	private CommentRepo commentRepository;

	@Autowired
	private IPostService postService;

	@Autowired
	private IUserProfileService userProfileService;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private PostRepo postRepo;

	@Override
	public CommentDto addComment(String postId, String userId, String text) throws ResourceException {
		try {
			if (!isUserHasPermission(userId, postId)) {
				throw new ResourceException(ErrorCodes.UNAUTHORIZED,
						"You do not have permission to comment on this post");
			}
			UserProfileDto userProfileDto = userProfileService.getUserProfileById(userId);
			Comment comment = new Comment();
			comment.setId(UUID.randomUUID().toString());
			comment.setPostId(postId);
			comment.setUserId(userId);
			comment.setText(text);
			comment.setCreatedOn(Instant.now().toString());
			comment.setLikes(new ArrayList<>());
			comment.setDislikes(new ArrayList<>());

			Comment savedComment = commentRepository.save(comment);

			return new CommentDto(savedComment, modelMapper.map(userProfileDto, UserProfileMinDto.class));
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to add comment");
		}
	}

	@Override
	public void deleteComment(String commentId, String userId) throws ResourceException {
		try {
			Comment comment = commentRepository.findById(commentId).orElseThrow(
					() -> new ResourceException(ErrorCodes.NOT_FOUND, "Comment not found with id: " + commentId));
			if (!comment.getUserId().equalsIgnoreCase(userId)) {
				throw new ResourceException(ErrorCodes.UNAUTHORIZED,
						"You do not have permission to delete this comment");
			}

			commentRepository.delete(comment);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to delete comment");
		}
	}

	@Override
	public List<CommentDto> getCommentsByPostId(String postId, String currentUserId) throws ResourceException {
		try {
			// Check if the user has access to the post
			if (!isUserHasPermission(currentUserId, postId)) {
				throw new ResourceException(ErrorCodes.UNAUTHORIZED,
						"You do not have permission to access comments on this post");
			}

			List<Comment> comments = new ArrayList<>(commentRepository.findByPostId(postId));

			if (comments.isEmpty()) {

				return new ArrayList<>();
			}

			comments.sort((c1, c2) -> c2.getCreatedOn().compareTo(c1.getCreatedOn()));

			List<String> userIds = comments.stream().map(Comment::getUserId)
					.filter(commentUserId -> !StringUtils.isEmpty(commentUserId)).toList();
			List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(userIds);
			Map<String, UserProfileDto> userProfileDtoMap = userProfileDtos.stream()
					.collect(Collectors.toMap(UserProfileDto::getId, commentUserProfileDto -> commentUserProfileDto));
			// Map each comment to CommentDto using the retrieved user profiles
			return comments.stream().map(comment -> {
				return new CommentDto(comment,
						modelMapper.map(userProfileDtoMap.get(comment.getUserId()), UserProfileMinDto.class));
			}).collect(Collectors.toList());
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to get comments for post");
		}
	}

	@Override
	public CommentDto likeComment(String commentId, String userId) throws ResourceException {
		try {
			Comment comment = commentRepository.findById(commentId).orElseThrow(
					() -> new ResourceException(ErrorCodes.NOT_FOUND, "Comment not found with id: " + commentId));

			if (!comment.getLikes().contains(userId)) {
				comment.getLikes().add(userId); // Add the userId to the likes list
				comment.getDislikes().remove(userId); // Remove the userId from the dislikes list, if present
			}

			Comment updatedComment = commentRepository.save(comment);
			UserProfileDto userProfileDto = userProfileService.getUserProfileById(comment.getUserId());

			return new CommentDto(updatedComment, modelMapper.map(userProfileDto, UserProfileMinDto.class));
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to like comment");
		}
	}

	@Override
	public CommentDto dislikeComment(String commentId, String userId) throws ResourceException {
		try {
			Comment comment = commentRepository.findById(commentId).orElseThrow(
					() -> new ResourceException(ErrorCodes.NOT_FOUND, "Comment not found with id: " + commentId));

			if (!comment.getDislikes().contains(userId)) {
				comment.getDislikes().add(userId); // Add the userId to the dislikes list
				comment.getLikes().remove(userId); // Remove the userId from the likes list, if present
			}

			Comment updatedComment = commentRepository.save(comment);
			UserProfileDto userProfileDto = userProfileService.getUserProfileById(comment.getUserId());

			return new CommentDto(updatedComment, modelMapper.map(userProfileDto, UserProfileMinDto.class));
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE, "Failed to dislike comment");
		}
	}

	@Override
	public List<UserProfileMinDto> getAllLikedUserForComment(String userId, String commentId) throws ResourceException {
		Comment comment = commentRepository.findById(commentId).orElseThrow(
				() -> new ResourceException(ErrorCodes.NOT_FOUND, "Comment not found with id: " + commentId));
		if (comment.getLikes().isEmpty()) {
			return List.of();
		}
		List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(comment.getLikes());
		return userProfileDtos.stream().map(userProfileDto -> modelMapper.map(userProfileDto, UserProfileMinDto.class))
				.collect(Collectors.toList());
	}

	@Override
	public List<UserProfileMinDto> getAllDisLikedUserForComment(String userId, String commentId)
			throws ResourceException {
		Comment comment = commentRepository.findById(commentId).orElseThrow(
				() -> new ResourceException(ErrorCodes.NOT_FOUND, "Comment not found with id: " + commentId));
		if (comment.getDislikes().isEmpty()) {
			return List.of();
		}
		List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(comment.getDislikes());
		return userProfileDtos.stream().map(userProfileDto -> modelMapper.map(userProfileDto, UserProfileMinDto.class))
				.collect(Collectors.toList());
	}

	private boolean isUserHasPermission(String userId, String postId) throws ResourceException {
		return postService.getAllPostsForUser(userId, null, null).stream()
				.anyMatch(post -> post.getId().equals(postId));
	}
}
