package com.arithmancer.math.turnbased;

import java.util.List;
import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Problem;
import com.arithmancer.math.Written;
import com.arithmancer.math.turnbased.Polynomial.Term;

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

	// As x approaches a, (x - a)(x + b)/(x - a) = x + b approaches a + b, with the top multiplied out. The mistakes
	// stop at b, factor with the signs the other way round, take ab, or read 0/0 as 0.
	private static Problem factored(Random random) {
		int a = random.nextInt(1, 10);
		int b = random.nextInt(1, 10);
		Polynomial top = Polynomial.of(new Term(1, 2), new Term(b - a, 1), new Term(-a * b, 0)).map(term -> term);
		return Problem.number(random, "lim x→" + a + " (" + top + ")/(x - " + a + ")", a + b,
				List.of(b, a - b, a * b, 0));
	}

	// As x grows, (pxⁿ + q)/(rx² + s) approaches 0 when n is below 2, p/r when it is 2, and ∞ above. The mistakes
	// give one of the others, flip p/r, or take the constants instead.
	private static Problem atInfinity(Random random) {
		int n = random.nextInt(1, 4);
		int p = random.nextInt(1, 10);
		int q = random.nextInt(1, 10);
		int r = random.nextInt(2, 10);
		int s = random.nextInt(1, 10);
		String answer = n < 2 ? "0" : n == 2 ? Written.fraction(p, r) : "∞";
		return new Problem("lim x→∞ (" + Written.term(p, n) + " + " + q + ")/(" + Written.term(r, 2) + " + " + s + ")",
				answer, WrongAnswers.pick(random, answer, List.of(Written.fraction(p, r), Written.fraction(r, p),
						Written.fraction(q, s), "0", "∞"),
						() -> Written.fraction(random.nextInt(1, 10), random.nextInt(2, 10))));
	}

}
