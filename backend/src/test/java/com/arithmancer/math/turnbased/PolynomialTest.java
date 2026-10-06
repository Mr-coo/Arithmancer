package com.arithmancer.math.turnbased;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.arithmancer.math.turnbased.Polynomial.Term;

class PolynomialTest {

	private final Polynomial polynomial = Polynomial.of(new Term(2, 3), new Term(-1, 2), new Term(-4, 1),
			new Term(7, 0));

	@Test
	void writesTermsWithSignsBetweenThem() {
		assertEquals("2x³ - x² - 4x + 7", polynomial.toString());
		assertEquals("0", Polynomial.of().toString());
	}

	@Test
	void derivativeLowersEachExponentAndDropsTheConstant() {
		assertEquals("6x² - 2x - 4", polynomial.derivative().toString());
	}

	@Test
	void worksOutItsValue() {
		assertEquals(2 * 8 - 4 - 8 + 7, polynomial.at(2));
	}

}
