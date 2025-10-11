package com.playmotech.api.core.websockets;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.playmotech.api.core.constants.AppConstants;
import com.playmotech.api.core.constants.MatchWSMessageType;
import com.playmotech.api.core.dto.BadmintonScoreNotificationDto;
import com.playmotech.api.core.services.impl.WebSocketBrokerService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class ScoreUpdateMessageListener implements MessageListener {
    private final WebSocketBrokerService webSocketBrokerService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            log.info("Received Redis message");
            String channel = new String(message.getChannel());
            String payload = new String(message.getBody());

            if (channel.endsWith(".updates")) {
                BadmintonScoreNotificationDto scoreUpdate = AppConstants.GSON.fromJson(payload,
                        BadmintonScoreNotificationDto.class);
                scoreUpdate.setType(MatchWSMessageType.LIVE_SCORING);
                // Forward to WebSocket broker
                webSocketBrokerService.sendMessageToMatch(scoreUpdate.getMatchId(), payload);
            }
        } catch (Exception e) {
            log.error("Error processing Redis message", e);
        }
    }
}
