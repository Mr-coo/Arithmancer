package com.arithmancer.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.arithmancer.room.Player;

class BattleTest {

	private static final double TICK_SECONDS = 0.05;

	private final Player player = new Player("session", "Marco");
	private final Battle battle = new Battle("ABCD", List.of(player));

	@Test
	void optionsAreFourDifferentNumbersWithTheAnswer() {
		// Answering right on every tick beats each goblin, so the battle lasts long enough for × and ÷ and larger
		// numbers.
		while (!battle.isOver() && battle.getTurn() <= 8) {
			Fighter fighter = battle.getFighter(player);
			List<Integer> options = fighter.getOptions();
			assertEquals(4, options.size(), options::toString);
			assertEquals(4, Set.copyOf(options).size(), options::toString);
			assertTrue(options.stream().allMatch(option -> option >= 0), options::toString);
			assertTrue(options.contains(fighter.getProblem().result()), options::toString);
			player.submitAnswer(fighter.getProblem().result());
			battle.tick(TICK_SECONDS);
		}
		assertTrue(battle.getTurn() > 8);
	}

	@Test
	void aWrongAnswerLocksTheOptions() {
		Fighter fighter = battle.getFighter(player);
		player.submitAnswer(fighter.getProblem().result() + 1);
		battle.tick(TICK_SECONDS);
		assertTrue(fighter.getLockSeconds() > 0);
		assertEquals(0, fighter.getCharge());
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
