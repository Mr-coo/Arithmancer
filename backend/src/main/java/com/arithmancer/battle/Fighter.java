package com.arithmancer.battle;

import java.util.List;

import com.arithmancer.math.Question;

// A player's side of a battle: their problem, and the damage their right answers have charged up this turn.
public class Fighter {

	private Question problem;
	private List<Integer> options = List.of();
	private int problemId;
	// Right answers this turn, and the damage they add up to.
	private int streak;
	private int charge;
	// Seconds until a wrong answer stops locking the options.
	private double lockSeconds;
	// Damage dealt over the battle.
	private int damageDealt;

	public Question getProblem() {
		return problem;
	}

	// The numbers to pick from, in the order shown.
	public List<Integer> getOptions() {
		return options;
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

	void pose(Question problem, List<Integer> options) {
		this.problem = problem;
		this.options = options;
		problemId++;
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
