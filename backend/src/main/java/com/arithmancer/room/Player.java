package com.arithmancer.room;

import com.arithmancer.entity.Entity;
import com.arithmancer.entity.Position;

public class Player extends Entity {

	// Starting values, to tune during development.
	private static final int MAX_HEALTH = 100;
	private static final int ATTACK = 1;
	private static final double SPEED = 200;

	private final String sessionId;
	private final String nickname;

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

}
