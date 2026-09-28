package com.arithmancer.ws;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.arithmancer.room.Player;
import com.arithmancer.room.Room;
import com.arithmancer.room.RoomRegistry;
import com.arithmancer.ws.ClientMessage.CreateRoom;
import com.arithmancer.ws.ServerMessage.RoomCreated;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GameGateway extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(GameGateway.class);

	private final JsonMapper jsonMapper;
	private final RoomRegistry roomRegistry;

	public GameGateway(JsonMapper jsonMapper, RoomRegistry roomRegistry) {
		this.jsonMapper = jsonMapper;
		this.roomRegistry = roomRegistry;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		log.info("Connected: {}", session.getId());
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		ClientMessage clientMessage;
		try {
			clientMessage = jsonMapper.readValue(message.getPayload(), ClientMessage.class);
		} catch (JacksonException e) {
			clientMessage = null;
		}
		switch (clientMessage) {
			case CreateRoom createRoom -> createRoom(session, createRoom);
			case null -> session.close(CloseStatus.BAD_DATA.withReason("Invalid message"));
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		log.info("Disconnected: {} ({})", session.getId(), status);
	}

	private void createRoom(WebSocketSession session, CreateRoom createRoom) throws IOException {
		if (createRoom.nickname() == null || createRoom.nickname().isBlank()) {
			session.close(CloseStatus.BAD_DATA.withReason("Nickname required"));
			return;
		}
		Room room = roomRegistry.create(new Player(session.getId(), createRoom.nickname().strip()));
		send(session, new RoomCreated(room.code(), room.players().stream().map(Player::nickname).toList()));
	}

	private void send(WebSocketSession session, ServerMessage message) throws IOException {
		session.sendMessage(new TextMessage(jsonMapper.writerFor(ServerMessage.class).writeValueAsString(message)));
	}

}
