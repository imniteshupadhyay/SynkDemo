package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dto.CommentDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface ICommentService {
	CommentDto addComment(String postId, String userId, String text) throws ResourceException;

	void deleteComment(String commentId, String userId) throws ResourceException;

	List<CommentDto> getCommentsByPostId(String postId, String userId) throws ResourceException;

	CommentDto likeComment(String commentId, String userId) throws ResourceException;

	CommentDto dislikeComment(String commentId, String userId) throws ResourceException;

	List<UserProfileMinDto> getAllLikedUserForComment(String userId, String commentId) throws ResourceException;

	List<UserProfileMinDto> getAllDisLikedUserForComment(String userId, String commentId) throws ResourceException;
}
