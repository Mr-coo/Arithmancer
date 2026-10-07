package com.arithmancer.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	void matrixProblemsDrawAMatrixWhoseDeterminantIsTheAnswer() {
		Random random = new Random(1);
		for (double seconds : SECONDS) {
			for (int i = 0; i < 1000; i++) {
				Problem problem = Problem.random(random, BattleTopic.MATRICES, seconds);
				List<List<Integer>> m = problem.matrix();
				assertTrue(m.size() == 2 || m.size() == 3, problem.text());
				m.forEach(row -> assertEquals(m.size(), row.size(), problem.text()));
				int determinant = m.size() == 2 ? m.get(0).get(0) * m.get(1).get(1) - m.get(0).get(1) * m.get(1).get(0)
						: m.get(0).get(0) * (m.get(1).get(1) * m.get(2).get(2) - m.get(1).get(2) * m.get(2).get(1))
								- m.get(0).get(1) * (m.get(1).get(0) * m.get(2).get(2) - m.get(1).get(2) * m.get(2).get(0))
								+ m.get(0).get(2) * (m.get(1).get(0) * m.get(2).get(1) - m.get(1).get(1) * m.get(2).get(0));
				assertEquals(String.valueOf(determinant), problem.answer(), problem.text());
			}
		}
	}

	@Test
	void onlyMatrixProblemsDrawAMatrix() {
		Random random = new Random(1);
		for (BattleTopic topic : BattleTopic.values()) {
			if (topic != BattleTopic.MATRICES) {
				assertNull(Problem.random(random, topic, 0).matrix(), topic::toString);
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
