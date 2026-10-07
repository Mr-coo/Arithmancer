package com.arithmancer.math.survival;

import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Question;

// Algebra: solve ax + b = c for x, as in the reference, but with x at least 0. From medium on, half the equations are
// bracketed instead, a(x + b) = c, with x at least 1.
public final class Algebra {

	// The bracketed b is at most this far from 0.
	private static final int MAX_BRACKETED = 10;

	private Algebra() {
	}

	public static Question ask(Random random, Difficulty difficulty) {
		int x = switch (difficulty) {
			case EASY -> random.nextInt(1, 11);
			case MEDIUM -> random.nextInt(0, 21);
			case HARD -> random.nextInt(0, 51);
		};
		int a = switch (difficulty) {
			case EASY -> random.nextInt(2, 6);
			case MEDIUM -> random.nextInt(2, 11);
			case HARD -> random.nextInt(2, 21);
		};
		if (difficulty != Difficulty.EASY && random.nextBoolean()) {
			x = Math.max(x, 1);
			int b = random.nextInt(1, MAX_BRACKETED + 1) * (random.nextBoolean() ? 1 : -1);
			return new Question(a + "(x " + (b > 0 ? "+ " + b : "- " + -b) + ") = " + a * (x + b), x);
		}
		int b = switch (difficulty) {
			case EASY -> random.nextInt(-10, 11);
			case MEDIUM -> random.nextInt(-30, 31);
			case HARD -> random.nextInt(-70, 71);
		};
		String constant = b > 0 ? " + " + b : b < 0 ? " - " + -b : "";
		return new Question(a + "x" + constant + " = " + (a * x + b), x);
	}

}
