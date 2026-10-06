package com.arithmancer.math.survival;

import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Question;

// Pythagoras: two sides of a right triangle and the third to find, with legs a and b and hypotenuse c. As in the
// reference, Euclid's formula makes them whole: for m > n, m² - n², 2mn and m² + n².
public final class Pythagoras {

	private Pythagoras() {
	}

	public static Question ask(Random random, Difficulty difficulty) {
		int m = switch (difficulty) {
			case EASY -> random.nextInt(2, 5);
			case MEDIUM -> random.nextInt(3, 9);
			case HARD -> random.nextInt(5, 16);
		};
		int n = random.nextInt(1, m);
		int a = m * m - n * n;
		int b = 2 * m * n;
		int c = m * m + n * n;
		return switch (random.nextInt(3)) {
			case 0 -> new Question("a=" + a + " b=" + b + " c=?", c);
			case 1 -> new Question("a=" + a + " c=" + c + " b=?", b);
			default -> new Question("b=" + b + " c=" + c + " a=?", a);
		};
	}

}
