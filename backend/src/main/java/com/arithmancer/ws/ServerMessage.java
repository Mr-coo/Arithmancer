package com.arithmancer.ws;

import java.util.List;

public sealed interface ServerMessage {

	record RoomCreated(String code, List<String> players) implements ServerMessage {
	}

	record RoomJoined(String code, List<String> players) implements ServerMessage {
	}

	record Input(String key, KeyAction action) implements ServerMessage {
	}

	record GameStarted(String code, List<String> players) implements ServerMessage {
	}

	record GameState(List<PlayerState> players, List<EnemyState> enemies, List<ShotState> shots)
			implements ServerMessage {
	}

	// cooldown: seconds until the player can shoot again, out of maxCooldown.
	record PlayerState(String nickname, double x, double y, int health, int maxHealth, double cooldown,
			double maxCooldown, boolean you) {
	}

	record EnemyState(int id, double x, double y, String question) {
	}

	// player: index in players, enemy: the id of the enemy hit.
	record ShotState(int player, int enemy) {
	}

}
