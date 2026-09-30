package com.arithmancer.entity;

import com.arithmancer.math.Question;

public class Enemy extends Entity {

	// Starting values, to tune during development. A goblin dies after one answer; a torch goblin is slower, hits
	// harder and needs more.
	public enum Type {
		GOBLIN(10, 160),
		TORCH(20, 120);

		private final int attack;
		private final double speed;

		Type(int attack, double speed) {
			this.attack = attack;
			this.speed = speed;
		}
	}

	private final int id;
	private final Type type;
	private Question question;

	// speedFactor: how much faster than its type's speed, as enemies speed up over a run.
	public Enemy(int id, Type type, int maxHealth, Position position, Question question, double speedFactor) {
		super(maxHealth, type.attack, type.speed * speedFactor, position);
		this.id = id;
		this.type = type;
		this.question = question;
	}

	public int getId() {
		return id;
	}

	public Type getType() {
		return type;
	}

	public Question getQuestion() {
		return question;
	}

	public void setQuestion(Question question) {
		this.question = question;
	}

}
