package com.arithmancer.math;

import java.util.Random;

public record Question(String text, int answer) {

	// Answers are one digit, so a single key press answers a question: bigger results count by their last digit.
	private static final int DIGITS = 10;
	// Starting values, to tune during development: runs start with small + and -, then × and ÷ and larger numbers are
	// added.
	private static final double TIMES_TABLES_FROM_SECONDS = 60;
	private static final double LARGE_NUMBERS_FROM_SECONDS = 150;
	// Bases of powers and logarithms, each with its highest exponent.
	private static final int[][] POWERS = { { 2, 8 }, { 3, 5 }, { 4, 4 }, { 5, 3 }, { 10, 3 } };
	private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";
	private static final String SUBSCRIPTS = "₀₁₂₃₄₅₆₇₈₉";

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

	// Harder questions, for tougher enemies: powers, logarithms and limits.
	public static Question advanced(Random random) {
		return switch (random.nextInt(3)) {
			case 0 -> power(random);
			case 1 -> log(random);
			default -> limit(random);
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

	// A square up to 12², or a base raised up to its highest exponent in POWERS.
	private static Question power(Random random) {
		if (random.nextBoolean()) {
			int n = 1 + random.nextInt(12);
			return of(n + "²", n * n);
		}
		int[] power = POWERS[random.nextInt(POWERS.length)];
		int exponent = random.nextInt(power[1] + 1);
		return of(power[0] + digits(exponent, SUPERSCRIPTS), (int) Math.pow(power[0], exponent));
	}

	// The logarithm of one of the powers in POWERS: the exponent.
	private static Question log(Random random) {
		int[] power = POWERS[random.nextInt(POWERS.length)];
		int exponent = random.nextInt(power[1] + 1);
		return of("log" + digits(power[0], SUBSCRIPTS) + " " + (int) Math.pow(power[0], exponent), exponent);
	}

	// As x approaches a, (x² - a²)/(x - a) approaches 2a, and bx + c approaches ab + c.
	private static Question limit(Random random) {
		int a = 1 + random.nextInt(9);
		if (random.nextBoolean()) {
			return of("lim x→" + a + " (x²-" + a * a + ")/(x-" + a + ")", 2 * a);
		}
		int b = 1 + random.nextInt(5);
		int c = random.nextInt(10);
		String linear = (b == 1 ? "" : b) + "x" + (c == 0 ? "" : "+" + c);
		return of("lim x→" + a + " (" + linear + ")", a * b + c);
	}

	// Writes n with these characters for 0 to 9.
	private static String digits(int n, String characters) {
		StringBuilder written = new StringBuilder();
		for (char digit : String.valueOf(n).toCharArray()) {
			written.append(characters.charAt(digit - '0'));
		}
		return written.toString();
	}

}
