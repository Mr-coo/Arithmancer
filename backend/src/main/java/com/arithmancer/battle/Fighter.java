package com.arithmancer.battle;

import java.util.ArrayList;
import java.util.List;

import com.arithmancer.math.Problem;

// A player's side of a battle: their problem, and the damage their right answers have charged up this turn.
public class Fighter {

	private Problem problem;
	private List<String> options = List.of();
	private int rightOption;
	// The options picked wrong on this problem, which no longer count.
	private final List<Integer> wrongPicks = new ArrayList<>();
	private int problemId;
	// Right answers this turn, and the damage they add up to.
	private int streak;
	private int charge;
	// Seconds until a wrong answer stops locking the options.
	private double lockSeconds;
	// Damage dealt over the battle.
	private int damageDealt;

	public Problem getProblem() {
		return problem;
	}

	// The answers to pick from, in the order shown.
	public List<String> getOptions() {
		return options;
	}

	// The index of the right answer in the options.
	public int getRightOption() {
		return rightOption;
	}

	public List<Integer> getWrongPicks() {
		return List.copyOf(wrongPicks);
	}

	public int getProblemId() {
		return problemId;
	}

	public int getCharge() {
		return charge;
	}

	public double getLockSeconds() {
		return lockSeconds;
	}

	public int getDamageDealt() {
		return damageDealt;
	}

	void pose(Problem problem, List<String> options, int rightOption) {
		this.problem = problem;
		this.options = options;
		this.rightOption = rightOption;
		wrongPicks.clear();
		problemId++;
	}

	void pickWrong(int option) {
		wrongPicks.add(option);
	}

	// Each right answer this turn adds one more than the last: 1, then 2, then 3...
	void answerRight() {
		streak++;
		charge += streak;
	}

	void lock(double seconds) {
		lockSeconds = seconds;
	}

	void coolDown(double deltaSeconds) {
		lockSeconds = Math.max(0, lockSeconds - deltaSeconds);
	}

	// Spends the charge, so the next turn starts from nothing. Returns the damage dealt.
	int strike() {
		int damage = charge;
		damageDealt += damage;
		charge = 0;
		streak = 0;
		return damage;
	}

}
