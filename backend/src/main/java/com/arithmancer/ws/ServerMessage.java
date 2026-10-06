package com.arithmancer.ws;

import java.util.List;

public sealed interface ServerMessage {

	record RoomCreated(String code, List<String> players) implements ServerMessage {
	}

	// host: whether the receiving player is the host, the first of players.
	record RoomJoined(String code, List<String> players, boolean host) implements ServerMessage {
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

	// time: seconds since the run started. round: the current round, whose herd starts coming in roundStartsIn
	// seconds, 0 once it has.
	record GameState(double time, int round, double roundStartsIn, List<PlayerState> players, List<EnemyState> enemies,
			List<ItemState> items, List<ShotState> shots) implements ServerMessage {
	}

	// cooldown: seconds until the player can shoot again, out of maxCooldown. score: enemies killed. revive: while
	// downed, how far a teammate has got reviving them, from 0 to 1. haste, speedBoost: seconds left of those item
	// boosts.
	record PlayerState(String nickname, double x, double y, int health, int maxHealth, double cooldown,
			double maxCooldown, int score, double revive, double haste, double speedBoost, boolean you) {
	}

	// type: goblin or torch. health: answers still needed to kill it, out of maxHealth.
	record EnemyState(int id, String type, double x, double y, String question, int health, int maxHealth) {
	}

	// type: heal, haste or speed.
	record ItemState(int id, String type, double x, double y) {
	}

	// player: index in players, enemy: the id of the enemy hit.
	record ShotState(int player, int enemy) {
	}

	// time: seconds the team survived.
	record GameOver(double time, List<FinalScore> players) implements ServerMessage {
	}

	record FinalScore(String nickname, int score, boolean you) {
	}

	record BattleStarted(String code, List<String> players) implements ServerMessage {
	}

	// phase: players or enemy, with secondsLeft in it. problem and locked (seconds until a wrong answer stops locking
	// the options) are the receiving player's. strikes: indices in players of the warriors striking the goblin on this
	// tick; hits: of the players the goblin strikes.
	record BattleState(int turn, String phase, double secondsLeft, List<FighterState> players, FoeState enemy,
			ProblemState problem, double locked, List<Integer> strikes, List<Integer> hits) implements ServerMessage {
	}

	// charge: the damage the player's right answers add up to this turn. score: damage dealt over the battle.
	record FighterState(String nickname, int health, int maxHealth, int charge, int score, boolean you) {
	}

	// type: goblin or torch.
	record FoeState(int id, String type, int health, int maxHealth) {
	}

	// id: changes with every new problem. options: the answers to pick from, as shown.
	record ProblemState(int id, String text, List<String> options) {
	}

	// turns: the turns the team lasted. beaten: goblins beaten. Each player's score is the damage they dealt.
	record BattleOver(int turns, int beaten, List<FinalScore> players) implements ServerMessage {
	}

}
