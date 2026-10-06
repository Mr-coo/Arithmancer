package com.arithmancer.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.arithmancer.entity.Enemy;
import com.arithmancer.entity.Position;
import com.arithmancer.map.Decoration;
import com.arithmancer.map.Detail;
import com.arithmancer.map.PathFinder;
import com.arithmancer.math.Question;
import com.arithmancer.math.Topic;
import com.arithmancer.room.Player;

public class Game {

	// Starting values, to tune during development. Enemies come in rounds, each a herd of HERD_SIZE enemies and
	// HERD_GROWTH more every round. They spawn SPAWN_INTERVAL_SECONDS apart, SPAWN_INTERVAL_FACTOR as long every round,
	// down to a floor. Every round starts after ROUND_BREAK_SECONDS, the next once the herd is all gone.
	private static final int HERD_SIZE = 8;
	private static final int HERD_GROWTH = 4;
	private static final double SPAWN_INTERVAL_SECONDS = 2;
	private static final double SPAWN_INTERVAL_FACTOR = 0.85;
	private static final double MIN_SPAWN_INTERVAL_SECONDS = 0.4;
	private static final double ROUND_BREAK_SECONDS = 5;
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
	// A downed player comes back with REVIVE_HEALTH_SHARE of their health once a standing teammate has stayed within
	// REVIVE_DISTANCE for REVIVE_SECONDS.
	private static final double REVIVE_DISTANCE = 48;
	private static final double REVIVE_SECONDS = 3;
	private static final double REVIVE_HEALTH_SHARE = 0.5;
	// A killed enemy drops an item with DROP_CHANCE, up to MAX_ITEMS lying around, each gone after ITEM_SECONDS. A heal
	// restores HEAL_AMOUNT health; boosts last BOOST_SECONDS.
	private static final double DROP_CHANCE = 0.15;
	private static final int MAX_ITEMS = 6;
	private static final double ITEM_SECONDS = 20;
	private static final int HEAL_AMOUNT = 30;
	private static final double BOOST_SECONDS = 8;
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
	// A player touching an item picks it up.
	private static final double PICKUP_DISTANCE = PLAYER_RADIUS + 12;
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
	private final Topic topic;
	private final List<Enemy> enemies = new ArrayList<>();
	private final List<Shot> shots = new ArrayList<>();
	private final List<Item> items = new ArrayList<>();
	private final Random random = new Random();
	private final List<Decoration> decorations;
	private final List<Detail> details;
	private final PathFinder pathFinder;
	private int round;
	// Enemies of this round's herd still to spawn.
	private int herdLeft;
	// Seconds left of the break before this round's herd starts coming.
	private double roundStartsIn;
	private double secondsUntilSpawn;
	private int nextEnemyId;
	private int nextItemId;
	private double elapsedSeconds;

