package com.arithmancer.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class QuestionTest {

	// Easy, medium and hard.
	private static final double[] SECONDS = { 0, 100, 300 };
	private static final Pattern EQUATION = Pattern.compile("(\\d+)x(?: ([+-]) (\\d+))? = (-?\\d+)");
	private static final Pattern SIDE = Pattern.compile("([abc])=(\\d+|\\?)");
	private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";
	private static final String SUBSCRIPTS = "₀₁₂₃₄₅₆₇₈₉";

	@Test
	void resultsAreAtLeastZero() {
		for (SurvivalTopic topic : SurvivalTopic.values()) {
			forEachQuestion(topic, question -> assertTrue(question.result() >= 0, question.text()));
		}
	}

	@Test
	void arithmeticAndFractionsWorkOutToTheirResult() {
		for (SurvivalTopic topic : new SurvivalTopic[] { SurvivalTopic.ARITHMETIC, SurvivalTopic.FRACTIONS }) {
			forEachQuestion(topic, question -> assertEquals(question.result(), new Evaluator(question.text()).evaluate(),
					question.text()));
		}
	}

	@Test
	void algebraResultsSolveTheirEquation() {
		forEachQuestion(SurvivalTopic.ALGEBRA, question -> {
			Matcher matcher = EQUATION.matcher(question.text());
			assertTrue(matcher.matches(), question.text());
			int b = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
			int left = Integer.parseInt(matcher.group(1)) * question.result() + ("-".equals(matcher.group(2)) ? -b : b);
			assertEquals(Integer.parseInt(matcher.group(4)), left, question.text());
		});
	}

	@Test
	void exponentsResultsMatchTheirPowerOrLogarithm() {
		forEachQuestion(SurvivalTopic.EXPONENTS, question -> {
			String text = question.text();
			if (text.startsWith("log")) {
				int base = Integer.parseInt(decode(text.substring(3, text.indexOf(' ')), SUBSCRIPTS));
				int power = Integer.parseInt(text.substring(text.indexOf(' ') + 1));
				assertEquals(power, (int) Math.pow(base, question.result()), text);
			} else {
				int split = text.length() - (int) text.chars().filter(c -> SUPERSCRIPTS.indexOf(c) >= 0).count();
				int base = Integer.parseInt(text.substring(0, split));
				int exponent = Integer.parseInt(decode(text.substring(split), SUPERSCRIPTS));
				assertEquals((int) Math.pow(base, exponent), question.result(), text);
			}
		});
	}

	@Test
	void pythagorasSidesMakeARightTriangle() {
		forEachQuestion(SurvivalTopic.PYTHAGORAS, question -> {
			Map<String, Integer> sides = new HashMap<>();
			Matcher matcher = SIDE.matcher(question.text());
			while (matcher.find()) {
				sides.put(matcher.group(1), "?".equals(matcher.group(2)) ? question.result()
						: Integer.parseInt(matcher.group(2)));
			}
			assertEquals(3, sides.size(), question.text());
			int a = sides.get("a");
			int b = sides.get("b");
			int c = sides.get("c");
			assertTrue(a > 0 && b > 0, question.text());
			assertTrue(c <= 100, question.text());
			assertEquals(c * c, a * a + b * b, question.text());
		});
	}

	// 1000 questions of the topic at each difficulty.
	private static void forEachQuestion(SurvivalTopic topic, Consumer<Question> check) {
		Random random = new Random(1);
		for (double seconds : SECONDS) {
			for (int i = 0; i < 1000; i++) {
				check.accept(Question.random(random, topic, seconds));
			}
		}
	}

	// The digits written with these characters for 0 to 9.
	private static String decode(String written, String characters) {
		StringBuilder digits = new StringBuilder();
		written.chars().forEach(c -> digits.append(characters.indexOf(c)));
		return digits.toString();
	}

	// Works out +, -, ×, ÷ and / (from left to right, × ÷ / before + -), brackets and negative numbers, checking that
	// every division comes out whole.
	private static class Evaluator {

		private final String text;
		private int at;

		Evaluator(String text) {
			this.text = text.replace(" ", "");
		}

		int evaluate() {
			int value = sum();
			assertEquals(text.length(), at, "Left over in " + text);
			return value;
		}

		private int sum() {
			int value = product();
			while (at < text.length() && (peek() == '+' || peek() == '-')) {
				value = text.charAt(at++) == '+' ? value + product() : value - product();
			}
			return value;
		}

		private int product() {
			int value = factor();
			while (at < text.length() && (peek() == '×' || peek() == '÷' || peek() == '/')) {
				char operator = text.charAt(at++);
				int right = factor();
				if (operator == '×') {
					value *= right;
				} else {
					assertEquals(0, value % right, "Not whole in " + text);
					value /= right;
				}
			}
			return value;
		}

		private int factor() {
			if (peek() == '-') {
				at++;
				return -factor();
			}
			if (peek() == '(') {
				at++;
				int value = sum();
				assertEquals(')', text.charAt(at++), text);
				return value;
			}
			int start = at;
			while (at < text.length() && Character.isDigit(peek())) {
				at++;
			}
			return Integer.parseInt(text.substring(start, at));
		}

		private char peek() {
			return text.charAt(at);
		}

	}

}
