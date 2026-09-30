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

	private final String sessionId;
	private final String nickname;
	private final Set<String> heldKeys = ConcurrentHashMap.newKeySet();
	private final Queue<Integer> answers = new ConcurrentLinkedQueue<>();
	// Seconds until the player can shoot again, out of how long the current cooldown lasts.
	private double shotCooldown;
	private double maxShotCooldown = SHOT_COOLDOWN_SECONDS;
	// One point per enemy the player killed.
	private int score;

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

	public void startShotCooldown() {
		coolDownFor(SHOT_COOLDOWN_SECONDS);
	}

	public void lockAfterWrongAnswer() {
		coolDownFor(WRONG_ANSWER_LOCK_SECONDS);
	}

	private void coolDownFor(double seconds) {
		shotCooldown = seconds;
		maxShotCooldown = seconds;
	}

	public void coolDown(double deltaSeconds) {
		shotCooldown = Math.max(0, shotCooldown - deltaSeconds);
	}

	public int getScore() {
		return score;
	}

	public void addPoint() {
		score++;
	}

}
