package com.arithmancer.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.arithmancer.entity.Enemy;
import com.arithmancer.entity.Position;
import com.arithmancer.map.Decoration;
import com.arithmancer.map.Detail;
import com.arithmancer.map.PathFinder;
import com.arithmancer.math.Question;
import com.arithmancer.room.Player;

public class Game {

	// Starting values, to tune during development. Spawns come faster over a run, down to a floor.
	private static final double SPAWN_INTERVAL_SECONDS = 2;
	private static final double MIN_SPAWN_INTERVAL_SECONDS = 0.6;
	private static final double SPAWN_INTERVAL_DROP_PER_SECOND = 0.01;
	private static final int MAX_ENEMIES = 30;
	// Enemies get up to this much faster, reached after SPEED_UP_SECONDS.
	private static final double MAX_SPEED_UP = 0.25;
	private static final double SPEED_UP_SECONDS = 300;
	// Torch goblins appear after TORCH_FROM_SECONDS, becoming up to MAX_TORCH_SHARE of spawns over TORCH_RAMP_SECONDS.
	// They need 2 answers, 3 from TORCH_THREE_ANSWERS_FROM_SECONDS on.
	private static final double TORCH_FROM_SECONDS = 45;
	private static final double TORCH_RAMP_SECONDS = 255;
	private static final double MAX_TORCH_SHARE = 0.35;
	private static final double TORCH_THREE_ANSWERS_FROM_SECONDS = 180;
	// Fixed logical view centered on each player, so screen size does not change who can hit what.
	// The frontend zooms its camera to show this view.
	private static final double VIEW_WIDTH = 960;
	private static final double VIEW_HEIGHT = 540;
	// Roughly outside the view.
	private static final double SPAWN_DISTANCE = 600;
	// Sizes as drawn by the frontend.
	private static final double PLAYER_RADIUS = 16;
	private static final double ENEMY_RADIUS = 14;
	private static final double CONTACT_DISTANCE = PLAYER_RADIUS + ENEMY_RADIUS;
	// Trees and stones are scattered around the start point, keeping it clear and leaving room to walk between them.
	private static final int TREES = 60;
	private static final int STONES = 30;
	private static final double DECORATION_RANGE = 2000;
	private static final double START_CLEARANCE = 200;
	private static final double DECORATION_SPACING = 120;
	private static final Position START = new Position(0, 0);
	// Details are scattered over the same area, apart from the trees, stones and each other.
	private static final int DETAILS = 250;
	private static final double DETAIL_SPACING = 50;
	// Types listed more than once are more common.
	private static final List<String> DETAIL_TYPES = List.of("bush", "bush", "bush", "mushroom", "mushroom", "pebble",
			"pebble", "pumpkin", "bone");

	private final String code;
	private final List<Player> players;
	private final List<Enemy> enemies = new ArrayList<>();
	private final List<Shot> shots = new ArrayList<>();
	private final Random random = new Random();
	private final List<Decoration> decorations;
	private final List<Detail> details;
	private final PathFinder pathFinder;
	private double secondsUntilSpawn = SPAWN_INTERVAL_SECONDS;
	private int nextEnemyId;
	private double elapsedSeconds;

