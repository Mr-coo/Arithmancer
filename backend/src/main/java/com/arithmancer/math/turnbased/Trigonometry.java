package com.arithmancer.math.turnbased;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Problem;
import com.arithmancer.math.Written;

// Trigonometry: the exact sine, cosine or tangent of a special angle. Easy angles go up to 90°, medium ones up to 180°
// with tangents too, and hard ones all the way round, half the time in radians.
public final class Trigonometry {

	private static final List<Integer> EASY_ANGLES = List.of(0, 30, 45, 60, 90);
	private static final List<Integer> MEDIUM_ANGLES = List.of(0, 30, 45, 60, 90, 120, 135, 150, 180);
	private static final List<Integer> HARD_ANGLES = List.of(0, 30, 45, 60, 90, 120, 135, 150, 180, 210, 225, 240, 270,
			300, 315, 330);
	private static final List<String> FUNCTIONS = List.of("sin", "cos", "tan");
	// Of the angles up to 90°; the others take these with a sign.
	private static final Map<Integer, String> SINES = Map.of(0, "0", 30, "1/2", 45, "√2/2", 60, "√3/2", 90, "1");
	private static final Map<Integer, String> TANGENTS = Map.of(0, "0", 30, "√3/3", 45, "1", 60, "√3");
	// Other exact values to guess, once the easy ones' are not enough.
	private static final List<String> EASY_VALUES = List.of("0", "1/2", "√2/2", "√3/2", "1");
	private static final List<String> VALUES = List.of("0", "1/2", "√2/2", "√3/2", "1", "√3/3", "√3", "-1/2", "-√2/2",
			"-√3/2", "-1", "-√3/3", "-√3");

	private Trigonometry() {
	}

	public static Problem pose(Random random, Difficulty difficulty) {
		List<Integer> angles = switch (difficulty) {
			case EASY -> EASY_ANGLES;
			case MEDIUM -> MEDIUM_ANGLES;
			case HARD -> HARD_ANGLES;
		};
		int functions = difficulty == Difficulty.EASY ? 2 : FUNCTIONS.size();
		String function;
		int angle;
		String answer;
		do {
			function = FUNCTIONS.get(random.nextInt(functions));
			angle = angles.get(random.nextInt(angles.size()));
			answer = value(function, angle);
		} while (answer == null);
		String shown = difficulty == Difficulty.HARD && random.nextBoolean() ? radians(angle) : angle + "°";
		// The mistakes take another function of the same angle, or flip the sign.
		List<String> mistakes = new ArrayList<>();
		for (String other : FUNCTIONS) {
			String value = value(other, angle);
			if (value != null) {
				mistakes.add(value);
			}
		}
		List<String> values = EASY_VALUES;
		if (difficulty != Difficulty.EASY) {
			mistakes.add(answer.startsWith("-") ? answer.substring(1) : signed(answer, true));
			values = VALUES;
		}
		List<String> guesses = values;
		return new Problem(function + " " + shown, answer,
				WrongAnswers.pick(random, answer, mistakes, () -> guesses.get(random.nextInt(guesses.size()))));
	}

	// The exact value of the function at the angle, from 0° to 359°, or null where the tangent is undefined.
	static String value(String function, int degrees) {
		int reference = degrees <= 90 ? degrees : degrees <= 180 ? 180 - degrees : degrees <= 270 ? degrees - 180
				: 360 - degrees;
		boolean sineNegative = degrees > 180;
		boolean cosineNegative = degrees > 90 && degrees < 270;
		return switch (function) {
			case "sin" -> signed(SINES.get(reference), sineNegative);
			case "cos" -> signed(SINES.get(90 - reference), cosineNegative);
			default -> reference == 90 ? null : signed(TANGENTS.get(reference), sineNegative != cosineNegative);
		};
	}

	private static String signed(String value, boolean negative) {
		return negative && !value.equals("0") ? "-" + value : value;
	}

	// The angle as a fraction of π: 0, π/6, 5π/4, π, 3π/2.
	private static String radians(int degrees) {
		if (degrees == 0) {
			return "0";
		}
		String[] fraction = Written.fraction(degrees, 180).split("/");
		return (fraction[0].equals("1") ? "" : fraction[0]) + "π" + (fraction.length > 1 ? "/" + fraction[1] : "");
	}

}
