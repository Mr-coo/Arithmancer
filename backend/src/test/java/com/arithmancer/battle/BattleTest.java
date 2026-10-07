package com.arithmancer.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.arithmancer.math.BattleTopic;
import com.arithmancer.room.Player;

class BattleTest {

	private static final double TICK_SECONDS = 0.05;

	private final Player player = new Player("session", "Marco");
	private final Battle battle = new Battle("ABCD", List.of(player), BattleTopic.LIMITS);

	@Test
	void optionsAreFourDifferentNumbersWithTheAnswer() {
		for (BattleTopic topic : BattleTopic.values()) {
			Player player = new Player("session", "Marco");
			Battle battle = new Battle("ABCD", List.of(player), topic);
			// Answering right on every tick beats each goblin, so the battle lasts long enough for larger numbers.
			while (!battle.isOver() && battle.getTurn() <= 8) {
				Fighter fighter = battle.getFighter(player);
				List<String> options = fighter.getOptions();
				assertEquals(4, options.size(), options::toString);
				assertEquals(4, Set.copyOf(options).size(), options::toString);
				assertEquals(fighter.getProblem().answer(), options.get(fighter.getRightOption()), options::toString);
				player.submitAnswer(fighter.getRightOption());
				battle.tick(TICK_SECONDS);
			}
			assertTrue(battle.getTurn() > 8, topic::toString);
		}
	}

	@Test
	void aWrongAnswerLocksTheOptionsAndKeepsTheProblem() {
		Fighter fighter = battle.getFighter(player);
		int problemId = fighter.getProblemId();
		int wrong = (fighter.getRightOption() + 1) % fighter.getOptions().size();
		player.submitAnswer(wrong);
		battle.tick(TICK_SECONDS);
		assertTrue(fighter.getLockSeconds() > 0);
		assertEquals(0, fighter.getCharge());
		assertEquals(problemId, fighter.getProblemId());
		assertEquals(List.of(wrong), fighter.getWrongPicks());
	}

	@Test
	void theRightAnswerCountsOnceTheLockIsOver() {
		Fighter fighter = battle.getFighter(player);
		int wrong = (fighter.getRightOption() + 1) % fighter.getOptions().size();
		player.submitAnswer(wrong);
		battle.tick(TICK_SECONDS);
		while (fighter.getLockSeconds() > 0) {
			battle.tick(TICK_SECONDS);
		}
		// The option picked wrong no longer counts, not even as another wrong answer.
		player.submitAnswer(wrong);
		battle.tick(TICK_SECONDS);
		assertEquals(0, fighter.getLockSeconds());
		player.submitAnswer(fighter.getRightOption());
		battle.tick(TICK_SECONDS);
		assertEquals(1, fighter.getCharge());
		assertEquals(List.of(), fighter.getWrongPicks());
	}

	@Test
	void eachRightAnswerChargesOneMoreAndAStrikeSpendsIt() {
		Fighter fighter = new Fighter();
		fighter.answerRight();
		fighter.answerRight();
		fighter.answerRight();
		assertEquals(1 + 2 + 3, fighter.getCharge());
		assertEquals(6, fighter.strike());
		assertEquals(0, fighter.getCharge());
		assertEquals(6, fighter.getDamageDealt());
		fighter.answerRight();
		assertEquals(1, fighter.getCharge());
	}

}
