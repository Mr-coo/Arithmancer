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

	record GameState(List<PlayerState> players, List<EnemyState> enemies) implements ServerMessage {
	}

	record PlayerState(String nickname, double x, double y, int health, int maxHealth, boolean you) {
	}

	record EnemyState(int id, double x, double y) {
	}

}
