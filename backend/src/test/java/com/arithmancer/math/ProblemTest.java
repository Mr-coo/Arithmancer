package com.arithmancer.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.arithmancer.math.turnbased.WrongAnswers;

class ProblemTest {

	// Easy, medium and hard.
	private static final double[] SECONDS = { 0, 100, 300 };

	@Test
	void everyProblemHasAnAnswerAndDistinctWrongOnes() {
		Random random = new Random(1);
		for (BattleTopic topic : BattleTopic.values()) {
			for (double seconds : SECONDS) {
				for (int i = 0; i < 1000; i++) {
					Problem problem = Problem.random(random, topic, seconds);
					String shown = problem.text() + " → " + problem.answer() + " " + problem.wrong();
					assertFalse(problem.text().isBlank(), shown);
					assertFalse(problem.answer().isBlank(), shown);
					List<String> wrong = problem.wrong();
					assertEquals(WrongAnswers.COUNT, wrong.size(), shown);
					Set<String> options = new HashSet<>(wrong);
					options.add(problem.answer());
					assertEquals(WrongAnswers.COUNT + 1, options.size(), shown);
				}
			}
		}
	}

	@Test
	void statisticsWrongAnswersAreDecimalWhenTheAnswerIs() {
		Random random = new Random(1);
		for (double seconds : SECONDS) {
			for (int i = 0; i < 1000; i++) {
				Problem problem = Problem.random(random, BattleTopic.STATISTICS, seconds);
				boolean decimal = problem.answer().contains(".");
				for (String wrong : problem.wrong()) {
					assertEquals(decimal, wrong.contains("."), problem.text() + " → " + problem.answer() + " "
							+ problem.wrong());
				}
			}
		}
	}

}
