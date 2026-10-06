package com.arithmancer.math.survival;

import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Question;
import com.arithmancer.math.Written;

// Exponents and logarithms: a power bⁿ, or the logarithm of one to base b, which is n, as in the reference. The
// exponent is lowered until the power is at most MAX_POWER.
public final class Exponents {

	private static final int MAX_POWER = 1000;

	private Exponents() {
	}

	public static Question ask(Random random, Difficulty difficulty) {
		int base = switch (difficulty) {
			case EASY -> random.nextInt(2, 5);
			case MEDIUM -> random.nextInt(2, 8);
			case HARD -> random.nextInt(2, 11);
		};
		int exponent = switch (difficulty) {
			case EASY -> random.nextInt(2, 5);
			case MEDIUM -> random.nextInt(2, 7);
			case HARD -> random.nextInt(2, 9);
		};
		while (Math.pow(base, exponent) > MAX_POWER) {
			exponent--;
		}
		int power = (int) Math.pow(base, exponent);
		if (random.nextBoolean()) {
			return new Question(base + Written.superscript(exponent), power);
		}
		return new Question("log" + Written.subscript(base) + " " + power, exponent);
	}

}
