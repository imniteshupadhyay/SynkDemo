package com.playmotech.api.core.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dto.CommentDto;
import com.playmotech.api.core.dto.CommentRequestDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.ICommentService;

@RestController
@RequestMapping("/posts")
public class CommentController {

	@Autowired
	private ICommentService commentService;

	// Add a comment to a post
	@PostMapping("/{postId}/comments")
	public ResponseEntity<Response<CommentDto>> addComment(@PathVariable("postId") String postId,
			@RequestBody CommentRequestDto commentRequest) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		try {
			CommentDto createdComment = commentService.addComment(postId, currentUser.getUserId(),
					commentRequest.getText());

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Response.<CommentDto>builder().status(HttpStatus.CREATED.value())
							.message("Comment added successfully").body(createdComment).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CommentDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	// Get all comments for a post
	@GetMapping("/{postId}/comments")
	public ResponseEntity<Response<List<CommentDto>>> getCommentsByPost(@PathVariable("postId") String postId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			List<CommentDto> comments = commentService.getCommentsByPostId(postId, currentUser.getUserId());

			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<CommentDto>>builder()
					.status(HttpStatus.OK.value()).message("Comments retrieved successfully").body(comments).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<CommentDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	// Delete a comment
	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<Response<Void>> deleteComment(@PathVariable("commentId") String commentId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		try {
			commentService.deleteComment(commentId, currentUser.getUserId());

			return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Response.<Void>builder()
					.status(HttpStatus.NO_CONTENT.value()).message("Comment deleted successfully").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<Void>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	// Like a comment
	@PostMapping("/comments/{commentId}/like")
	public ResponseEntity<Response<CommentDto>> likeComment(@PathVariable("commentId") String commentId,
			@RequestBody String userId) {
		try {
			CommentDto commentDto = commentService.likeComment(commentId, userId);

			return ResponseEntity.status(HttpStatus.OK).body(Response.<CommentDto>builder()
					.status(HttpStatus.OK.value()).message("Comment liked successfully").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CommentDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	// Dislike a comment
	@PostMapping("/comments/{commentId}/dislike")
	public ResponseEntity<Response<CommentDto>> dislikeComment(@PathVariable("commentId") String commentId,
			@RequestBody String userId) {
		try {
			CommentDto commentDto = commentService.dislikeComment(commentId, userId);

			return ResponseEntity.status(HttpStatus.OK).body(Response.<CommentDto>builder()
					.status(HttpStatus.OK.value()).message("Comment disliked successfully").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<CommentDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/comments/{commentId}/likedusers", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<UserProfileMinDto>>> getAllLikedUserForComment(
			@PathVariable("commentId") String commentId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		try {
			List<UserProfileMinDto> likedUserForPost = commentService.getAllLikedUserForComment(currentUser.getUserId(),
					commentId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<UserProfileMinDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(likedUserForPost).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<UserProfileMinDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/comments/{commentId}/dislikedusers", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<UserProfileMinDto>>> getAllDisLikedUserForComment(
			@PathVariable("commentId") String commentId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		try {
			List<UserProfileMinDto> likedUserForPost = commentService
					.getAllDisLikedUserForComment(currentUser.getUserId(), commentId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<UserProfileMinDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(likedUserForPost).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<UserProfileMinDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}
}
