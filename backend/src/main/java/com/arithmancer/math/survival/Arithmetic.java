package com.arithmancer.math.survival;

import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Question;

// Arithmetic: +, -, × and ÷ mixed, one operation at a time around the expression so far, which is bracketed once it has
// one: (3 + 4) × 2. As in the reference, an operation that would not come out whole and at least 0 becomes a +.
public final class Arithmetic {

	// × only grows expressions up to this, so results stay in the hundreds.
	private static final int MAX_MULTIPLIED = 30;
	// A number divided by an expression is at most this many times it.
	private static final int MAX_DIVIDED = 20;

	private Arithmetic() {
	}

	public static Question ask(Random random, Difficulty difficulty) {
		int operations = switch (difficulty) {
			case EASY -> 1;
			case MEDIUM -> 2;
			case HARD -> 3;
		};
		int max = switch (difficulty) {
			case EASY -> 10;
			case MEDIUM -> 20;
			case HARD -> 30;
		};
		int value = random.nextInt(1, max + 1);
		String text = String.valueOf(value);
		for (int i = 0; i < operations; i++) {
			String expression = i == 0 ? text : "(" + text + ")";
			int number = random.nextInt(1, max + 1);
			int factor = random.nextInt(2, 10);
			// Whether the new number goes before the expression rather than after it.
			boolean first = random.nextBoolean();
			switch (random.nextInt(4)) {
				case 1 -> {
					if (first && number >= value) {
						text = number + " - " + expression;
						value = number - value;
						continue;
					}
					if (value >= number) {
						text = expression + " - " + number;
						value -= number;
						continue;
					}
				}
				case 2 -> {
					if (value <= MAX_MULTIPLIED) {
						text = first ? factor + " × " + expression : expression + " × " + factor;
						value *= factor;
						continue;
					}
				}
				case 3 -> {
					if (first && value >= 1 && value <= MAX_DIVIDED) {
						text = value * factor + " ÷ " + expression;
						value = factor;
						continue;
					}
					if (value % factor == 0) {
						text = expression + " ÷ " + factor;
						value /= factor;
						continue;
					}
				}
				default -> {
				}
			}
			text = first ? number + " + " + expression : expression + " + " + number;
			value += number;
		}
		return new Question(text, value);
	}

}