	public Game(String code, List<Player> players, Topic topic) {
		this.code = code;
		this.players = List.copyOf(players);
		this.topic = topic;
		this.decorations = scatterDecorations();
		this.details = scatterDetails();
		this.pathFinder = new PathFinder(decorations, ENEMY_RADIUS);
		startRound();
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

	public List<Item> getItems() {
		return items;
	}

	// Hits from the last tick.
	public List<Shot> getShots() {
		return shots;
	}

	// How long the run has lasted.
	public double getElapsedSeconds() {
		return elapsedSeconds;
	}

	public int getRound() {
		return round;
	}

	// Seconds until this round's herd starts coming, 0 once it has.
	public double getRoundStartsIn() {
		return roundStartsIn;
	}

	// The run is lost once every player is dead.
	public boolean isOver() {
		return standingPlayers().isEmpty();
	}

	public void tick(double deltaSeconds) {
		// One simulation step: movement, spawning, questions, damage and revives go here.
		elapsedSeconds += deltaSeconds;
		shots.clear();
		// Players who left are downed for good.
		players.stream().filter(Player::hasLeft).forEach(player -> player.setHealth(0));
		players.forEach(player -> move(player, deltaSeconds));
		pickUpItems(deltaSeconds);
		players.forEach(player -> revive(player, deltaSeconds));
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

	private void dropItem(Position position) {
		if (items.size() < MAX_ITEMS && random.nextDouble() < DROP_CHANCE) {
			Item.Type[] types = Item.Type.values();
			items.add(new Item(nextItemId++, types[random.nextInt(types.length)], position, ITEM_SECONDS));
		}
	}

	// Items wear away, and the first standing player touching one picks it up.
	private void pickUpItems(double deltaSeconds) {
		for (Iterator<Item> iterator = items.iterator(); iterator.hasNext();) {
			Item item = iterator.next();
			item.age(deltaSeconds);
			Player picker = standingPlayers().stream()
					.filter(player -> player.getPosition().distanceTo(item.getPosition()) <= PICKUP_DISTANCE)
					.findFirst()
					.orElse(null);
			if (picker != null) {
				switch (item.getType()) {
					case HEAL -> picker.heal(HEAL_AMOUNT);
					case HASTE -> picker.boostHaste(BOOST_SECONDS);
					case SPEED -> picker.boostSpeed(BOOST_SECONDS);
				}
			}
			if (picker != null || item.isGone()) {
				iterator.remove();
			}
		}
	}

	// A downed player is revived while a standing teammate stays next to them; progress is lost when nobody is. Players
	// who left cannot be revived.
	private void revive(Player player, double deltaSeconds) {
		if (player.getHealth() > 0 || player.hasLeft()) {
			return;
		}
		boolean helped = standingPlayers().stream()
				.anyMatch(teammate -> teammate.getPosition().distanceTo(player.getPosition()) <= REVIVE_DISTANCE);
		if (!helped) {
			player.setReviveProgress(0);
			return;
		}
		player.setReviveProgress(player.getReviveProgress() + deltaSeconds / REVIVE_SECONDS);
		if (player.getReviveProgress() >= 1) {
			player.setReviveProgress(0);
			player.setHealth((int) Math.round(player.getMaxHealth() * REVIVE_HEALTH_SHARE));
		}
	}

	private static int held(Player player, String key) {
		return player.isHeld(key) ? 1 : 0;
	}

	private void answer(Player player, double deltaSeconds) {
		player.coolDown(deltaSeconds);
		for (Integer answer = player.pollAnswer(); answer != null; answer = player.pollAnswer()) {
			// Any answer starts a cooldown, longer for a wrong one, and answers during it are ignored. Downed players
			// cannot shoot.
			if (player.getShotCooldown() > 0 || player.getHealth() <= 0) {
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
			dropItem(target.getPosition());
		} else {
			target.setQuestion(newQuestion());
		}
		return true;
	}

	private static boolean inView(Position center, Position point) {
		return Math.abs(point.x() - center.x()) <= VIEW_WIDTH / 2
				&& Math.abs(point.y() - center.y()) <= VIEW_HEIGHT / 2;
	}

	// After the break, the round's herd spawns one enemy at a time. Once it has all spawned and none are left, the next
	// round starts.
	private void spawnEnemies(double deltaSeconds) {
		if (roundStartsIn > 0) {
			roundStartsIn = Math.max(0, roundStartsIn - deltaSeconds);
			return;
		}
		if (herdLeft == 0) {
			if (enemies.isEmpty()) {
				startRound();
			}
			return;
		}
		secondsUntilSpawn -= deltaSeconds;
		List<Player> standing = standingPlayers();
		if (secondsUntilSpawn > 0 || enemies.size() >= MAX_ENEMIES || standing.isEmpty()) {
			return;
		}
		secondsUntilSpawn = Math.max(MIN_SPAWN_INTERVAL_SECONDS,
				SPAWN_INTERVAL_SECONDS * Math.pow(SPAWN_INTERVAL_FACTOR, round - 1));
		herdLeft--;
		Position center = standing.get(random.nextInt(standing.size())).getPosition();
		double angle = random.nextDouble(2 * Math.PI);
		Position position = pushOutOfDecorations(new Position(center.x() + Math.cos(angle) * SPAWN_DISTANCE,
				center.y() + Math.sin(angle) * SPAWN_DISTANCE), ENEMY_RADIUS);
		double speedFactor = 1 + MAX_SPEED_UP * Math.min(1, elapsedSeconds / SPEED_UP_SECONDS);
		double torchShare = MAX_TORCH_SHARE
				* Math.clamp((elapsedSeconds - TORCH_FROM_SECONDS) / TORCH_RAMP_SECONDS, 0.0, 1.0);
		Enemy.Type type = random.nextDouble() < torchShare ? Enemy.Type.TORCH : Enemy.Type.GOBLIN;
		int maxHealth = type == Enemy.Type.GOBLIN ? 1 : elapsedSeconds < TORCH_THREE_ANSWERS_FROM_SECONDS ? 2 : 3;
		enemies.add(new Enemy(nextEnemyId++, type, maxHealth, position, newQuestion(), speedFactor));
	}

	// The next round's herd is bigger, and its first enemy comes as soon as the break is over.
	private void startRound() {
		round++;
		herdLeft = HERD_SIZE + HERD_GROWTH * (round - 1);
		roundStartsIn = ROUND_BREAK_SECONDS;
		secondsUntilSpawn = 0;
	}

	// Every enemy asks a question of the run's topic, with numbers that grow over the run.
	private Question newQuestion() {
		return Question.random(random, topic, elapsedSeconds);
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