	public Game(String code, List<Player> players) {
		this.code = code;
		this.players = List.copyOf(players);
		this.decorations = scatterDecorations();
		this.details = scatterDetails();
		this.pathFinder = new PathFinder(decorations, ENEMY_RADIUS);
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

	public List<Decoration> getDecorations() {
		return decorations;
	}

	public List<Detail> getDetails() {
		return details;
	}

	// Hits from the last tick.
	public List<Shot> getShots() {
		return shots;
	}

	// How long the run has lasted.
	public double getElapsedSeconds() {
		return elapsedSeconds;
	}

	// The run is lost once every player is dead.
	public boolean isOver() {
		return standingPlayers().isEmpty();
	}

	public void tick(double deltaSeconds) {
		// One simulation step: movement, spawning, questions, damage and revives go here.
		elapsedSeconds += deltaSeconds;
		shots.clear();
		players.forEach(player -> move(player, deltaSeconds));
		players.forEach(player -> answer(player, deltaSeconds));
		spawnEnemies(deltaSeconds);
		enemies.forEach(enemy -> chase(enemy, deltaSeconds));
		separateEnemies();
		enemies.removeIf(this::hitPlayer);
	}

	// y grows downward, as on screen. Dead players cannot move.
	private void move(Player player, double deltaSeconds) {
		int dx = held(player, "d") - held(player, "a");
		int dy = held(player, "s") - held(player, "w");
		if (player.getHealth() <= 0 || (dx == 0 && dy == 0)) {
			return;
		}
		// Divide by the direction's length so diagonal moves are not faster.
		double step = player.getSpeed() * deltaSeconds / Math.hypot(dx, dy);
		Position position = player.getPosition();
		player.setPosition(pushOutOfDecorations(new Position(position.x() + dx * step, position.y() + dy * step),
				PLAYER_RADIUS));
	}

	// Characters cannot walk into a decoration's solid circle: push them back to its edge, so they slide around it.
	private Position pushOutOfDecorations(Position position, double radius) {
		for (Decoration decoration : decorations) {
			Position center = decoration.solid().center();
			double minDistance = decoration.solid().radius() + radius;
			double distance = position.distanceTo(center);
			if (distance < minDistance && distance > 0) {
				double scale = minDistance / distance;
				position = new Position(center.x() + (position.x() - center.x()) * scale,
						center.y() + (position.y() - center.y()) * scale);
			}
		}
		return position;
	}

	private List<Decoration> scatterDecorations() {
		List<Decoration> placed = new ArrayList<>();
		for (int attempt = 0; attempt < 10_000 && placed.size() < TREES + STONES; attempt++) {
			Position base = new Position(random.nextDouble(-DECORATION_RANGE, DECORATION_RANGE),
					random.nextDouble(-DECORATION_RANGE, DECORATION_RANGE));
			boolean clear = base.distanceTo(START) >= START_CLEARANCE && placed.stream()
					.allMatch(decoration -> decoration.solid().center().distanceTo(base) >= DECORATION_SPACING);
			if (clear) {
				placed.add(placed.size() < TREES ? Decoration.tree(base) : Decoration.stone(base));
			}
		}
		return List.copyOf(placed);
	}

	private List<Detail> scatterDetails() {
		List<Detail> placed = new ArrayList<>();
		for (int attempt = 0; attempt < 10_000 && placed.size() < DETAILS; attempt++) {
			Position position = new Position(random.nextDouble(-DECORATION_RANGE, DECORATION_RANGE),
					random.nextDouble(-DECORATION_RANGE, DECORATION_RANGE));
			boolean clear = decorations.stream()
					.allMatch(decoration -> decoration.solid().center().distanceTo(position) >= DETAIL_SPACING)
					&& placed.stream().allMatch(detail -> detail.position().distanceTo(position) >= DETAIL_SPACING);
			if (clear) {
				placed.add(new Detail(DETAIL_TYPES.get(random.nextInt(DETAIL_TYPES.size())), position));
			}
		}
		return List.copyOf(placed);
	}

	private static int held(Player player, String key) {
		return player.isHeld(key) ? 1 : 0;
	}

	private void answer(Player player, double deltaSeconds) {
		player.coolDown(deltaSeconds);
		for (Integer answer = player.pollAnswer(); answer != null; answer = player.pollAnswer()) {
			// Any answer starts a cooldown, longer for a wrong one, and answers during it are ignored.
			if (player.getShotCooldown() > 0) {
				continue;
			}
			if (hitEnemy(player, answer)) {
				player.startShotCooldown();
			} else {
				player.lockAfterWrongAnswer();
			}
		}
	}

	// Of the enemies in the player's view with this answer, the nearest takes a hit. False when none has it.
	private boolean hitEnemy(Player player, int answer) {
		Position from = player.getPosition();
		Enemy target = enemies.stream()
				.filter(enemy -> enemy.getQuestion().answer() == answer && inView(from, enemy.getPosition()))
				.min(Comparator.comparingDouble(enemy -> from.distanceTo(enemy.getPosition())))
				.orElse(null);
		if (target == null) {
			return false;
		}
		shots.add(new Shot(player, target));
		target.setHealth(target.getHealth() - player.getAttack());
		if (target.getHealth() <= 0) {
			enemies.remove(target);
			player.addPoint();
		} else {
			target.setQuestion(newQuestion(target.getType()));
		}
		return true;
	}

	private static boolean inView(Position center, Position point) {
		return Math.abs(point.x() - center.x()) <= VIEW_WIDTH / 2
				&& Math.abs(point.y() - center.y()) <= VIEW_HEIGHT / 2;
	}

	private void spawnEnemies(double deltaSeconds) {
		secondsUntilSpawn -= deltaSeconds;
		List<Player> standing = standingPlayers();
		if (secondsUntilSpawn > 0 || enemies.size() >= MAX_ENEMIES || standing.isEmpty()) {
			return;
		}
		secondsUntilSpawn = Math.max(MIN_SPAWN_INTERVAL_SECONDS,
				SPAWN_INTERVAL_SECONDS - SPAWN_INTERVAL_DROP_PER_SECOND * elapsedSeconds);
		Position center = standing.get(random.nextInt(standing.size())).getPosition();
		double angle = random.nextDouble(2 * Math.PI);
		Position position = pushOutOfDecorations(new Position(center.x() + Math.cos(angle) * SPAWN_DISTANCE,
				center.y() + Math.sin(angle) * SPAWN_DISTANCE), ENEMY_RADIUS);
		double speedFactor = 1 + MAX_SPEED_UP * Math.min(1, elapsedSeconds / SPEED_UP_SECONDS);
		double torchShare = MAX_TORCH_SHARE
				* Math.clamp((elapsedSeconds - TORCH_FROM_SECONDS) / TORCH_RAMP_SECONDS, 0.0, 1.0);
		Enemy.Type type = random.nextDouble() < torchShare ? Enemy.Type.TORCH : Enemy.Type.GOBLIN;
		int maxHealth = type == Enemy.Type.GOBLIN ? 1 : elapsedSeconds < TORCH_THREE_ANSWERS_FROM_SECONDS ? 2 : 3;
		enemies.add(new Enemy(nextEnemyId++, type, maxHealth, position, newQuestion(type), speedFactor));
	}

	// Torch goblins ask powers, logarithms and limits; goblins ask sums that get harder over the run.
	private Question newQuestion(Enemy.Type type) {
		return type == Enemy.Type.TORCH ? Question.advanced(random) : Question.random(random, elapsedSeconds);
	}

	private void chase(Enemy enemy, double deltaSeconds) {
		Position from = enemy.getPosition();
		Player target = standingPlayers().stream()
				.min(Comparator.comparingDouble(player -> from.distanceTo(player.getPosition())))
				.orElse(null);
		if (target == null) {
			return;
		}
		Position to = pathFinder.waypoint(from, target.getPosition());
		double distance = from.distanceTo(to);
		double step = enemy.getSpeed() * deltaSeconds;
		if (distance <= step) {
			enemy.setPosition(to);
			return;
		}
		enemy.setPosition(pushOutOfDecorations(new Position(from.x() + (to.x() - from.x()) / distance * step,
				from.y() + (to.y() - from.y()) / distance * step), ENEMY_RADIUS));
	}

	// Overlapping enemies push each other apart, half the overlap each, so they crowd around a player instead of
	// stacking on the same spot. Whatever is left is resolved over the next ticks.
	private void separateEnemies() {
		for (int i = 0; i < enemies.size(); i++) {
			for (int j = i + 1; j < enemies.size(); j++) {
				Enemy a = enemies.get(i);
				Enemy b = enemies.get(j);
				double distance = a.getPosition().distanceTo(b.getPosition());
				double overlap = 2 * ENEMY_RADIUS - distance;
				if (overlap <= 0) {
					continue;
				}
				// Enemies on the exact same spot split in a random direction.
				double angle = distance > 0
						? Math.atan2(b.getPosition().y() - a.getPosition().y(), b.getPosition().x() - a.getPosition().x())
						: random.nextDouble(2 * Math.PI);
				double pushX = Math.cos(angle) * overlap / 2;
				double pushY = Math.sin(angle) * overlap / 2;
				a.setPosition(pushOutOfDecorations(
						new Position(a.getPosition().x() - pushX, a.getPosition().y() - pushY), ENEMY_RADIUS));
				b.setPosition(pushOutOfDecorations(
						new Position(b.getPosition().x() + pushX, b.getPosition().y() + pushY), ENEMY_RADIUS));
			}
		}
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
