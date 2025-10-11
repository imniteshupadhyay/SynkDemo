package com.playmotech.api.core.websockets;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.api.core.dao_postgres.BadmintonLiveScore;
import com.playmotech.api.core.dto.BadmintonScoreNotificationDto;
import com.playmotech.api.core.repo.BadmintonLiveScoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SubscribeMatchUpdatesWebSocketHandler extends AbstractWebSocketHandler {
	private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();
	private final BadmintonLiveScoreRepository badmintonLiveScoreRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws IOException {
		var principal = session.getPrincipal();
		if (principal == null || principal.getName() == null) {
			session.close(CloseStatus.SERVER_ERROR.withReason("User must be authenticated"));
			return;
		}

		String matchId = getMatchIdFromSession(session);
		if (matchId == null) {
			session.close(CloseStatus.BAD_DATA.withReason("MatchId is required"));
			return;
		}

		log.info("WebSocket connection established - session: {}, match: {}, user: {}",
				session.getId(), matchId, principal.getName());

		sessions.computeIfAbsent(matchId, k -> new ArrayList<>()).add(session);

		// Send match score history to the new connection
		sendMatchScoreHistory(session, matchId);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		String matchId = getMatchIdFromSession(session);
		if (matchId != null) {
			List<WebSocketSession> matchSessions = sessions.get(matchId);
			if (matchSessions != null) {
				matchSessions.remove(session);
				if (matchSessions.isEmpty()) {
					sessions.remove(matchId);
				}
			}
		}

		log.info("WebSocket connection closed - session: {}, status: {}", session.getId(), status);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		log.error("WebSocket transport error - session: {}", session.getId(), exception);
	}

	private String getMatchIdFromSession(WebSocketSession session) {
		try {
			URI uri = session.getUri();
			if (uri == null)
				return null;
			return getQueryParams(uri).get("matchId");
		} catch (Exception e) {
			log.error("Error getting matchId from session", e);
			return null;
		}
	}

	private Map<String, String> getQueryParams(URI uri) {
		Map<String, String> queryParams = new HashMap<>();
		String query = uri.getQuery();
		if (query != null) {
			for (String pair : query.split("&")) {
				String[] keyValue = pair.split("=");
				if (keyValue.length == 2) {
					queryParams.put(keyValue[0], keyValue[1]);
				}
			}
		}
		return queryParams;
	}

	public List<WebSocketSession> getSessionsForMatch(String matchId) {
		return new ArrayList<>(sessions.getOrDefault(matchId, new ArrayList<>()));
	}

	public void cleanupMatchHistory(String matchId) {
		if (matchId != null) {
			sessions.remove(matchId);
			log.info("Cleaned up WebSocket sessions for match: {}", matchId);
		}
	}

	private void sendMatchScoreHistory(WebSocketSession session, String matchId) {
    try {
        // Get match history from repository
        List<BadmintonLiveScore> scoreHistory = badmintonLiveScoreRepository.findByMatchIdOrderByLastUpdatedDesc(matchId);

        if (scoreHistory != null && !scoreHistory.isEmpty()) {
            for (BadmintonLiveScore liveScore : scoreHistory) {
                BadmintonScoreNotificationDto dto = new BadmintonScoreNotificationDto();
                dto.setMatchId(liveScore.getMatchId());
                dto.setPoints(liveScore.getScore() != null ? liveScore.getScore().intValue() : null);
                dto.setComment(liveScore.getCommentary());
                dto.setGuestPlayerName(liveScore.getGuestPlayerName());
                dto.setTeamId(liveScore.getTeamId());
                dto.setScoreTime(liveScore.getScoreTime());
                dto.setRequestId(liveScore.getRequestId());
                dto.setMetadata(liveScore.getMetadata());
                dto.setInitial(liveScore.getInitial() != null ? liveScore.getInitial() : Boolean.FALSE);

                String json = objectMapper.writeValueAsString(dto);
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }

            log.info("Sent {} individual match score messages for match: {}", scoreHistory.size(), matchId);
        } else {
            log.info("No history data found for match: {}", matchId);
        }
    } catch (IOException e) {
        log.error("Error sending match history to session: {}", session.getId(), e);
    } catch (Exception e) {
        log.error("Unexpected error retrieving match history for match: {}", matchId, e);
    }
}


	private void sendMatchScoreHistoryOld(WebSocketSession session, String matchId) {
		try {
			// Get match history from repository
			List<BadmintonLiveScore> scoreHistory = badmintonLiveScoreRepository
					.findByMatchIdOrderByLastUpdatedDesc(matchId);

			if (scoreHistory != null && !scoreHistory.isEmpty()) {
				// Convert BadmintonLiveScore entities to BadmintonScoreNotificationDto objects
				List<BadmintonScoreNotificationDto> notificationDtos = scoreHistory.stream()
						.map(liveScore -> {
							BadmintonScoreNotificationDto dto = new BadmintonScoreNotificationDto();
							dto.setMatchId(liveScore.getMatchId());
							dto.setPoints(liveScore.getScore() != null ? liveScore.getScore().intValue() : null); // Convert
																													// Long
																													// to
																													// Integer
							dto.setComment(liveScore.getCommentary()); // Map commentary to comment
							dto.setGuestPlayerName(liveScore.getGuestPlayerName());
							dto.setTeamId(liveScore.getTeamId());
							dto.setScoreTime(liveScore.getScoreTime());
							dto.setRequestId(liveScore.getRequestId());
							// Note: PlayerUserProfile info not included to avoid additional DB queries
							// Client should handle displaying player info based on playerId if needed
							return dto;
						})
						.collect(Collectors.toList());

				// Convert to JSON and send
				String historyJson = objectMapper.writeValueAsString(notificationDtos);
				if (session.isOpen()) {
					session.sendMessage(new TextMessage(historyJson));
					log.info("Sent match history data for match: {}, records: {}", matchId, notificationDtos.size());
				}
			} else {
				log.info("No history data found for match: {}", matchId);
			}
		} catch (IOException e) {
			log.error("Error sending match history to session: {}", session.getId(), e);
		} catch (Exception e) {
			log.error("Unexpected error retrieving match history for match: {}", matchId, e);
		}
	}
}