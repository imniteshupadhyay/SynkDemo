package com.playmotech.api.core.services.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.playmotech.api.core.websockets.SubscribeMatchUpdatesWebSocketHandler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class WebSocketBrokerService {
    private final SubscribeMatchUpdatesWebSocketHandler webSocketHandler;

    public void sendMessageToMatch(String matchId, String message) {
        List<WebSocketSession> matchSessions = webSocketHandler.getSessionsForMatch(matchId);

        if (matchSessions != null && !matchSessions.isEmpty()) {
            log.debug("Sending message to {} clients for match {}", matchSessions.size(), matchId);
            for (WebSocketSession session : matchSessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    } else {
                        log.debug("Session for match {} is closed, skipping", matchId);
                    }
                } catch (Exception e) {
                    log.error("Error sending Websocket message to session for match {}: {}", matchId, e);
                }
            }
            log.info("Sent message to {} clients for match {}", matchSessions.size(), matchId);
        } else {
            log.debug("No active WebSocket sessions for match {}", matchId);
        }
    }
}
