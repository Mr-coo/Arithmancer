package com.arithmancer.math.turnbased;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.arithmancer.math.Difficulty;
import com.arithmancer.math.Problem;

// Statistics of a list of numbers, shown unsorted: the mean or range of 3 to 5 at first, then the median or mode, then
// the median of 6 or 7, or a mean that comes out as a decimal.
public final class Statistics {

	private Statistics() {
	}

	public static Problem pose(Random random, Difficulty difficulty) {
		return switch (difficulty) {
			case EASY -> random.nextBoolean() ? ask(random, "Mean", wholeMean(random))
					: ask(random, "Range", numbers(random, random.nextInt(3, 6), 10));
			case MEDIUM -> random.nextBoolean() ? ask(random, "Median", numbers(random, random.nextInt(5, 7), 15))
					: ask(random, "Mode", oneMode(random));
			case HARD -> random.nextBoolean() ? ask(random, "Median", numbers(random, random.nextInt(6, 8), 20))
					: ask(random, "Mean", numbers(random, random.nextInt(4, 6), 20));
		};
	}

	// The mistakes give another statistic of the same numbers, the middle one before sorting, or the largest.
	private static Problem ask(Random random, String statistic, List<Integer> numbers) {
		String mean = mean(numbers);
		String median = median(numbers);
		String range = String.valueOf(Collections.max(numbers) - Collections.min(numbers));
		Integer mode = mode(numbers);
		String answer = switch (statistic) {
			case "Mean" -> mean;
			case "Median" -> median;
			case "Range" -> range;
			default -> String.valueOf(mode);
		};
		List<String> mistakes = new ArrayList<>(List.of(median, range, String.valueOf(numbers.get(numbers.size() / 2)),
				String.valueOf(Collections.max(numbers))));
		if (mean != null) {
			mistakes.add(mean);
		}
		if (mode != null) {
			mistakes.add(String.valueOf(mode));
		}
		String text = statistic + " of " + numbers.stream().map(String::valueOf).collect(Collectors.joining(", "));
		return new Problem(text, answer, WrongAnswers.pick(random, answer, mistakes,
				WrongAnswers.near(random, (int) Math.round(Double.parseDouble(answer)))));
	}

	// count numbers from 1 to max.
	private static List<Integer> numbers(Random random, int count, int max) {
		return random.ints(count, 1, max + 1).boxed().toList();
	}

	// 3 to 5 numbers up to about 10, the last raised so that their mean is whole.
	private static List<Integer> wholeMean(Random random) {
		int count = random.nextInt(3, 6);
		List<Integer> numbers = new ArrayList<>(numbers(random, count, 10));
		int sum = numbers.stream().mapToInt(Integer::intValue).sum();
		int last = numbers.removeLast();
		numbers.add(last + (count - sum % count) % count);
		return numbers;
	}

	// 4 or 5 different numbers up to 15, one of them repeated once or twice, so it is the only mode.
	private static List<Integer> oneMode(Random random) {
		List<Integer> numbers = new ArrayList<>(
				random.ints(1, 16).distinct().limit(random.nextInt(4, 6)).boxed().toList());
		int repeated = numbers.get(random.nextInt(numbers.size()));
		numbers.addAll(Collections.nCopies(random.nextInt(1, 3), repeated));
		Collections.shuffle(numbers, random);
		return numbers;
	}

	// The sum over the count, or null when it does not come out as a decimal that ends.
	private static String mean(List<Integer> numbers) {
		return decimal(numbers.stream().mapToInt(Integer::intValue).sum(), numbers.size());
	}

	// The middle of the sorted numbers, or the mean of the two in the middle.
	private static String median(List<Integer> numbers) {
		int[] sorted = numbers.stream().mapToInt(Integer::intValue).sorted().toArray();
		int middle = sorted.length / 2;
		return sorted.length % 2 == 1 ? String.valueOf(sorted[middle])
				: decimal(sorted[middle - 1] + sorted[middle], 2);
	}

	// The only number that comes up most, more than once, or null when there is none.
	private static Integer mode(List<Integer> numbers) {
		Map<Integer, Long> counts = numbers.stream().collect(Collectors.groupingBy(Function.identity(),
				Collectors.counting()));
		long most = Collections.max(counts.values());
		List<Integer> modes = counts.entrySet().stream().filter(entry -> entry.getValue() == most).map(Map.Entry::getKey)
				.toList();
		return most > 1 && modes.size() == 1 ? modes.getFirst() : null;
	}

	private static String decimal(int numerator, int denominator) {
		try {
			return new BigDecimal(numerator).divide(new BigDecimal(denominator)).stripTrailingZeros().toPlainString();
		} catch (ArithmeticException e) {
			return null;
		}
	}

}
