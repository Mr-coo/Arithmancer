package com.arithmancer.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.arithmancer.entity.Enemy;
import com.arithmancer.entity.Position;
import com.arithmancer.math.Question;
import com.arithmancer.room.Player;

public class Game {

	// Starting values, to tune during development.
	private static final double SPAWN_INTERVAL_SECONDS = 2;
	private static final int MAX_ENEMIES = 20;
	// Roughly outside a 1280x720 view centered on the player.
	private static final double SPAWN_DISTANCE = 750;
	// Player radius 16 plus enemy radius 14, as drawn by the frontend.
	private static final double CONTACT_DISTANCE = 30;

	private final String code;
	private final List<Player> players;
	private final List<Enemy> enemies = new ArrayList<>();
	private final Random random = new Random();
	private double secondsUntilSpawn = SPAWN_INTERVAL_SECONDS;
	private int nextEnemyId;

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

	public List<Enemy> getEnemies() {
		return enemies;
	}

	public void tick(double deltaSeconds) {
		// One simulation step: movement, spawning, questions, damage and revives go here.
		players.forEach(player -> move(player, deltaSeconds));
		spawnEnemies(deltaSeconds);
		enemies.forEach(enemy -> chase(enemy, deltaSeconds));
		enemies.removeIf(this::hitPlayer);
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
		player.setPosition(new Position(position.x() + dx * step, position.y() + dy * step));
	}

	private static int held(Player player, String key) {
		return player.isHeld(key) ? 1 : 0;
	}

	private void spawnEnemies(double deltaSeconds) {
		secondsUntilSpawn -= deltaSeconds;
		List<Player> standing = standingPlayers();
		if (secondsUntilSpawn > 0 || enemies.size() >= MAX_ENEMIES || standing.isEmpty()) {
			return;
		}
		secondsUntilSpawn = SPAWN_INTERVAL_SECONDS;
		Position center = standing.get(random.nextInt(standing.size())).getPosition();
		double angle = random.nextDouble(2 * Math.PI);
		enemies.add(new Enemy(nextEnemyId++, new Position(center.x() + Math.cos(angle) * SPAWN_DISTANCE,
				center.y() + Math.sin(angle) * SPAWN_DISTANCE), Question.random(random)));
	}

	private void chase(Enemy enemy, double deltaSeconds) {
		Position from = enemy.getPosition();
		Player target = standingPlayers().stream()
				.min(Comparator.comparingDouble(player -> from.distanceTo(player.getPosition())))
				.orElse(null);
		if (target == null) {
			return;
		}
		Position to = target.getPosition();
		double distance = from.distanceTo(to);
		double step = enemy.getSpeed() * deltaSeconds;
		if (distance <= step) {
			enemy.setPosition(to);
			return;
		}
		enemy.setPosition(new Position(from.x() + (to.x() - from.x()) / distance * step,
				from.y() + (to.y() - from.y()) / distance * step));
	}

	// An enemy touching a standing player deals its attack as damage, then disappears.
	private boolean hitPlayer(Enemy enemy) {
		Player touched = standingPlayers().stream()
				.filter(player -> enemy.getPosition().distanceTo(player.getPosition()) <= CONTACT_DISTANCE)
				.findFirst()
				.orElse(null);
		if (touched == null) {
			return false;
		}
		touched.setHealth(Math.max(0, touched.getHealth() - enemy.getAttack()));
		return true;
	}

	// Players at 0 health are downed: enemies ignore them.
	private List<Player> standingPlayers() {
		return players.stream().filter(player -> player.getHealth() > 0).toList();
	}

}
