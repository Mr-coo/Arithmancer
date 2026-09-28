package com.arithmancer.room;

import java.util.List;

public record Room(String code, List<Player> players) {

	public static final int MAX_PLAYERS = 4;

	public synchronized boolean join(Player player) {
		if (players.size() >= MAX_PLAYERS) {
			return false;
		}
		players.add(player);
		return true;
	}

}
