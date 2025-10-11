package com.playmotech.api.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.playmotech.api.core.repo.BadmintonLiveScoreRepository;
import com.playmotech.api.core.websockets.SubscribeMatchUpdatesWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final BadmintonLiveScoreRepository badmintonLiveScoreRepository;

    @Bean
    public SubscribeMatchUpdatesWebSocketHandler matchUpdatesWebSocketHandler() {
        return new SubscribeMatchUpdatesWebSocketHandler(badmintonLiveScoreRepository);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(matchUpdatesWebSocketHandler(), "/ws/match/subscribe", "/ws/**")
                .setAllowedOrigins("*");
    }
}