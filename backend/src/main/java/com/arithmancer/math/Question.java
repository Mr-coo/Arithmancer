package com.arithmancer.math;

import java.util.Random;

public record Question(String text, int answer) {

	// Starting range, to tune during development: runs start with small + and -.
	private static final int MAX_OPERAND = 10;
	// Answers are one digit, so a single key press answers a question.
	private static final int MAX_ANSWER = 9;

	public static Question random(Random random) {
		int answer = random.nextInt(MAX_ANSWER + 1);
		if (random.nextBoolean()) {
			int a = random.nextInt(answer + 1);
			return new Question(a + " + " + (answer - a), answer);
		}
		int b = random.nextInt(MAX_OPERAND - answer + 1);
		return new Question((answer + b) + " - " + b, answer);
	}

}
