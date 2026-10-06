package com.arithmancer.math.turnbased;

import java.util.List;
import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Problem;
import com.arithmancer.math.Written;
import com.arithmancer.math.turnbased.Polynomial.Term;

// Integrals of polynomials: indefinite ones of one term at first, then of two, then definite ones between whole bounds.
// The antiderivative is picked first, so its coefficients are whole.
public final class Integrals {

	private Integrals() {
	}

	public static Problem pose(Random random, Difficulty difficulty) {
		Polynomial antiderivative = switch (difficulty) {
			case EASY -> Polynomial.of(new Term(random.nextInt(1, 7), random.nextInt(2, 6)));
			case MEDIUM, HARD -> {
				int exponent = random.nextInt(2, 5);
				yield Polynomial.of(new Term(random.nextInt(1, 7), exponent),
						new Term(Derivatives.signed(random), random.nextInt(1, exponent)));
			}
		};
		Polynomial integrand = antiderivative.derivative();
		return difficulty == Difficulty.HARD ? definite(random, antiderivative, integrand)
				: indefinite(random, antiderivative, integrand);
	}

	// ∫ f dx = F + C. The mistakes do not divide by the raised exponent, differentiate instead, keep the exponent, or
	// leave out the + C.
	private static Problem indefinite(Random random, Polynomial antiderivative, Polynomial integrand) {
		String answer = antiderivative + " + C";
		List<String> mistakes = List.of(
				integrand.map(term -> new Term(term.coefficient(), term.exponent() + 1)) + " + C",
				integrand.derivative() + " + C", integrand + " + C", antiderivative.toString());
		return new Problem("∫ " + bracketed(integrand) + " dx", answer, WrongAnswers.pick(random, answer, mistakes,
				() -> Derivatives.nudge(random, antiderivative) + " + C"));
	}

	// The integral from a to b is F(b) - F(a). The mistakes take F(b) alone, work out f(b) - f(a), or F(a) - F(b).
	private static Problem definite(Random random, Polynomial antiderivative, Polynomial integrand) {
		int from = random.nextInt(0, 3);
		int to = random.nextInt(from + 1, 4);
		int answer = antiderivative.at(to) - antiderivative.at(from);
		String text = "∫" + Written.subscript(from) + Written.superscript(to) + " " + bracketed(integrand) + " dx";
		return Problem.number(random, text, answer,
				List.of(antiderivative.at(to), integrand.at(to) - integrand.at(from), -answer));
	}

	// The polynomial, bracketed when it has more than one term.
	private static String bracketed(Polynomial polynomial) {
		return polynomial.terms().size() > 1 ? "(" + polynomial + ")" : polynomial.toString();
	}

}
