package com.arithmancer.game;

import java.util.List;

import com.arithmancer.entity.Position;
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

	public void tick(double deltaSeconds) {
		// One simulation step: movement, spawning, questions, damage and revives go here.
		players.forEach(player -> move(player, deltaSeconds));
	}

	// y grows downward, as on screen.
	private static void move(Player player, double deltaSeconds) {
		int dx = held(player, "d") - held(player, "a");
		int dy = held(player, "s") - held(player, "w");
		if (dx == 0 && dy == 0) {
			return;
		}
		// Divide by the direction's length so diagonal moves are not faster.
		double step = player.getSpeed() * deltaSeconds / Math.hypot(dx, dy);
		Position position = player.getPosition();
		player.setPosition(new Position(position.x() + dx * step, position.y() + dy * step));	}

	private static int held(Player player, String key) {
		return player.isHeld(key) ? 1 : 0;
	}

}
