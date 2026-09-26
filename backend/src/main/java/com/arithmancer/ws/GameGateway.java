package com.arithmancer.ws;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameGateway extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(GameGateway.class);

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		log.info("Connected: {}", session.getId());
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		String payload = message.getPayload();
		if (payload.codePointCount(0, payload.length()) != 1) {
			session.close(CloseStatus.BAD_DATA.withReason("Expected one character"));
			return;
		}
		session.sendMessage(message);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		log.info("Disconnected: {} ({})", session.getId(), status);
	}

}
