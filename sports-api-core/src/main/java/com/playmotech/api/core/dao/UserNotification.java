package com.playmotech.api.core.dao;

import java.util.List;

import lombok.Data;

@Data
public class UserNotification {
	private String id;
	private List<Notification> notifications;
}
