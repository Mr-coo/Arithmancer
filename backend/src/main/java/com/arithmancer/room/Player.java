package com.arithmancer.room;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.arithmancer.entity.Entity;
import com.arithmancer.entity.Position;

public class Player extends Entity {

	// Starting values, to tune during development.
	private static final int MAX_HEALTH = 100;
	private static final int ATTACK = 1;
	private static final double SPEED = 200;
	private static final double SHOT_COOLDOWN_SECONDS = 0.5;
	// A wrong answer locks the player's input for longer.
	private static final double WRONG_ANSWER_LOCK_SECONDS = 1;
	// Item boosts: shots cool down in HASTE_FACTOR of the time, and moves are SPEED_BOOST_FACTOR as fast.
	private static final double HASTE_FACTOR = 0.5;
	private static final double SPEED_BOOST_FACTOR = 1.5;

	private final String sessionId;
	private final String nickname;
	private final Set<String> heldKeys = ConcurrentHashMap.newKeySet();
	private final Queue<Integer> answers = new ConcurrentLinkedQueue<>();
	// Seconds until the player can shoot again, out of how long the current cooldown lasts.
	private double shotCooldown;
	private double maxShotCooldown = SHOT_COOLDOWN_SECONDS;
	// One point per enemy the player killed.
	private int score;
	// While downed: how far a teammate has got reviving the player, from 0 to 1.
	private double reviveProgress;
	// Seconds left of each item boost.
	private double hasteSeconds;
	private double speedBoostSeconds;
	// Set from the socket's thread when the player's connection closes during a run.
	private volatile boolean left;

	public Player(String sessionId, String nickname) {
		super(MAX_HEALTH, ATTACK, SPEED, new Position(0, 0));
		this.sessionId = sessionId;
		this.nickname = nickname;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getNickname() {
		return nickname;
	}

	public void press(String key) {
		heldKeys.add(key);
	}

	public void release(String key) {
		heldKeys.remove(key);
	}

	public boolean isHeld(String key) {
		return heldKeys.contains(key);
	}

	public void submitAnswer(int answer) {
		answers.add(answer);
	}

	// The next answer waiting for the game tick, or null when there is none.
	public Integer pollAnswer() {
		return answers.poll();
	}

	public double getShotCooldown() {
		return shotCooldown;
	}

	public double getMaxShotCooldown() {
		return maxShotCooldown;
	}

	@Override
	public double getSpeed() {
		return speedBoostSeconds > 0 ? super.getSpeed() * SPEED_BOOST_FACTOR : super.getSpeed();
	}

	public void startShotCooldown() {
		coolDownFor(hasteSeconds > 0 ? SHOT_COOLDOWN_SECONDS * HASTE_FACTOR : SHOT_COOLDOWN_SECONDS);
	}

	public void lockAfterWrongAnswer() {
		coolDownFor(WRONG_ANSWER_LOCK_SECONDS);
	}

	private void coolDownFor(double seconds) {
		shotCooldown = seconds;
		maxShotCooldown = seconds;
	}

	// Counts down the shot cooldown and the item boosts.
	public void coolDown(double deltaSeconds) {
		shotCooldown = Math.max(0, shotCooldown - deltaSeconds);
		hasteSeconds = Math.max(0, hasteSeconds - deltaSeconds);
		speedBoostSeconds = Math.max(0, speedBoostSeconds - deltaSeconds);
	}

	public void heal(int amount) {
		setHealth(Math.min(getMaxHealth(), getHealth() + amount));
	}

	public double getHasteSeconds() {
		return hasteSeconds;
	}

	public void boostHaste(double seconds) {
		hasteSeconds = seconds;
	}

	public double getSpeedBoostSeconds() {
		return speedBoostSeconds;
	}

	public void boostSpeed(double seconds) {
		speedBoostSeconds = seconds;
	}

	public double getReviveProgress() {
		return reviveProgress;
	}

	public void setReviveProgress(double reviveProgress) {
		this.reviveProgress = reviveProgress;
	}

	public void leave() {
		left = true;
	}

	public boolean hasLeft() {
		return left;
	}

	public int getScore() {
		return score;
	}

	public void addPoint() {
		score++;
	}

}
