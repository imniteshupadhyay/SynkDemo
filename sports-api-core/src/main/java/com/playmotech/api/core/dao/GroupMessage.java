package com.playmotech.api.core.dao;

import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
public class GroupMessage {
	private String id;
	private String groupId;
	private String academyId;
//    private List<Message>  messages;
	private long time;
	private String senderUserId;
	private String message;
	private String mediaUrl;
}
