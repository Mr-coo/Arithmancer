package com.arithmancer.game;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.arithmancer.room.Room;

@Component
public class GameManager {

	private static final Logger log = LoggerFactory.getLogger(GameManager.class);

	private static final int TICKS_PER_SECOND = 20;

	private final List<Game> games = new CopyOnWriteArrayList<>();

	public Game start(Room room) {
		Game game = new Game(room.code(), room.players());
		games.add(game);
		log.info("Started game {} with {} players", game.getCode(), game.getPlayers().size());
		return game;
	}

	@Scheduled(fixedRate = 1000 / TICKS_PER_SECOND)
	void loop() {
		games.forEach(Game::tick);
	}

}
