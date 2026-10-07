package com.arithmancer.math.survival;

import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Question;

// Integers and fractions: a fraction that comes out whole, then +, -, × or ÷ an integer, as in the reference; fractions
// that are not whole but add up, or multiply, to a whole number; and from medium on, negative integers. Results are
// whole and at least 0, and at least 1 for the last two kinds. Unlike the reference, a division's quotient is kept
// small, so its fraction is no bigger than the others'.
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
		int kinds = difficulty == Difficulty.EASY ? 2 : 3;
		return switch (random.nextInt(kinds)) {
			case 0 -> wholeFraction(random, difficulty, denominator, maxWhole);
			case 1 -> realFractions(random, denominator, maxWhole);
			default -> negatives(random, maxWhole);
		};
	}

	// A fraction that comes out whole, then +, -, × or ÷ an integer. From medium on, a subtraction can take away a
	// negative integer.
	private static Question wholeFraction(Random random, Difficulty difficulty, int denominator, int maxWhole) {
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
			default -> {
				int quotient = random.nextInt(1, maxWhole / factor + 1);
				yield new Question(denominator * quotient * factor + "/" + denominator + " ÷ " + factor, quotient);
			}
		};
	}

	// Fractions that are not whole but come out whole: a/d + b/d or a/d - b/d, or a fraction below 1 times a multiple
	// of its denominator, as in 2/3 × 9.
	private static Question realFractions(Random random, int denominator, int maxWhole) {
		int result = random.nextInt(1, maxWhole / 2 + 1);
		int part;
		do {
			part = random.nextInt(1, result * denominator);
		} while (part % denominator == 0);
		return switch (random.nextInt(3)) {
			case 0 -> new Question(part + "/" + denominator + " + " + (result * denominator - part) + "/" + denominator,
					result);
			case 1 -> new Question((part + result * denominator) + "/" + denominator + " - " + part + "/" + denominator,
					result);
			default -> {
				int top = random.nextInt(1, denominator);
				int times = random.nextInt(1, 10);
				yield new Question(top + "/" + denominator + " × " + denominator * times, top * times);
			}
		};
	}

	// Negative integers: (-a) × (-b), -a + b with b above a, or (-ab) ÷ (-b).
	private static Question negatives(Random random, int maxWhole) {
		int a = random.nextInt(2, 10);
		int b = random.nextInt(2, 10);
		return switch (random.nextInt(3)) {
			case 0 -> new Question("(-" + a + ") × (-" + b + ")", a * b);
			case 1 -> {
				int taken = random.nextInt(1, maxWhole + 1);
				int added = random.nextInt(taken + 1, taken + maxWhole + 1);
				yield new Question("-" + taken + " + " + added, added - taken);
			}
			default -> new Question("-" + a * b + " ÷ (-" + b + ")", a);
		};
	}

}
