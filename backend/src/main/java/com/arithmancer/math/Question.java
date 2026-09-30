package com.arithmancer.math;

import java.util.Random;

public record Question(String text, int answer) {

	// Answers are one digit, so a single key press answers a question: bigger results count by their last digit.
	private static final int DIGITS = 10;
	// Starting values, to tune during development: runs start with small + and -, then × and ÷ and larger numbers are
	// added.
	private static final double TIMES_TABLES_FROM_SECONDS = 60;
	private static final double LARGE_NUMBERS_FROM_SECONDS = 150;

	public static Question random(Random random, double elapsedSeconds) {
		if (elapsedSeconds < TIMES_TABLES_FROM_SECONDS) {
			return addOrSubtract(random, 10);
		}
		boolean large = elapsedSeconds >= LARGE_NUMBERS_FROM_SECONDS;
		return switch (random.nextInt(4)) {
			case 0, 1 -> addOrSubtract(random, large ? 50 : 20);
			case 2 -> multiply(random, large ? 10 : 5);
			default -> divide(random, large ? 10 : 5);
		};
	}

	// The answer to a question whose result is this.
	static Question of(String text, int result) {
		return new Question(text, result % DIGITS);
	}

	// Results and numbers from 0 to max.
	private static Question addOrSubtract(Random random, int max) {
		int result = random.nextInt(max + 1);
		if (random.nextBoolean()) {
			int a = random.nextInt(result + 1);
			return of(a + " + " + (result - a), result);
		}
		int b = random.nextInt(max - result + 1);
		return of((result + b) + " - " + b, result);
	}

	// Factors from 1 to max.
	private static Question multiply(Random random, int max) {
		int a = 1 + random.nextInt(max);
		int b = 1 + random.nextInt(max);
		return of(a + " × " + b, a * b);
	}

	// The divisor and the whole result from 1 to max.
	private static Question divide(Random random, int max) {
		int divisor = 1 + random.nextInt(max);
		int result = 1 + random.nextInt(max);
		return of(divisor * result + " ÷ " + divisor, result);
	}

}
