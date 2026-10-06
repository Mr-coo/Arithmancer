package com.arithmancer.math.survival;

import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Question;

// Integers and fractions: a fraction that comes out whole, then +, -, × or ÷ an integer, as in the reference. From
// medium on, a subtraction can take away a negative integer. Results are whole and at least 0.
public final class Fractions {

	private Fractions() {
	}

	public static Question ask(Random random, Difficulty difficulty) {
		int denominator = switch (difficulty) {
			case EASY -> random.nextInt(2, 6);
			case MEDIUM -> random.nextInt(3, 11);
			case HARD -> random.nextInt(5, 16);
		};
		int maxWhole = switch (difficulty) {
			case EASY -> 10;
			case MEDIUM -> 30;
			case HARD -> 60;
		};
		int whole = random.nextInt(1, maxWhole + 1);
		String fraction = denominator * whole + "/" + denominator;
		int integer = random.nextInt(1, maxWhole + 1);
		int factor = random.nextInt(2, 10);
		return switch (random.nextInt(4)) {
			case 0 -> new Question(fraction + " + " + integer, whole + integer);
			case 1 -> {
				if (difficulty != Difficulty.EASY && random.nextBoolean()) {
					yield new Question(fraction + " - (-" + integer + ")", whole + integer);
				}
				int taken = random.nextInt(whole + 1);
				yield new Question(fraction + " - " + taken, whole - taken);
			}
			case 2 -> new Question(fraction + " × " + factor, whole * factor);
			default -> new Question(denominator * whole * factor + "/" + denominator + " ÷ " + factor, whole);
		};
	}

}
