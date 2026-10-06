package com.arithmancer.math.turnbased;

import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Problem;
import com.arithmancer.math.Written;

// Geometry: a rectangle's area or perimeter, or a triangle's missing angle, at first; then a triangle's area, or a
// circle's area or circumference in terms of π; then the volume of a cylinder or a cone.
public final class Geometry {

	private Geometry() {
	}

	public static Problem pose(Random random, Difficulty difficulty) {
		return switch (difficulty) {
			case EASY -> switch (random.nextInt(3)) {
				case 0 -> rectangle(random, true);
				case 1 -> rectangle(random, false);
				default -> angle(random);
			};
			case MEDIUM -> switch (random.nextInt(3)) {
				case 0 -> triangle(random);
				case 1 -> circle(random, true);
				default -> circle(random, false);
			};
			case HARD -> random.nextBoolean() ? cylinder(random) : cone(random);
		};
	}

	// The area is w × h and the perimeter 2(w + h). The mistakes give the other one, or add the sides once.
	private static Problem rectangle(Random random, boolean area) {
		int w = random.nextInt(2, 13);
		int h = random.nextInt(2, 13);
		String text = "Rectangle " + w + " × " + h + ": " + (area ? "area" : "perimeter") + "?";
		return area ? Problem.number(random, text, w * h, List.of(2 * (w + h), w + h))
				: Problem.number(random, text, 2 * (w + h), List.of(w * h, w + h, 2 * w + h));
	}

	// The angles of a triangle add up to 180°. The mistakes take the two from 360° or 90°, or add them.
	private static Problem angle(Random random) {
		int a = random.nextInt(20, 90);
		int b = random.nextInt(20, 160 - a);
		int answer = 180 - a - b;
		return ask(random, "Triangle " + a + "°, " + b + "°: third angle?", answer,
				List.of(360 - a - b, a + b, Math.abs(90 - a - b)), n -> n + "°");
	}

	// Half the base times the height, kept whole. The mistakes forget the half, or add the two.
	private static Problem triangle(Random random) {
		int base = random.nextInt(2, 13);
		int height = random.nextInt(2, 13);
		if (base * height % 2 == 1) {
			base++;
		}
		return Problem.number(random, "Triangle base " + base + " height " + height + ": area?", base * height / 2,
				List.of(base * height, base + height, (base + height) * 2));
	}

	// The area is πr² and the circumference 2πr. The mistakes give the other one, πr, or use the diameter as r.
	private static Problem circle(Random random, boolean area) {
		int r = random.nextInt(1, 10);
		String text = "Circle r=" + r + ": " + (area ? "area" : "circumference") + "?";
		return area ? ask(random, text, r * r, List.of(2 * r, r, 4 * r * r), Geometry::pi)
				: ask(random, text, 2 * r, List.of(r * r, r, 4 * r), Geometry::pi);
	}

	// πr²h. The mistakes give the side's area 2πrh, πrh, or a cone's volume.
	private static Problem cylinder(Random random) {
		int r = random.nextInt(1, 7);
		int h = random.nextInt(2, 11);
		String answer = pi(r * r * h);
		List<String> mistakes = List.of(pi(2 * r * h), pi(r * h), pi(r * r * h, 3));
		return new Problem("Cylinder r=" + r + " h=" + h + ": volume?", answer,
				WrongAnswers.pick(random, answer, mistakes, near(random, r * r * h, Geometry::pi)));
	}

	// ⅓πr²h, with h a multiple of 3 so it comes out whole. The mistakes forget the third, halve instead, or use r for
	// r².
	private static Problem cone(Random random) {
		int r = random.nextInt(1, 7);
		int h = 3 * random.nextInt(1, 4);
		return ask(random, "Cone r=" + r + " h=" + h + ": volume?", r * r * h / 3,
				List.of(r * r * h, r * r * h / 2, r * h / 3), Geometry::pi);
	}

	// A problem with a whole answer written with a unit, such as ° or π. Its wrong answers are some of the mistakes,
	// then numbers near the answer.
	private static Problem ask(Random random, String text, int answer, List<Integer> mistakes,
			IntFunction<String> written) {
		return new Problem(text, written.apply(answer), WrongAnswers.pick(random, written.apply(answer),
				mistakes.stream().map(written::apply).toList(), near(random, answer, written)));
	}

	private static Supplier<String> near(Random random, int answer, IntFunction<String> written) {
		Supplier<String> near = WrongAnswers.near(random, answer);
		return () -> written.apply(Integer.parseInt(near.get()));
	}

	// n × π: 9π, π, 0.
	private static String pi(int n) {
		return pi(n, 1);
	}

	// n/d × π: 20π/3, 2π, π/2.
	private static String pi(int n, int d) {
		if (n == 0) {
			return "0";
		}
		String[] fraction = Written.fraction(n, d).split("/");
		return (fraction[0].equals("1") ? "" : fraction[0]) + "π" + (fraction.length > 1 ? "/" + fraction[1] : "");
	}

}
