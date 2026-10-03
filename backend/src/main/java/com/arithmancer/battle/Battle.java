package com.arithmancer.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.arithmancer.entity.Enemy;
import com.arithmancer.math.Question;
import com.arithmancer.room.Player;

// A turn-based battle against one goblin at a time. On the players' turn, everyone answers their own problems at
// once, charging up a strike. On the goblin's turn, the warriors strike, then the goblin strikes back.
public class Battle {

	public enum Phase {
		PLAYERS, ENEMY
	}

	// Starting values, to tune during development.
	private static final double TURN_SECONDS = 15;
	// The warriors strike as the goblin's turn starts. The goblin acts ENEMY_ACTS_AT_SECONDS in, once their strikes have
	// played out on screen, and its turn lasts until its own move has too.
	private static final double ENEMY_TURN_SECONDS = 6;
	private static final double ENEMY_ACTS_AT_SECONDS = 2.8;
	// A wrong answer locks the player's options for this long, while the turn goes on.
	private static final double WRONG_ANSWER_LOCK_SECONDS = 3;
	// Each problem offers the answer and WRONG_OPTIONS other numbers, at least 0 and at most WRONG_OPTION_RANGE from it.
	private static final int WRONG_OPTIONS = 3;
	private static final int WRONG_OPTION_RANGE = 10;
	private static final int GOBLIN_HEALTH_PER_PLAYER = 15;
	private static final int GOBLIN_ATTACK = 10;

	private final String code;
	private final List<Player> players;
	private final Map<Player, Fighter> fighters = new HashMap<>();
	private final List<Player> strikes = new ArrayList<>();
	private final List<Player> hits = new ArrayList<>();
	private final Random random = new Random();
	private Foe foe;
	private int nextFoeId;
	private int beaten;
	private Phase phase = Phase.PLAYERS;
	private double secondsLeft = TURN_SECONDS;
	private boolean enemyActed;
	private boolean over;
	private int turn = 1;
	private double elapsedSeconds;

	public Battle(String code, List<Player> players) {
		this.code = code;
		this.players = List.copyOf(players);
		this.players.forEach(player -> fighters.put(player, new Fighter()));
		this.foe = nextFoe();
		fighters.values().forEach(this::pose);
	}

	public String getCode() {
		return code;
	}

	public List<Player> getPlayers() {
		return players;
	}

	public Fighter getFighter(Player player) {
		return fighters.get(player);
	}

	public Foe getFoe() {
		return foe;
	}

	public Phase getPhase() {
		return phase;
	}

	// Seconds left in the current phase.
	public double getSecondsLeft() {
		return secondsLeft;
	}

	public int getTurn() {
		return turn;
	}

	// The players whose warriors struck the goblin on the last tick.
	public List<Player> getStrikes() {
		return strikes;
	}

	// The players the goblin struck on the last tick.
	public List<Player> getHits() {
		return hits;
	}

	public int getBeaten() {
		return beaten;
	}

	// The battle is lost once every player is downed, at the end of the goblin's turn, so its last attack plays out.
	public boolean isOver() {
		return over;
	}

	public void tick(double deltaSeconds) {
		elapsedSeconds += deltaSeconds;
		secondsLeft -= deltaSeconds;
		strikes.clear();
		hits.clear();
		players.forEach(player -> answer(player, deltaSeconds));
		if (phase == Phase.PLAYERS && secondsLeft <= 0) {
			strike();
		} else if (phase == Phase.ENEMY && !enemyActed && secondsLeft <= ENEMY_TURN_SECONDS - ENEMY_ACTS_AT_SECONDS) {
			enemyActs();
		} else if (phase == Phase.ENEMY && secondsLeft <= 0) {
			if (standingPlayers().isEmpty()) {
				over = true;
			} else {
				startTurn();
			}
		}
	}

	private void answer(Player player, double deltaSeconds) {
		Fighter fighter = fighters.get(player);
		fighter.coolDown(deltaSeconds);
		for (Integer answer = player.pollAnswer(); answer != null; answer = player.pollAnswer()) {
			// Answers only count on the players' turn, from standing players whose options are not locked.
			if (phase != Phase.PLAYERS || player.getHealth() <= 0 || fighter.getLockSeconds() > 0) {
				continue;
			}
			if (answer == fighter.getProblem().result()) {
				fighter.answerRight();
			} else {
				fighter.lock(WRONG_ANSWER_LOCK_SECONDS);
			}
			pose(fighter);
		}
	}

	// The players' turn is over: every warrior with a charge strikes the goblin, and the goblin's turn starts.
	private void strike() {
		for (Player player : players) {
			int damage = fighters.get(player).strike();
			if (damage > 0) {
				strikes.add(player);
				foe.takeDamage(damage);
			}
		}
		phase = Phase.ENEMY;
		secondsLeft = ENEMY_TURN_SECONDS;
		enemyActed = false;
	}

	// A beaten goblin is replaced by the next one. Otherwise the goblin strikes every standing player.
	private void enemyActs() {
		enemyActed = true;
		if (foe.getHealth() <= 0) {
			beaten++;
			foe = nextFoe();
			return;
		}
		for (Player player : standingPlayers()) {
			player.setHealth(Math.max(0, player.getHealth() - foe.getAttack()));
			hits.add(player);
		}
	}

	// Everyone starts the players' turn with a new problem and no lock.
	private void startTurn() {
		turn++;
		phase = Phase.PLAYERS;
		secondsLeft = TURN_SECONDS;
		for (Fighter fighter : fighters.values()) {
			fighter.lock(0);
			pose(fighter);
		}
	}

	// Players at 0 health are downed: they cannot answer, and the goblin leaves them alone.
	private List<Player> standingPlayers() {
		return players.stream().filter(player -> player.getHealth() > 0).toList();
	}

	// Torch goblins, whose torch swing reads well as an attack.
	private Foe nextFoe() {
		return new Foe(nextFoeId++, Enemy.Type.TORCH, GOBLIN_HEALTH_PER_PLAYER * players.size(), GOBLIN_ATTACK);
	}

	// Problems follow the real-time ramp by how long the battle has lasted.
	private void pose(Fighter fighter) {
		Question problem = Question.random(random, elapsedSeconds);
		fighter.pose(problem, options(problem.result()));
	}

	// The answer and WRONG_OPTIONS different numbers near it, in random order.
	private List<Integer> options(int answer) {
		List<Integer> wrong = new ArrayList<>();
		for (int n = Math.max(0, answer - WRONG_OPTION_RANGE); n <= answer + WRONG_OPTION_RANGE; n++) {
			if (n != answer) {
				wrong.add(n);
			}
		}
		Collections.shuffle(wrong, random);
		List<Integer> options = new ArrayList<>(wrong.subList(0, WRONG_OPTIONS));
		options.add(answer);
		Collections.shuffle(options, random);
		return List.copyOf(options);
	}

}
