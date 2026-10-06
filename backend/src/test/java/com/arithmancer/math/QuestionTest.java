package com.arithmancer.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class QuestionTest {

	// From the start of a run, through medium, to large numbers.
	private static final double[] SECONDS = { 0, 30, 60, 100, 150, 300 };
	private static final Map<SurvivalTopic, String> OPERATORS = Map.of(SurvivalTopic.ADDITION, "+",
			SurvivalTopic.SUBTRACTION, "-", SurvivalTopic.MULTIPLICATION, "×", SurvivalTopic.DIVISION, "÷");

	@Test
	void arithmeticResultsMatchTheirQuestion() {
		Random random = new Random(1);
		for (SurvivalTopic topic : OPERATORS.keySet()) {
			for (double seconds : SECONDS) {
				for (int i = 0; i < 1000; i++) {
					Question question = Question.random(random, topic, seconds);
					assertEquals(OPERATORS.get(topic), question.text().split(" ")[1], question.text());
					assertTrue(question.result() >= 0, question.text());
					assertEquals(evaluate(question.text()), question.result(), question.text());
				}
			}
		}
	}

	@Test
	void resultsAreAtLeastZero() {
		Random random = new Random(1);
		for (SurvivalTopic topic : SurvivalTopic.values()) {
			for (double seconds : SECONDS) {
				for (int i = 0; i < 1000; i++) {
					Question question = Question.random(random, topic, seconds);
					assertTrue(question.result() >= 0, question.text());
				}
			}
		}
	}

	// Works out "a op b" for +, -, × and ÷, where a division comes out whole.
	private static int evaluate(String text) {
		String[] parts = text.split(" ");
		int a = Integer.parseInt(parts[0]);
		int b = Integer.parseInt(parts[2]);
		return switch (parts[1]) {
			case "+" -> a + b;
			case "-" -> a - b;
			case "×" -> a * b;
			case "÷" -> {
				assertEquals(0, a % b, text);
				yield a / b;
			}
			default -> throw new AssertionError("Unknown operator in " + text);
		};
	}

}
