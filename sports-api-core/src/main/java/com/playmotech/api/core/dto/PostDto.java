package com.playmotech.api.core.dto;

import java.util.List;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.PostUserAction;
import com.playmotech.api.core.constants.Visibility;
import com.playmotech.api.core.dao.Post;

import lombok.Data;

@Data
public class PostDto {
	private String id;
	private String title;
	private String body;
	private List<String> mediaUrls;
	private Visibility visibility;
	private String createdOn;
	private String updatedOn;
	private int numberOfLikes;
	private int numberOfDislikes;
	private UserProfileMinDto userProfile;
	private AcademyMinDto academy;
	private int numberOfComments;
	private PostUserAction postUserAction;
	private List<String> thumbnailUrls;
	private List<String> likes;

	public PostDto(Post post, UserProfileMinDto userProfile, AcademyMinDto academy, int numberOfComments, String loggedInUserId) {
		this.id = post.getId();
		this.title = post.getTitle();
		this.body = post.getBody();
		this.mediaUrls = post.getMediaUrls();
		this.visibility = post.getVisibility();
		this.createdOn = post.getCreatedOn();
		this.updatedOn = post.getUpdatedOn();
		this.numberOfLikes = CollectionUtils.isEmpty(post.getLikes()) ? 0 : post.getLikes().size();
		this.numberOfDislikes = CollectionUtils.isEmpty(post.getDislikes()) ? 0 : post.getDislikes().size();
		this.userProfile = userProfile;
		this.academy = academy;
		this.numberOfComments = numberOfComments;
		this.postUserAction = this.getPostUserActionByUser(post, userProfile.getId(), loggedInUserId);
		this.thumbnailUrls = post.getThumbnailUrls();
	}
	
	// Keep the old constructor for backward compatibility
//		public PostDto(Post post, UserProfileMinDto userProfile, AcademyMinDto academy, int numberOfComments) {
//			this(post, userProfile, academy, numberOfComments, null);
//		}

	private PostUserAction getPostUserActionByUser(Post post, String userId, String loggedInUserId) {
		if(!StringUtils.hasText(loggedInUserId)) {
			return PostUserAction.NONE;
		}
		if (post.getLikes() != null && post.getLikes().contains(loggedInUserId)) {
			return PostUserAction.LIKE;
		} else if (post.getDislikes() != null && post.getDislikes().contains(loggedInUserId)) {
			return PostUserAction.DISLIKE;
		} else {
			return PostUserAction.NONE;
		}
	}
}
