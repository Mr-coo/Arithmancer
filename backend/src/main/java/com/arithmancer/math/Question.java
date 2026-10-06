package com.arithmancer.math;

import java.util.Random;

import com.arithmancer.math.survival.Algebra;
import com.arithmancer.math.survival.Arithmetic;
import com.arithmancer.math.survival.Exponents;
import com.arithmancer.math.survival.Fractions;
import com.arithmancer.math.survival.Pythagoras;

// A survival question: its text and its whole result, at least 0.
public record Question(String text, int result) {

	// In real-time games, answers are one digit, so a single key press answers a question: bigger results count by
	// their last digit.
	private static final int DIGITS = 10;

	// A question of the topic, as hard as the run has got.
	public static Question random(Random random, SurvivalTopic topic, double elapsedSeconds) {
		Difficulty difficulty = Difficulty.after(elapsedSeconds);
		return switch (topic) {
			case ARITHMETIC -> Arithmetic.ask(random, difficulty);
			case FRACTIONS -> Fractions.ask(random, difficulty);
			case ALGEBRA -> Algebra.ask(random, difficulty);
			case EXPONENTS -> Exponents.ask(random, difficulty);
			case PYTHAGORAS -> Pythagoras.ask(random, difficulty);
		};
	}

	// The one-digit answer that real-time games accept: the result's last digit.
	public int answer() {
		return result % DIGITS;
	}

}
