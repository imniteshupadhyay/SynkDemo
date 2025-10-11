package com.playmotech.api.core.services.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.TopicManagementResponse;
import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.dao.Notification;
import com.playmotech.api.core.dao.UserNotification;
import com.playmotech.api.core.dto.NotificationDto;
import com.playmotech.api.core.dto.TestNotificationDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dynamorepo.UserNotificationsRepo;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IPushNotificationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PushNotificationService implements IPushNotificationService {

	// private final IUserProfileService userProfileService;
	private final UserNotificationsRepo userNotificationsRepo;
	private final ModelMapper modelMapper = new ModelMapper();

	public PushNotificationService(UserNotificationsRepo userNotificationsRepo) {
		this.userNotificationsRepo = userNotificationsRepo;
		this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
	}

	@Override
	public boolean subscribeToTopic(String pushToken, String topic) {
		if (!StringUtils.hasText(pushToken)) {
			log.error("Push token is empty");
			return false;
		}

		try {
			TopicManagementResponse response = FirebaseMessaging.getInstance()
					.subscribeToTopic(Collections.singletonList(pushToken), topic);
			log.info("[Subscribe] Success: {}, Failure: {}, Errors: {}", response.getSuccessCount(),
					response.getFailureCount(), response.getErrors());
			return response.getSuccessCount() == 1;
		} catch (FirebaseMessagingException e) {
			log.error("Error while subscribing to Topic: {} for Token: {}", topic, pushToken, e);
			return false;
		}
	}

	@Override
	public boolean unsubscribeToTopic(String pushToken, String topic) {
		if (!StringUtils.hasText(pushToken)) {
			log.error("Push token is empty");
			return false;
		}
		try {
			TopicManagementResponse response = FirebaseMessaging.getInstance()
					.unsubscribeFromTopic(Arrays.asList(pushToken), topic);
			log.info("[UnSubscribe] Success: {}, Failure: {}, Errors: {}", response.getSuccessCount(),
					response.getFailureCount(), response.getErrors());
			return response.getSuccessCount() == 1;
		} catch (FirebaseMessagingException e) {
			log.error("Error while un-subscribing from Topic: {} for Token: {}", topic, pushToken, e);
			return false;
		}
	}

	@Override
	public boolean sendMessageToTopic(String topic, NotificationType type, String title, String body, String cta,
			CtaType ctaType, Map<String, String> extraParams) {
		Message.Builder messageBuilder = Message.builder().putData("type", type.name()).putData("title", title)
				.putData("body", body).putData("ctaType", ctaType.name()).putData("cta", cta).setTopic(topic);

		if (!CollectionUtils.isEmpty(extraParams)) {
			extraParams.forEach(messageBuilder::putData);
		}

		String response;
		try {
			response = FirebaseMessaging.getInstance().send(messageBuilder.build());
		} catch (FirebaseMessagingException e) {
			log.error("Error while sending message to topic {}", topic, e);
			return false;
		}

		log.info("Successfully sent message: {}", response);
		return true;
	}

	@Async
	@Override
	public void sendMessageToPushToken(String pushToken, NotificationType type, String title, String body,
			String cta, CtaType ctaType, Map<String, String> extraParams) {
		if (!StringUtils.hasText(pushToken)) {
			log.error("Push token is empty");
			// return false;
		}
		Message.Builder messageBuilder = Message.builder().putData("type", type.name()).putData("title", title)
				.putData("body", body).putData("ctaType", ctaType.name()).putData("cta", cta).setToken(pushToken);

		if (!CollectionUtils.isEmpty(extraParams)) {
			extraParams.forEach(messageBuilder::putData);
		}

		String response = null;
		try {
			response = FirebaseMessaging.getInstance().send(messageBuilder.build());
		} catch (FirebaseMessagingException e) {
			log.error("Error while sending message to token {}", pushToken, e);
			// return false;
		}

		log.info("Successfully sent message: {}", response);
		// return true;
	}

	@Override
	public void testNotification(UserProfileDto userProfileDto, TestNotificationDto notificationDto)
			throws ResourceException {
		if (userProfileDto == null) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
		}

		if (!StringUtils.hasText(userProfileDto.getAndroidFcmPushToken())) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Push token not found.");
		}

		Map<String, String> extraParam = new HashMap<>();
		extraParam.put("test", "true");
		log.info("Sending test notification to user: {}", userProfileDto.getDisplayName());
		sendMessageToPushToken(userProfileDto.getAndroidFcmPushToken(), NotificationType.TEST_NOTIFICATION,
				notificationDto.getTitle(), notificationDto.getBody(), notificationDto.getCta(),
				notificationDto.getCtaType(), notificationDto.getExtraParams());
		addNotification(userProfileDto.getId(), notificationDto.getBody(), notificationDto.getCtaType(),
				notificationDto.getCta(), notificationDto.getExtraParams());
	}

	@Async
	@Override
	public void addNotification(List<String> userId, String notification, CtaType ctaType, String cta,
			Map<String, String> extraArgs) {
		for (String id : userId) {
			addNotification(id, notification, ctaType, cta, extraArgs);
		}
	}

	private void addNotification(String userId, String notification, CtaType ctaType, String cta,
			Map<String, String> extraArgs) {
		Optional<UserNotification> userNotification = userNotificationsRepo.findById(userId);
		long currentTimeInMillis = System.currentTimeMillis();
		if (userNotification.isPresent()) {
			UserNotification userNotification1 = userNotification.get();
			userNotification1.getNotifications()
					.add(getNotification(currentTimeInMillis, notification, ctaType, cta, extraArgs));
			userNotificationsRepo.save(userNotification1);
		} else {
			UserNotification userNotification1 = new UserNotification();
			userNotification1.setId(userId);
			userNotification1.setNotifications(
					List.of(getNotification(currentTimeInMillis, notification, ctaType, cta, extraArgs)));
			userNotificationsRepo.save(userNotification1);
		}
	}

	@Override
	public List<NotificationDto> getNotifications(String userId) {
		Optional<UserNotification> userNotification = userNotificationsRepo.findById(userId);
		if (userNotification.isPresent()) {
			List<Notification> notifications = userNotification.get().getNotifications();
			notifications.sort(Comparator.comparingLong(Notification::getEpochTimeInMillis).reversed());
			return notifications.stream().map(notification -> {
				NotificationDto notificationDto = modelMapper.map(notification, NotificationDto.class);
				notificationDto
						.setNotificationTimeInUtc(Instant.ofEpochMilli(notification.getEpochTimeInMillis()).toString());
				return notificationDto;
			}).toList();
		}
		return List.of();
	}

	private Notification getNotification(long epochTimeInMillis, String notificationMessage, CtaType ctaType,
			String cta, Map<String, String> extraArgs) {
		Notification notification = new Notification();
		notification.setNotification(notificationMessage);
		notification.setCtaType(ctaType);
		notification.setCta(cta);
		notification.setExtraArgs(extraArgs);
		notification.setEpochTimeInMillis(epochTimeInMillis);

		return notification;
	}
}
