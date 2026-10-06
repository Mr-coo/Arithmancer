package com.arithmancer.math.turnbased;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

// The wrong options of a turn-based problem: its likely mistakes first, topped up by other guesses.
public final class WrongAnswers {

	public static final int COUNT = 3;
	// Numbers near an answer are at most this far from it.
	private static final int NEAR = 10;

	private WrongAnswers() {
	}

	// COUNT distinct wrong answers: some of the mistakes, in random order, then more until there are enough. more must
	// be able to come up with enough answers that differ from each other and from the answer.
	public static List<String> pick(Random random, String answer, Collection<String> mistakes, Supplier<String> more) {
		List<String> shuffled = new ArrayList<>(new LinkedHashSet<>(mistakes));
		shuffled.remove(answer);
		Collections.shuffle(shuffled, random);
		Set<String> wrong = new LinkedHashSet<>(shuffled.subList(0, Math.min(COUNT, shuffled.size())));
		while (wrong.size() < COUNT) {
			String guess = more.get();
			if (!guess.equals(answer)) {
				wrong.add(guess);
			}
		}
		return List.copyOf(wrong);
	}

	// Whole numbers within NEAR of the answer, not below 0 unless the answer is.
	public static Supplier<String> near(Random random, int answer) {
		int lowest = answer >= 0 ? Math.max(0, answer - NEAR) : answer - NEAR;
		return () -> String.valueOf(random.nextInt(lowest, answer + NEAR + 1));
	}

}
