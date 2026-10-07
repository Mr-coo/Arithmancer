package com.arithmancer.math.turnbased;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Problem;

// Matrices: the determinant of a 2×2 matrix with small entries at first, then with negative ones too, then of a 3×3.
// Matrices whose determinant is 0 are drawn again, so the answers vary.
public final class Matrices {

	private Matrices() {
	}

	public static Problem pose(Random random, Difficulty difficulty) {
		return switch (difficulty) {
			case EASY -> twoByTwo(random, 0, 5);
			case MEDIUM -> twoByTwo(random, -5, 9);
			case HARD -> threeByThree(random);
		};
	}

	// det [a b; c d] = ad - bc. The mistakes add bc, subtract the other way round, or stop at ad.
	private static Problem twoByTwo(Random random, int min, int max) {
		int[][] m;
		int ad;
		int bc;
		do {
			m = entries(random, 2, min, max);
			ad = m[0][0] * m[1][1];
			bc = m[0][1] * m[1][0];
		} while (ad == bc);
		return drawn(Problem.number(random, "det " + written(m), ad - bc, List.of(ad + bc, bc - ad, ad)), m);
	}

	// The sum of the products down to the right, less those down to the left. The mistakes add them all, take them
	// the other way round, or stop at the first sum.
	private static Problem threeByThree(Random random) {
		int[][] m;
		int right;
		int left;
		do {
			m = entries(random, 3, 0, 3);
			right = 0;
			left = 0;
			for (int i = 0; i < 3; i++) {
				right += m[0][i] * m[1][(i + 1) % 3] * m[2][(i + 2) % 3];
				left += m[0][i] * m[1][(i + 2) % 3] * m[2][(i + 1) % 3];
			}
		} while (right == left);
		return drawn(Problem.number(random, "det " + written(m), right - left, List.of(right + left, left - right, right)),
				m);
	}

	// The problem with its matrix, drawn instead of its text.
	private static Problem drawn(Problem problem, int[][] matrix) {
		return new Problem(problem.text(), problem.answer(), problem.wrong(),
				Arrays.stream(matrix).map(row -> Arrays.stream(row).boxed().toList()).toList());
	}

	private static int[][] entries(Random random, int size, int min, int max) {
		int[][] matrix = new int[size][size];
		for (int[] row : matrix) {
			Arrays.setAll(row, i -> random.nextInt(min, max + 1));
		}
		return matrix;
	}

	// The rows, split by semicolons: [2 3; 1 4].
	private static String written(int[][] matrix) {
		return Arrays.stream(matrix)
				.map(row -> Arrays.stream(row).mapToObj(String::valueOf).collect(Collectors.joining(" ")))
				.collect(Collectors.joining("; ", "[", "]"));
	}

}
