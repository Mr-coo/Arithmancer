package com.arithmancer.game;

import java.util.List;

import com.arithmancer.room.Player;

public class Game {

	private final String code;
	private final List<Player> players;

	public Game(String code, List<Player> players) {
		this.code = code;
		this.players = List.copyOf(players);
	}

	public String getCode() {
		return code;
	}

	public List<Player> getPlayers() {
		return players;
	}

	public void tick() {
		// One simulation step: movement, spawning, questions, damage and revives go here.
	}

}
