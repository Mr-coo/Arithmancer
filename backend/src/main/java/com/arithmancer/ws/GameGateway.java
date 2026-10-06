package com.arithmancer.ws;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.arithmancer.battle.Battle;
import com.arithmancer.battle.BattleManager;
import com.arithmancer.game.Game;
import com.arithmancer.game.GameManager;
import com.arithmancer.math.BattleTopic;
import com.arithmancer.math.SurvivalTopic;
import com.arithmancer.room.Player;
import com.arithmancer.room.Room;
import com.arithmancer.room.RoomRegistry;
import com.arithmancer.ws.ClientMessage.Answer;
import com.arithmancer.ws.ClientMessage.CreateRoom;
import com.arithmancer.ws.ClientMessage.Input;
import com.arithmancer.ws.ClientMessage.JoinRoom;
import com.arithmancer.ws.ClientMessage.StartGame;
import com.arithmancer.ws.ServerMessage.BattleStarted;
import com.arithmancer.ws.ServerMessage.DecorationState;
import com.arithmancer.ws.ServerMessage.DetailState;
import com.arithmancer.ws.ServerMessage.GameStarted;
import com.arithmancer.ws.ServerMessage.RoomCreated;
import com.arithmancer.ws.ServerMessage.RoomJoined;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GameGateway extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(GameGateway.class);

	// Short enough to fit over a character's head and in the score lists. The frontend's input stops there too.
	private static final int MAX_NICKNAME_LENGTH = 16;

	private final JsonMapper jsonMapper;
	private final RoomRegistry roomRegistry;
	private final GameManager gameManager;
	private final BattleManager battleManager;
	private final SessionRegistry sessionRegistry;

	public GameGateway(JsonMapper jsonMapper, RoomRegistry roomRegistry, GameManager gameManager,
			BattleManager battleManager, SessionRegistry sessionRegistry) {
		this.jsonMapper = jsonMapper;
		this.roomRegistry = roomRegistry;
		this.gameManager = gameManager;
		this.battleManager = battleManager;
		this.sessionRegistry = sessionRegistry;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		sessionRegistry.add(session);
		log.info("Connected: {}", session.getId());
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		switch (parse(message.getPayload())) {
			case CreateRoom createRoom -> createRoom(session, createRoom);
			case JoinRoom joinRoom -> joinRoom(session, joinRoom);
			case Input input -> input(session, input);
			case StartGame startGame -> startGame(session, startGame);
			case Answer answer -> answer(session, answer);
			case null -> session.close(CloseStatus.BAD_DATA.withReason("Invalid message"));
		}
	}

	private ClientMessage parse(String payload) {
		try {
			Event event = jsonMapper.readValue(payload, Event.class);
			if (event == null || event.content() == null) {
				return null;
			}
			return switch (event.type()) {
				case "createRoom" -> jsonMapper.treeToValue(event.content(), CreateRoom.class);
				case "joinRoom" -> jsonMapper.treeToValue(event.content(), JoinRoom.class);
				case "input" -> jsonMapper.treeToValue(event.content(), Input.class);
				case "startGame" -> jsonMapper.treeToValue(event.content(), StartGame.class);
				case "answer" -> jsonMapper.treeToValue(event.content(), Answer.class);
				case null, default -> null;
			};
		} catch (JacksonException e) {
			return null;
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		sessionRegistry.remove(session.getId());
		Room room = roomRegistry.leave(session.getId());
		if (room != null) {
			sendRoom(room);
		}
		Player player = findInRun(session.getId());
		if (player != null) {
			player.leave();
		}
		log.info("Disconnected: {} ({})", session.getId(), status);
	}

	private void createRoom(WebSocketSession session, CreateRoom createRoom) throws IOException {
		if (isInRoom(session.getId())) {
			session.close(CloseStatus.BAD_DATA.withReason("Already in a room"));
			return;
		}
		if (isBlank(createRoom.nickname())) {
			session.close(CloseStatus.BAD_DATA.withReason("Nickname required"));
			return;
		}
		if (isTooLong(createRoom.nickname())) {
			session.close(CloseStatus.BAD_DATA.withReason("Nickname too long"));
			return;
		}
		Room room = roomRegistry.create(new Player(session.getId(), createRoom.nickname().strip()));
		send(session, "roomCreated", new RoomCreated(room.code(), nicknames(room.players())));
	}

	private void joinRoom(WebSocketSession session, JoinRoom joinRoom) throws IOException {
		if (isInRoom(session.getId())) {
			session.close(CloseStatus.BAD_DATA.withReason("Already in a room"));
			return;
		}
		if (isBlank(joinRoom.nickname())) {
			session.close(CloseStatus.BAD_DATA.withReason("Nickname required"));
			return;
		}
		if (isTooLong(joinRoom.nickname())) {
			session.close(CloseStatus.BAD_DATA.withReason("Nickname too long"));
			return;
		}
		Room room = roomRegistry.find(joinRoom.code());
		if (room == null) {
			session.close(CloseStatus.BAD_DATA.withReason("Room not found"));
			return;
		}
		if (!room.join(new Player(session.getId(), joinRoom.nickname().strip()))) {
			session.close(CloseStatus.BAD_DATA.withReason("Room is full"));
			return;
		}
		sendRoom(room);
	}

	// Everyone in the lobby gets its players, and whether they are the host: the first of them.
	private void sendRoom(Room room) {
		List<Player> players = List.copyOf(room.players());
		for (int i = 0; i < players.size(); i++) {
			sessionRegistry.send(players.get(i).getSessionId(), "roomJoined",
					new RoomJoined(room.code(), nicknames(players), i == 0));
		}
	}

	private void input(WebSocketSession session, Input input) throws IOException {
		String key = input.key();
		if (key == null || key.codePointCount(0, key.length()) != 1 || !Character.isLetter(key.codePointAt(0))) {
			session.close(CloseStatus.BAD_DATA.withReason("Expected one letter"));
			return;
		}
		if (input.action() == null) {
			session.close(CloseStatus.BAD_DATA.withReason("Expected down or up"));
			return;
		}
		Player player = gameManager.findPlayer(session.getId());
		if (player != null) {
			switch (input.action()) {
				case DOWN -> player.press(key.toLowerCase(Locale.ROOT));
				case UP -> player.release(key.toLowerCase(Locale.ROOT));
			}
		}
	}

	private void startGame(WebSocketSession session, StartGame startGame) throws IOException {
		// A second click on start can arrive once the run has started: ignore it.
		if (findInRun(session.getId()) != null) {
			return;
		}
		Room room = roomRegistry.findBySession(session.getId());
		if (room == null) {
			session.close(CloseStatus.BAD_DATA.withReason("Not in a room"));
			return;
		}
		if (!room.players().getFirst().getSessionId().equals(session.getId())) {
			session.close(CloseStatus.BAD_DATA.withReason("Only the host can start"));
			return;
		}
		boolean turnBased = startGame.mode() == Mode.TURN_BASED;
		SurvivalTopic survivalTopic = topic(startGame.topic(), SurvivalTopic.class);
		BattleTopic battleTopic = topic(startGame.topic(), BattleTopic.class);
		if (turnBased ? battleTopic == null : survivalTopic == null) {
			session.close(CloseStatus.BAD_DATA.withReason("Topic not in this mode"));
			return;
		}
		if (!roomRegistry.remove(room)) {
			session.close(CloseStatus.BAD_DATA.withReason("Not in a room"));
			return;
		}
		if (turnBased) {
			Battle battle = battleManager.start(room, battleTopic);
			sendAll(battle.getPlayers(), "battleStarted",
					new BattleStarted(battle.getCode(), nicknames(battle.getPlayers())));
			return;
		}
		Game game = gameManager.start(room, survivalTopic);
		List<DecorationState> decorations = game.getDecorations().stream()
				.map(decoration -> new DecorationState(decoration.type(), decoration.solid().center().x(),
						decoration.solid().center().y(), decoration.solid().radius(), decoration.cover().center().x(),
						decoration.cover().center().y(), decoration.cover().radius()))
				.toList();
		List<DetailState> details = game.getDetails().stream()
				.map(detail -> new DetailState(detail.type(), detail.position().x(), detail.position().y()))
				.toList();
		sendAll(game.getPlayers(), "gameStarted",
				new GameStarted(game.getCode(), nicknames(game.getPlayers()), decorations, details));
	}

	private void answer(WebSocketSession session, Answer answer) throws IOException {
		if (answer.value() == null || answer.value() < 0) {
			session.close(CloseStatus.BAD_DATA.withReason("Expected a whole number of at least 0"));
			return;
		}
		Player player = findInRun(session.getId());
		if (player != null) {
			player.submitAnswer(answer.value());
		}
	}

	// The topic of this type that the name is written as, the first of them when there is no name, or null when none
	// is.
	private <T extends Enum<T>> T topic(String name, Class<T> type) {
		if (name == null) {
			return type.getEnumConstants()[0];
		}
		try {
			return jsonMapper.convertValue(name, type);
		} catch (JacksonException | IllegalArgumentException e) {
			return null;
		}
	}

	// A connection is in one room at a time, from its lobby to the end of its run.
	private boolean isInRoom(String sessionId) {
		return roomRegistry.findBySession(sessionId) != null || findInRun(sessionId) != null;
	}

	// The session's player in a real-time game or a battle, or null when it is in neither.
	private Player findInRun(String sessionId) {
		Player player = gameManager.findPlayer(sessionId);
		return player != null ? player : battleManager.findPlayer(sessionId);
	}

	private static boolean isBlank(String nickname) {
		return nickname == null || nickname.isBlank();
	}

	private static boolean isTooLong(String nickname) {
		String stripped = nickname.strip();
		return stripped.codePointCount(0, stripped.length()) > MAX_NICKNAME_LENGTH;
	}

	private static List<String> nicknames(List<Player> players) {
		return players.stream().map(Player::getNickname).toList();
	}

	private void send(WebSocketSession session, String type, ServerMessage content) {
		sessionRegistry.send(session.getId(), type, content);
	}

	private void sendAll(List<Player> players, String type, ServerMessage content) {
		players.forEach(player -> sessionRegistry.send(player.getSessionId(), type, content));
	}

}
