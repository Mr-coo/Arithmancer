package com.arithmancer.game;

import com.arithmancer.entity.Position;

// Dropped by a killed enemy, and picked up by the first standing player to touch it.
public class Item {

	public enum Type {
		// Restores some health.
		HEAL,
		// Shots cool down faster for a while.
		HASTE,
		// Moves are faster for a while.
		SPEED
	}

	private final int id;
	private final Type type;
	private final Position position;
	// Seconds until it disappears.
	private double secondsLeft;

	public Item(int id, Type type, Position position, double secondsLeft) {
		this.id = id;
		this.type = type;
		this.position = position;
		this.secondsLeft = secondsLeft;
	}

	public int getId() {
		return id;
	}

	public Type getType() {
		return type;
	}

	public Position getPosition() {
		return position;
	}

	public void age(double deltaSeconds) {
		secondsLeft -= deltaSeconds;
	}

	public boolean isGone() {
		return secondsLeft <= 0;
	}

}
