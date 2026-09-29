package com.arithmancer.game;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.arithmancer.room.Player;
import com.arithmancer.room.Room;
import com.arithmancer.ws.ServerMessage.EnemyState;
import com.arithmancer.ws.ServerMessage.GameState;
import com.arithmancer.ws.ServerMessage.PlayerState;
import com.arithmancer.ws.ServerMessage.ShotState;
import com.arithmancer.ws.SessionRegistry;

@Component
public class GameManager {

	private static final Logger log = LoggerFactory.getLogger(GameManager.class);

	private static final int TICKS_PER_SECOND = 20;

	private final List<Game> games = new CopyOnWriteArrayList<>();
	private final SessionRegistry sessionRegistry;

	public GameManager(SessionRegistry sessionRegistry) {
		this.sessionRegistry = sessionRegistry;
	}

	public Game start(Room room) {
		Game game = new Game(room.code(), room.players());
		games.add(game);
		log.info("Started game {} with {} players", game.getCode(), game.getPlayers().size());
		return game;
	}

	public Player findPlayer(String sessionId) {
		return games.stream()
				.flatMap(game -> game.getPlayers().stream())
				.filter(player -> player.getSessionId().equals(sessionId))
				.findFirst()
				.orElse(null);
	}

	@Scheduled(fixedRate = 1000 / TICKS_PER_SECOND)
	void loop() {
		for (Game game : games) {
			game.tick(1.0 / TICKS_PER_SECOND);
			sendState(game);
		}
	}

	private void sendState(Game game) {
		List<EnemyState> enemies = game.getEnemies().stream()
				.map(enemy -> new EnemyState(enemy.getId(), enemy.getPosition().x(), enemy.getPosition().y(),
						enemy.getQuestion().text()))
				.toList();
		List<ShotState> shots = game.getShots().stream()
				.map(shot -> new ShotState(game.getPlayers().indexOf(shot.player()), shot.enemy().getId()))
				.toList();
		for (Player recipient : game.getPlayers()) {
			List<PlayerState> players = game.getPlayers().stream()
					.map(player -> new PlayerState(player.getNickname(), player.getPosition().x(),
							player.getPosition().y(), player.getHealth(), player.getMaxHealth(), player.getShotCooldown(),
							player.getMaxShotCooldown(), player == recipient))
					.toList();
			sessionRegistry.send(recipient.getSessionId(), "state", new GameState(players, enemies, shots));
		}
	}

}
