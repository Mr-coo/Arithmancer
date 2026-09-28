package com.arithmancer.entity;

public abstract class Entity {

	private final int maxHealth;
	private final int attack;
	private final double speed;
	private int health;
	private Position position;

	protected Entity(int maxHealth, int attack, double speed, Position position) {
		this.maxHealth = maxHealth;
		this.attack = attack;
		this.speed = speed;
		this.health = maxHealth;
		this.position = position;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	public int getAttack() {
		return attack;
	}

	public double getSpeed() {
		return speed;
	}

	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

}
