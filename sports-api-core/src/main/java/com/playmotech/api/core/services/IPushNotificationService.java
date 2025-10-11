package com.playmotech.api.core.services;

import java.util.List;
import java.util.Map;

import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.dto.NotificationDto;
import com.playmotech.api.core.dto.TestNotificationDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.exceptions.ResourceException;

/**
 * Created By: deep.patel
 **/
public interface IPushNotificationService {
	boolean subscribeToTopic(String pushToken, String topic);

	boolean unsubscribeToTopic(String pushToken, String topic);

	boolean sendMessageToTopic(String topic, NotificationType type, String title, String body, String cta,
			CtaType ctaType, Map<String, String> extraParams);

	void sendMessageToPushToken(String pushToken, NotificationType type, String title, String body, String cta,
			CtaType ctaType, Map<String, String> extraParams);

	void testNotification(UserProfileDto userProfileDto, TestNotificationDto notificationDto) throws ResourceException;

	void addNotification(List<String> userIds, String notification, CtaType ctaType, String cta,
			Map<String, String> extraArgs);

	List<NotificationDto> getNotifications(String userId);
}
