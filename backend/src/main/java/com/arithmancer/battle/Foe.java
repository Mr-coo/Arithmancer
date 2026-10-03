package com.arithmancer.battle;

import com.arithmancer.entity.Enemy;

// The goblin the players are fighting.
public class Foe {

	private final int id;
	private final Enemy.Type type;
	private final int maxHealth;
	private final int attack;
	private int health;

	public Foe(int id, Enemy.Type type, int maxHealth, int attack) {
		this.id = id;
		this.type = type;
		this.maxHealth = maxHealth;
		this.attack = attack;
		this.health = maxHealth;
	}

	public int getId() {
		return id;
	}

	public Enemy.Type getType() {
		return type;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	public int getAttack() {
		return attack;
	}

	public int getHealth() {
		return health;
	}

	void takeDamage(int damage) {
		health = Math.max(0, health - damage);
	}

}
