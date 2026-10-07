package com.arithmancer.math.survival;

import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Question;

// Pythagoras: two sides of a right triangle and the third to find, with legs a and b and hypotenuse c. As in the
// reference, Euclid's formula makes them whole: for m > n, m² - n², 2mn and m² + n². Unlike the reference, the triangle
// is then scaled by k, keeping the hypotenuse small enough to work out in real time.
public final class Pythagoras {

	private Pythagoras() {
	}

	public static Question ask(Random random, Difficulty difficulty) {
		int maxHypotenuse = switch (difficulty) {
			case EASY -> 30;
			case MEDIUM -> 60;
			case HARD -> 100;
		};
		int m;
		int n;
		do {
			m = random.nextInt(2, 10);
			n = random.nextInt(1, m);
		} while (m * m + n * n > maxHypotenuse);
		int k = random.nextInt(1, maxHypotenuse / (m * m + n * n) + 1);
		int a = k * (m * m - n * n);
		int b = k * 2 * m * n;
		int c = k * (m * m + n * n);
		return switch (random.nextInt(3)) {
			case 0 -> new Question("a=" + a + " b=" + b + " c=?", c);
			case 1 -> new Question("a=" + a + " c=" + c + " b=?", b);
			default -> new Question("b=" + b + " c=" + c + " a=?", a);
		};
	}

}
