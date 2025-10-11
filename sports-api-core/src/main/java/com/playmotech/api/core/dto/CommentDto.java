package com.playmotech.api.core.dto;

import org.springframework.util.CollectionUtils;

import com.playmotech.api.core.constants.CommentUserAction;
import com.playmotech.api.core.dao.Comment;

import lombok.Data;

@Data
public class CommentDto {
	private String id;
	private String postId;
	private String userId;
	private String text;
	private String createdOn;
	private int numberOfLikes;
	private int numberOfDislikes;
	private UserProfileMinDto userProfile;
	private CommentUserAction commentUserAction;

	// Constructor from Comment entity
	public CommentDto(Comment comment, UserProfileMinDto userProfile) {
		this.id = comment.getId();
		this.postId = comment.getPostId();
		this.userId = comment.getUserId();
		this.text = comment.getText();
		this.createdOn = comment.getCreatedOn();
		this.numberOfLikes = CollectionUtils.isEmpty(comment.getLikes()) ? 0 : comment.getLikes().size();
		this.numberOfDislikes = CollectionUtils.isEmpty(comment.getDislikes()) ? 0 : comment.getDislikes().size();
		this.userProfile = userProfile;
		this.commentUserAction = this.getCommentUserActionByUser(comment, userProfile.getId());
	}

	private CommentUserAction getCommentUserActionByUser(Comment comment, String userId) {
		if (comment.getLikes() != null && comment.getLikes().contains(userId)) {
			return CommentUserAction.LIKE;
		} else if (comment.getLikes() != null && comment.getDislikes().contains(userId)) {
			return CommentUserAction.DISLIKE;
		} else {
			return CommentUserAction.NONE;
		}
	}
}
