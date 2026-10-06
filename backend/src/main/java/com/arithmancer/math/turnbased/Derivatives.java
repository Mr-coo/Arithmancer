package com.arithmancer.math.turnbased;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Problem;
import com.arithmancer.math.turnbased.Polynomial.Term;

// Derivatives of polynomials: one term at first, then two, then three with a constant.
public final class Derivatives {

	private Derivatives() {
	}

	public static Problem pose(Random random, Difficulty difficulty) {
		Polynomial function = switch (difficulty) {
			case EASY -> Polynomial.of(new Term(random.nextInt(2, 10), random.nextInt(2, 6)));
			case MEDIUM -> {
				int exponent = random.nextInt(2, 5);
				yield Polynomial.of(new Term(random.nextInt(1, 10), exponent),
						new Term(signed(random), random.nextInt(1, exponent)));
			}
			case HARD -> {
				int exponent = random.nextInt(3, 6);
				yield Polynomial.of(new Term(random.nextInt(1, 10), exponent),
						new Term(signed(random), random.nextInt(1, exponent)), new Term(signed(random), 0));
			}
		};
		Polynomial answer = function.derivative();
		// The mistakes keep the exponents, or do not multiply by them, keep the constant, or flip the last sign.
		List<String> mistakes = new ArrayList<>();
		mistakes.add(function.map(term -> new Term(term.coefficient() * term.exponent(), term.exponent())).toString());
		mistakes.add(function.map(term -> new Term(term.coefficient(), term.exponent() - 1)).toString());
		Term last = function.terms().getLast();
		if (last.exponent() == 0) {
			List<Term> kept = new ArrayList<>(answer.terms());
			kept.add(last);
			mistakes.add(new Polynomial(kept).toString());
		}
		if (answer.terms().size() > 1) {
			mistakes.add(flipLast(answer).toString());
		}
		return new Problem("d/dx (" + function + ")", answer.toString(),
				WrongAnswers.pick(random, answer.toString(), mistakes, () -> nudge(random, answer).toString()));
	}

	// A coefficient from -9 to 9, not 0.
	static int signed(Random random) {
		int coefficient = random.nextInt(1, 10);
		return random.nextBoolean() ? coefficient : -coefficient;
	}

	// The polynomial with its last term's sign flipped.
	static Polynomial flipLast(Polynomial polynomial) {
		List<Term> terms = new ArrayList<>(polynomial.terms());
		Term last = terms.removeLast();
		terms.add(new Term(-last.coefficient(), last.exponent()));
		return new Polynomial(terms);
	}

	// The polynomial with one coefficient off by 1 to 3.
	static Polynomial nudge(Random random, Polynomial polynomial) {
		List<Term> terms = new ArrayList<>(polynomial.terms());
		int i = random.nextInt(terms.size());
		int off = random.nextInt(1, 4) * (random.nextBoolean() ? 1 : -1);
		terms.set(i, new Term(terms.get(i).coefficient() + off, terms.get(i).exponent()));
		return new Polynomial(terms).map(term -> term);
	}

}
