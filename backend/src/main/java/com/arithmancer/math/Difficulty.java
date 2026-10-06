package com.arithmancer.math;

// How hard questions are, growing over a run. Starting values, to tune during development.
public enum Difficulty {

	EASY, MEDIUM, HARD;

	private static final double MEDIUM_FROM_SECONDS = 60;
	private static final double HARD_FROM_SECONDS = 150;

	public static Difficulty after(double elapsedSeconds) {
		if (elapsedSeconds < MEDIUM_FROM_SECONDS) {
			return EASY;
		}
		return elapsedSeconds < HARD_FROM_SECONDS ? MEDIUM : HARD;
	}

}
