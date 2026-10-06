package com.arithmancer.math.survival;

import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Question;

// Algebra: solve ax + b = c for x, as in the reference, but with x at least 0.
public final class Algebra {

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
		int b = switch (difficulty) {
			case EASY -> random.nextInt(-10, 11);
			case MEDIUM -> random.nextInt(-30, 31);
			case HARD -> random.nextInt(-70, 71);
		};
		String constant = b > 0 ? " + " + b : b < 0 ? " - " + -b : "";
		return new Question(a + "x" + constant + " = " + (a * x + b), x);
	}

}
