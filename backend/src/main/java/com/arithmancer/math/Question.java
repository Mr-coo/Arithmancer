package com.arithmancer.math;

import java.util.Random;

public record Question(String text, int answer) {

	// Starting range, to tune during development: runs start with small + and -.
	private static final int MAX_OPERAND = 10;

	public static Question random(Random random) {
		int a = random.nextInt(MAX_OPERAND + 1);
		int b = random.nextInt(MAX_OPERAND + 1);
		if (random.nextBoolean()) {
			return new Question(a + " + " + b, a + b);
		}
		// Subtract the smaller number so the answer stays a whole number >= 0.
		return new Question(Math.max(a, b) + " - " + Math.min(a, b), Math.abs(a - b));
	}

}
