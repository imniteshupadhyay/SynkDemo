package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.NotificationType;

import lombok.Data;

@Data
public class PushMessage {
	private NotificationType type;
	private NotificationDto notification;
	private NotificationDataDto data;
}
