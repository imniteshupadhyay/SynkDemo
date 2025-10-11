package com.playmotech.api.core.dao;

import java.util.List;

import lombok.Data;

@Data
public class Comment {
	private String id;
	private String postId;
	private String userId;
	private String text;
	private String createdOn;
	private List<String> likes;
	private List<String> dislikes;
}
