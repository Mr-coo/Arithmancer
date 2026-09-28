package com.arithmancer.entity;

import com.arithmancer.math.Question;

public class Enemy extends Entity {

	// Starting values, to tune during development.
	private static final int MAX_HEALTH = 1;
	private static final int ATTACK = 10;
	private static final double SPEED = 120;

	private final int id;
	private Question question;

	public Enemy(int id, Position position, Question question) {
		super(MAX_HEALTH, ATTACK, SPEED, position);
		this.id = id;
		this.question = question;
	}

	public int getId() {
		return id;
	}

	public Question getQuestion() {
		return question;
	}

	public void setQuestion(Question question) {
		this.question = question;
	}

}
