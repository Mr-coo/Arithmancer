package com.arithmancer.math;

import java.util.List;
import java.util.Random;

import com.arithmancer.math.turnbased.WrongAnswers;

// A turn-based problem: its text, its exact answer and WrongAnswers.COUNT wrong ones, all as shown.
public record Problem(String text, String answer, List<String> wrong) {

	// A question of the topic, with numbers near its result as the wrong answers.
	public static Problem random(Random random, Topic topic, double elapsedSeconds) {
		Question question = Question.random(random, topic, elapsedSeconds);
		String answer = String.valueOf(question.result());
		return new Problem(question.text(), answer,
				WrongAnswers.pick(random, answer, List.of(), WrongAnswers.near(random, question.result())));
	}

}
