package com.arithmancer.math.turnbased;

import java.util.List;
import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Problem;

// Limits: of a line at first, then of quotients that only settle once factored, then of quotients at infinity too.
public final class Limits {

	private Limits() {
	}

	public static Problem pose(Random random, Difficulty difficulty) {
		return switch (difficulty) {
			case EASY -> line(random);
			case MEDIUM -> factored(random);
			case HARD -> random.nextBoolean() ? factored(random) : atInfinity(random);
		};
	}

	// As x approaches a, bx + c approaches ab + c. The mistakes leave out b, a or c.
	private static Problem line(Random random) {
		int a = random.nextInt(1, 10);
		int b = random.nextInt(2, 6);
		int c = random.nextInt(1, 10);
		return Problem.number(random, "lim x→" + a + " (" + b + "x + " + c + ")", a * b + c,
				List.of(a + c, b + c, a * b));
	}

	// As x approaches a, (x² - a²)/(x - a) = x + a approaches 2a. The mistakes stop at a or a², or read 0/0 as 0.
	private static Problem factored(Random random) {
		int a = random.nextInt(1, 10);
		return Problem.number(random, "lim x→" + a + " (x² - " + a * a + ")/(x - " + a + ")", 2 * a,
				List.of(a, a * a, 0));
	}

	// As x grows, (px² + q)/(rx² + s) approaches p/r. The mistakes flip it, take the constants instead, or give 0 or ∞.
	private static Problem atInfinity(Random random) {
		int p = random.nextInt(1, 10);
		int q = random.nextInt(1, 10);
		int r = random.nextInt(2, 10);
		int s = random.nextInt(1, 10);
		String answer = Written.fraction(p, r);
		return new Problem("lim x→∞ (" + Written.term(p, 2) + " + " + q + ")/(" + Written.term(r, 2) + " + " + s + ")",
				answer, WrongAnswers.pick(random, answer, List.of(Written.fraction(r, p), Written.fraction(q, s), "0", "∞"),
						() -> Written.fraction(random.nextInt(1, 10), random.nextInt(2, 10))));
	}

}
