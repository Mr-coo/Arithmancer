package com.arithmancer.math.turnbased;

import java.util.List;
import java.util.function.UnaryOperator;

import com.arithmancer.math.Written;

// A polynomial with whole coefficients, as its terms from the highest power down.
record Polynomial(List<Term> terms) {

	// coefficient × x to the exponent.
	record Term(int coefficient, int exponent) {
	}

	static Polynomial of(Term... terms) {
		return new Polynomial(List.of(terms));
	}

	// Changes every term, leaving out those that come to nothing or to a negative exponent.
	Polynomial map(UnaryOperator<Term> change) {
		return new Polynomial(terms.stream()
				.map(change)
				.filter(term -> term.coefficient() != 0 && term.exponent() >= 0)
				.toList());
	}

	// Each term cxⁿ becomes ncxⁿ⁻¹, and constants drop out.
	Polynomial derivative() {
		return map(term -> new Term(term.coefficient() * term.exponent(), term.exponent() - 1));
	}

	int at(int x) {
		return terms.stream().mapToInt(term -> term.coefficient() * (int) Math.pow(x, term.exponent())).sum();
	}

	// The terms written out, highest power first: 3x² - 2x + 5. 0 when there are none.
	@Override
	public String toString() {
		if (terms.isEmpty()) {
			return "0";
		}
		Term first = terms.getFirst();
		StringBuilder written = new StringBuilder(Written.term(first.coefficient(), first.exponent()));
		for (Term term : terms.subList(1, terms.size())) {
			written.append(term.coefficient() < 0 ? " - " : " + ")
					.append(Written.term(Math.abs(term.coefficient()), term.exponent()));
		}
		return written.toString();
	}

}
