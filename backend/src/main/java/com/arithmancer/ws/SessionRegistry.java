package com.arithmancer.ws;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import tools.jackson.databind.json.JsonMapper;

@Component
public class SessionRegistry {

	private static final Logger log = LoggerFactory.getLogger(SessionRegistry.class);

	private static final int SEND_TIME_LIMIT_MS = 1000;
	private static final int BUFFER_SIZE_LIMIT = 64 * 1024;

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
	private final JsonMapper jsonMapper;

	public SessionRegistry(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public void add(WebSocketSession session) {
		// The game loop and message handlers can send to the same session at once, which a plain session does not allow.
		sessions.put(session.getId(),
				new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT));
	}

	public void remove(String sessionId) {
		sessions.remove(sessionId);
	}

	public void send(String sessionId, String type, ServerMessage content) {
		WebSocketSession session = sessions.get(sessionId);
		if (session == null) {
			return;
		}
		try {
			Event event = new Event(type, jsonMapper.valueToTree(content));
			session.sendMessage(new TextMessage(jsonMapper.writeValueAsString(event)));
		} catch (IOException | RuntimeException e) {
			log.warn("Could not send {} to {}: {}", type, sessionId, e.getMessage());
		}
	}

}
