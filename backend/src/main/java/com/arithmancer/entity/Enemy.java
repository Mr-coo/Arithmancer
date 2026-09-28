package com.arithmancer.entity;

public class Enemy extends Entity {

	// Starting values, to tune during development.
	private static final int MAX_HEALTH = 1;
	private static final int ATTACK = 10;
	private static final double SPEED = 120;

	private final int id;

	public Enemy(int id, Position position) {
		super(MAX_HEALTH, ATTACK, SPEED, position);
		this.id = id;
	}

	public int getId() {
		return id;
	}

}
