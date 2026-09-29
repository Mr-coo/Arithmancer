package com.arithmancer.ws;

import java.util.List;

public sealed interface ServerMessage {

	record RoomCreated(String code, List<String> players) implements ServerMessage {
	}

	record RoomJoined(String code, List<String> players) implements ServerMessage {
	}

	record Input(String key, KeyAction action) implements ServerMessage {
	}

	record GameStarted(String code, List<String> players, List<DecorationState> decorations,
			List<DetailState> details) implements ServerMessage {
	}

	// Solid circle at (x, y) with radius; the cover circle marks where a character is behind it.
	record DecorationState(String type, double x, double y, double radius, double coverX, double coverY,
			double coverRadius) {
	}

	// Drawn on the ground at (x, y); characters walk over it.
	record DetailState(String type, double x, double y) {
	}

	// time: seconds since the run started.
	record GameState(double time, List<PlayerState> players, List<EnemyState> enemies, List<ShotState> shots)
			implements ServerMessage {
	}

	// cooldown: seconds until the player can shoot again, out of maxCooldown. score: enemies killed.
	record PlayerState(String nickname, double x, double y, int health, int maxHealth, double cooldown,
			double maxCooldown, int score, boolean you) {
	}

	record EnemyState(int id, double x, double y, String question) {
	}

	// player: index in players, enemy: the id of the enemy hit.
	record ShotState(int player, int enemy) {
	}

	// time: seconds the team survived.
	record GameOver(double time, List<FinalScore> players) implements ServerMessage {
	}

	record FinalScore(String nickname, int score, boolean you) {
	}

}
