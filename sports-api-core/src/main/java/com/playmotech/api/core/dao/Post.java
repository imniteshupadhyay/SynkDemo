package com.playmotech.api.core.dao;

import java.util.List;

import com.playmotech.api.core.constants.PostStatus;
import com.playmotech.api.core.constants.Visibility;

import lombok.Data;

@Data
public class Post {
	private String id;
	private String title;
	private String body;
	private List<String> mediaUrls;
	private Visibility visibility;
	private String academyId;
	private String userId;
	private String createdOn;
	private String updatedOn;
	private PostStatus status;
	private List<String> likes;
	private List<String> dislikes;
	private List<String> thumbnailUrls;
}
