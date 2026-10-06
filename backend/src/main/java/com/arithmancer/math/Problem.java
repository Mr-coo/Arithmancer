package com.arithmancer.math;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import com.arithmancer.math.turnbased.Limits;
import com.arithmancer.math.turnbased.Trigonometry;
import com.arithmancer.math.turnbased.WrongAnswers;

// A turn-based problem: its text, its exact answer and WrongAnswers.COUNT wrong ones, all as shown.
public record Problem(String text, String answer, List<String> wrong) {

	// A problem of the topic, as hard as the battle has got.
	public static Problem random(Random random, BattleTopic topic, double elapsedSeconds) {
		Difficulty difficulty = Difficulty.after(elapsedSeconds);
		return switch (topic) {
			case TRIGONOMETRY -> Trigonometry.pose(random, difficulty);
			case LIMITS -> Limits.pose(random, difficulty);
		};
	}

	// A problem with a whole answer. Its wrong answers are some of the mistakes, then numbers near the answer.
	public static Problem number(Random random, String text, int answer, Collection<Integer> mistakes) {
		String written = String.valueOf(answer);
		return new Problem(text, written, WrongAnswers.pick(random, written,
				mistakes.stream().map(String::valueOf).toList(), WrongAnswers.near(random, answer)));
	}

}
